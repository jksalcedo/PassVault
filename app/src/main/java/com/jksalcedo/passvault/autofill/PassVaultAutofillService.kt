package com.jksalcedo.passvault.autofill

import android.app.PendingIntent
import android.content.Intent
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.util.Log
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.crypto.Encryption
import com.jksalcedo.passvault.data.PasswordEntry
import com.jksalcedo.passvault.repositories.PasswordRepository
import com.jksalcedo.passvault.ui.auth.UnlockActivity
import com.jksalcedo.passvault.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PassVaultAutofillService : AutofillService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository by lazy { PasswordRepository(applicationContext) }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        try { Encryption.ensureKeyExists() } catch (_: Exception) {}

        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess(null)
            return
        }

        val parsed = StructureParser.parse(structure)
        Log.d(TAG, "Parsed: user=${parsed.usernameId} pass=${parsed.passwordId} email=${parsed.emailId} domain=${parsed.webDomain} pkg=${parsed.packageName}")

        if (parsed.usernameId == null && parsed.passwordId == null && parsed.emailId == null) {
            Log.d(TAG, "No fillable fields found")
            callback.onSuccess(null)
            return
        }

        val callingPackage = parsed.packageName ?: structure.activityComponent?.packageName
        if (callingPackage == packageName) {
            callback.onSuccess(null)
            return
        }

        serviceScope.launch {
            try {
                val entries = findMatchingEntries(parsed, callingPackage)
                Log.d(TAG, "Found ${entries.size} matching entries")

                if (entries.isEmpty()) {
                    callback.onSuccess(null)
                    return@launch
                }

                val responseBuilder = FillResponse.Builder()
                var datasetCount = 0
                for (entry in entries.take(5)) {
                    val dataset = buildDataset(entry, parsed)
                    if (dataset != null) {
                        responseBuilder.addDataset(dataset)
                        datasetCount++
                    }
                }

                if (datasetCount > 0) {
                    callback.onSuccess(responseBuilder.build())
                } else {
                    callback.onSuccess(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onFillRequest", e)
                callback.onSuccess(null)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun buildAuthResponse(parsed: ParsedStructure): FillResponse {
        val authIntent = Intent(this, UnlockActivity::class.java).apply {
            putExtra(EXTRA_AUTOFILL_AUTH, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            AUTH_REQUEST_CODE,
            authIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )

        val presentation = RemoteViews(packageName, R.layout.autofill_item).apply {
            setTextViewText(R.id.autofill_title, getString(R.string.autofill_unlock_title))
            setTextViewText(R.id.autofill_subtitle, getString(R.string.autofill_unlock_subtitle))
        }

        val datasetBuilder = Dataset.Builder(presentation)
            .setAuthentication(pendingIntent.intentSender)

        parsed.usernameId?.let {
            datasetBuilder.setValue(it, AutofillValue.forText(""))
        }
        parsed.passwordId?.let {
            datasetBuilder.setValue(it, AutofillValue.forText(""))
        }
        parsed.emailId?.let {
            datasetBuilder.setValue(it, AutofillValue.forText(""))
        }

        return FillResponse.Builder()
            .addDataset(datasetBuilder.build())
            .build()
    }

    private fun extractBaseDomain(rawDomain: String): String {
        val parts = rawDomain.split(".")
        return if (parts.size > 2) parts.takeLast(2).joinToString(".") else rawDomain
    }

    private suspend fun findMatchingEntries(
        parsed: ParsedStructure,
        callingPackage: String?
    ): List<PasswordEntry> {
        val rawDomain = parsed.webDomain

        if (!rawDomain.isNullOrEmpty()) {
            val baseDomain = extractBaseDomain(rawDomain)
            val siteName = baseDomain.substringBeforeLast(".")
            Log.d(TAG, "Searching: rawDomain=$rawDomain baseDomain=$baseDomain siteName=$siteName")

            val byDomain = repository.getEntriesByDomain(baseDomain)
            Log.d(TAG, "byDomain: ${byDomain.size} results")
            if (byDomain.isNotEmpty()) return byDomain

            if (siteName.length >= 3) {
                val bySearch = repository.searchEntries(siteName)
                Log.d(TAG, "bySearch($siteName): ${bySearch.size} results")
                if (bySearch.isNotEmpty()) return bySearch
            }
        }

        if (!callingPackage.isNullOrEmpty()) {
            val parts = callingPackage.split(".")
            val appName = parts.find { it.length >= 3 && it != "com" && it != "android" && it != "app" && it != "mobile" && it != "org" && it != "net" }
            if (!appName.isNullOrEmpty()) {
                val byAppName = repository.searchEntries(appName)
                Log.d(TAG, "byAppName($appName): ${byAppName.size} results")
                if (byAppName.isNotEmpty()) return byAppName
            }
        }

        return emptyList()
    }

    private fun buildDataset(entry: PasswordEntry, parsed: ParsedStructure): Dataset? {
        val credential = entry.username?.takeIf { it.isNotEmpty() }
            ?: entry.email?.takeIf { it.isNotEmpty() }
            ?: ""

        Log.d(TAG, "buildDataset: title=${entry.title} credential=$credential")

        val presentation = RemoteViews(packageName, R.layout.autofill_item).apply {
            setTextViewText(R.id.autofill_title, entry.title)
            setTextViewText(R.id.autofill_subtitle, credential)
        }

        val datasetBuilder = Dataset.Builder(presentation)
        var hasValue = false

        val password = try {
            Encryption.decrypt(entry.passwordCipher, entry.passwordIv)
        } catch (e: Exception) {
            Log.e(TAG, "Decrypt failed for ${entry.title}", e)
            null
        }

        parsed.usernameId?.let { id ->
            if (credential.isNotEmpty()) {
                datasetBuilder.setValue(id, AutofillValue.forText(credential))
                hasValue = true
            }
        }

        parsed.emailId?.let { id ->
            if (credential.isNotEmpty()) {
                datasetBuilder.setValue(id, AutofillValue.forText(credential))
                hasValue = true
            }
        }

        parsed.passwordId?.let { id ->
            if (!password.isNullOrEmpty()) {
                datasetBuilder.setValue(id, AutofillValue.forText(password))
                hasValue = true
            }
        }

        Log.d(TAG, "buildDataset result: hasValue=$hasValue")
        return if (hasValue) datasetBuilder.build() else null
    }

    companion object {
        private const val TAG = "PVAutofill"
        const val EXTRA_AUTOFILL_AUTH = "extra_autofill_auth"
        private const val AUTH_REQUEST_CODE = 9001
    }
}

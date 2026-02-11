package com.jksalcedo.passvault.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
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
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess(null)
            return
        }

        val parsed = StructureParser.parse(structure)
        if (parsed.usernameId == null && parsed.passwordId == null && parsed.emailId == null) {
            callback.onSuccess(null)
            return
        }

        // Skip our own package
        val callingPackage = parsed.packageName ?: structure.activityComponent?.packageName
        if (callingPackage == packageName) {
            callback.onSuccess(null)
            return
        }

        if (!SessionManager.isUnlocked) {
            val authResponse = buildAuthResponse(parsed)
            callback.onSuccess(authResponse)
            return
        }

        serviceScope.launch {
            try {
                val entries = findMatchingEntries(parsed, callingPackage)
                if (entries.isEmpty()) {
                    callback.onSuccess(null)
                    return@launch
                }

                val responseBuilder = FillResponse.Builder()
                for (entry in entries.take(5)) {
                    val dataset = buildDataset(entry, parsed)
                    if (dataset != null) {
                        responseBuilder.addDataset(dataset)
                    }
                }
                callback.onSuccess(responseBuilder.build())
            } catch (_: Exception) {
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

    private suspend fun findMatchingEntries(
        parsed: ParsedStructure,
        callingPackage: String?
    ): List<PasswordEntry> {
        val domain = parsed.webDomain
        if (!domain.isNullOrEmpty()) {
            val stripped = domain.removePrefix("www.")
            val entries = repository.getEntriesByDomain(stripped)
            if (entries.isNotEmpty()) return entries
        }
        // Fallback: search by package name
        if (!callingPackage.isNullOrEmpty()) {
            val parts = callingPackage.split(".")
            if (parts.size >= 2) {
                val domainGuess = parts.takeLast(2).reversed().joinToString(".")
                val entries = repository.getEntriesByDomain(domainGuess)
                if (entries.isNotEmpty()) return entries
            }
        }
        return emptyList()
    }

    private fun buildDataset(entry: PasswordEntry, parsed: ParsedStructure): Dataset? {
        val presentation = RemoteViews(packageName, R.layout.autofill_item).apply {
            setTextViewText(R.id.autofill_title, entry.title)
            setTextViewText(
                R.id.autofill_subtitle,
                entry.username ?: entry.email ?: ""
            )
        }

        val datasetBuilder = Dataset.Builder(presentation)
        var hasValue = false

        val password = try {
            Encryption.decrypt(entry.passwordCipher, entry.passwordIv)
        } catch (_: Exception) {
            null
        }

        parsed.usernameId?.let { id ->
            val value = entry.username ?: entry.email ?: ""
            if (value.isNotEmpty()) {
                datasetBuilder.setValue(id, AutofillValue.forText(value))
                hasValue = true
            }
        }

        parsed.emailId?.let { id ->
            val value = entry.email ?: entry.username ?: ""
            if (value.isNotEmpty()) {
                datasetBuilder.setValue(id, AutofillValue.forText(value))
                hasValue = true
            }
        }

        parsed.passwordId?.let { id ->
            if (password != null) {
                datasetBuilder.setValue(id, AutofillValue.forText(password))
                hasValue = true
            }
        }

        return if (hasValue) datasetBuilder.build() else null
    }

    companion object {
        const val EXTRA_AUTOFILL_AUTH = "extra_autofill_auth"
        private const val AUTH_REQUEST_CODE = 9001
    }
}

package com.jksalcedo.passvault.autofill

import android.app.assist.AssistStructure
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.autofill.AutofillId

data class ParsedStructure(
    val usernameId: AutofillId? = null,
    val passwordId: AutofillId? = null,
    val emailId: AutofillId? = null,
    val webDomain: String? = null,
    val packageName: String? = null
)

object StructureParser {

    private const val TAG = "PVAutofill"

    private val PASSWORD_HINTS = setOf(
        View.AUTOFILL_HINT_PASSWORD,
        "current-password", "new-password", "currentPassword", "newPassword"
    )

    private val USERNAME_HINTS = setOf(
        View.AUTOFILL_HINT_USERNAME,
        "username", "login", "accountName"
    )

    private val EMAIL_HINTS = setOf(
        View.AUTOFILL_HINT_EMAIL_ADDRESS,
        "email", "emailAddress", "e-mail"
    )

    fun parse(structure: AssistStructure): ParsedStructure {
        var usernameId: AutofillId? = null
        var passwordId: AutofillId? = null
        var emailId: AutofillId? = null
        var webDomain: String? = null

        val packageName = if (structure.windowNodeCount > 0) {
            structure.getWindowNodeAt(0).title?.toString()?.let { title ->
                if (title.contains("/")) title.substringBefore("/") else null
            }
        } else null

        for (i in 0 until structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            val rootNode = windowNode.rootViewNode ?: continue
            val result = traverseNode(rootNode)
            if (result.usernameId != null) usernameId = result.usernameId
            if (result.passwordId != null) passwordId = result.passwordId
            if (result.emailId != null) emailId = result.emailId
            if (result.webDomain != null) webDomain = result.webDomain
        }

        return ParsedStructure(
            usernameId = usernameId,
            passwordId = passwordId,
            emailId = emailId,
            webDomain = webDomain,
            packageName = packageName
        )
    }

    private fun traverseNode(node: AssistStructure.ViewNode): ParsedStructure {
        var usernameId: AutofillId? = null
        var passwordId: AutofillId? = null
        var emailId: AutofillId? = null
        var webDomain: String? = null

        if (!node.webDomain.isNullOrEmpty()) {
            webDomain = node.webDomain
        }

        val autofillId = node.autofillId
        val hints = node.autofillHints
        val inputType = node.inputType
        val htmlInfo = node.htmlInfo

        if (autofillId != null) {
            var detected = false

            // 1. Standard autofill hints (Android + W3C)
            if (!hints.isNullOrEmpty()) {
                when {
                    hints.any { h -> PASSWORD_HINTS.any { it.equals(h, ignoreCase = true) } } -> {
                        passwordId = autofillId; detected = true
                    }
                    hints.any { h -> USERNAME_HINTS.any { it.equals(h, ignoreCase = true) } } -> {
                        usernameId = autofillId; detected = true
                    }
                    hints.any { h -> EMAIL_HINTS.any { it.equals(h, ignoreCase = true) } } -> {
                        emailId = autofillId; detected = true
                    }
                }
            }

            // 2. HTML attributes (critical for web views like Facebook)
            if (!detected && htmlInfo != null) {
                val htmlTag = htmlInfo.tag?.lowercase()
                if (htmlTag == "input" || htmlTag == "textarea") {
                    val attrs = mutableMapOf<String, String>()
                    for (i in 0 until (htmlInfo.attributes?.size ?: 0)) {
                        val pair = htmlInfo.attributes?.get(i)
                        if (pair != null) {
                            attrs[pair.first.lowercase()] = pair.second?.lowercase() ?: ""
                        }
                    }

                    val htmlType = attrs["type"] ?: "text"
                    val htmlName = attrs["name"] ?: ""
                    val htmlAuto = attrs["autocomplete"] ?: ""
                    val htmlId = attrs["id"] ?: ""
                    val combined = "$htmlName $htmlAuto $htmlId"

                    Log.d(TAG, "HTML input: type=$htmlType name=$htmlName auto=$htmlAuto id=$htmlId")

                    when {
                        htmlType == "password" || htmlAuto.contains("password") -> {
                            passwordId = autofillId; detected = true
                        }
                        htmlType == "email" || htmlAuto.contains("email") ||
                                combined.containsAny("email", "e-mail", "mail") -> {
                            emailId = autofillId; detected = true
                        }
                        htmlType == "tel" || combined.containsAny("phone", "tel", "mobile") -> {
                            emailId = autofillId; detected = true // treat phone as email-type credential
                        }
                        combined.containsAny("user", "login", "account", "identifier") -> {
                            usernameId = autofillId; detected = true
                        }
                        htmlType == "text" && combined.containsAny("pass", "pwd", "secret") -> {
                            passwordId = autofillId; detected = true
                        }
                    }
                }
            }

            // 3. InputType detection
            if (!detected && isPasswordInputType(inputType)) {
                passwordId = autofillId; detected = true
            }
            if (!detected && isEmailInputType(inputType)) {
                emailId = autofillId; detected = true
            }

            // 4. Heuristic: idEntry / hint text
            if (!detected && (isTextInput(inputType) || inputType == 0)) {
                val idEntry = node.idEntry?.lowercase() ?: ""
                val hintText = node.hint?.lowercase() ?: ""
                val combined = "$idEntry $hintText"

                when {
                    combined.containsAny("pass", "pwd", "secret") -> passwordId = autofillId
                    combined.containsAny("email", "e-mail", "mail", "phone", "tel") -> emailId = autofillId
                    combined.containsAny("user", "login", "account", "identifier") -> usernameId = autofillId
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i)
            val childResult = traverseNode(child)
            if (usernameId == null) usernameId = childResult.usernameId
            if (passwordId == null) passwordId = childResult.passwordId
            if (emailId == null) emailId = childResult.emailId
            if (webDomain == null) webDomain = childResult.webDomain
        }

        return ParsedStructure(usernameId, passwordId, emailId, webDomain)
    }

    private fun isTextInput(inputType: Int): Boolean {
        return (inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT
    }

    private fun isPasswordInputType(inputType: Int): Boolean {
        if (!isTextInput(inputType)) return false
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    }

    private fun isEmailInputType(inputType: Int): Boolean {
        if (!isTextInput(inputType)) return false
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
    }

    private fun String.containsAny(vararg terms: String): Boolean {
        return terms.any { this.contains(it) }
    }
}

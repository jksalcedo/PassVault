package com.jksalcedo.passvault.autofill

import android.app.assist.AssistStructure
import android.os.Build
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

        if (autofillId != null) {
            when {
                hints?.any { it.equals(View.AUTOFILL_HINT_PASSWORD, ignoreCase = true) } == true -> {
                    passwordId = autofillId
                }
                hints?.any {
                    it.equals(View.AUTOFILL_HINT_USERNAME, ignoreCase = true) ||
                            it.equals(View.AUTOFILL_HINT_NAME, ignoreCase = true)
                } == true -> {
                    usernameId = autofillId
                }
                hints?.any {
                    it.equals(View.AUTOFILL_HINT_EMAIL_ADDRESS, ignoreCase = true)
                } == true -> {
                    emailId = autofillId
                }
                hints.isNullOrEmpty() && isTextField(inputType) -> {
                    // Fallback: use inputType heuristics
                    when {
                        isPasswordField(inputType) -> passwordId = autofillId
                        isEmailField(inputType) -> emailId = autofillId
                        usernameId == null && passwordId == null -> {
                            val idEntry = node.idEntry?.lowercase()
                            val hint = node.hint?.lowercase()
                            if (idEntry?.containsAny("user", "login", "account") == true ||
                                hint?.containsAny("user", "login", "account", "username") == true
                            ) {
                                usernameId = autofillId
                            }
                        }
                    }
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

    private fun isTextField(inputType: Int): Boolean {
        val classType = inputType and android.text.InputType.TYPE_MASK_CLASS
        return classType == android.text.InputType.TYPE_CLASS_TEXT
    }

    private fun isPasswordField(inputType: Int): Boolean {
        val variation = inputType and android.text.InputType.TYPE_MASK_VARIATION
        return variation == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                variation == android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    }

    private fun isEmailField(inputType: Int): Boolean {
        val variation = inputType and android.text.InputType.TYPE_MASK_VARIATION
        return variation == android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                variation == android.text.InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
    }

    private fun String.containsAny(vararg terms: String): Boolean {
        return terms.any { this.contains(it) }
    }
}

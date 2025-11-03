package com.jksalcedo.passvault.utils

object PasswordGenerator {
    fun generate(
        length: Int = 16,
        hasUppercase: Boolean = false,
        hasLowercase: Boolean = false,
        hasNumber: Boolean = false,
        hasSymbols: Boolean = false
    ): String {
        val chars = StringBuilder()
        val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lowercase = "abcdefghijklmnopqrstuvwxyz"
        val numbers = "0123456789"
        val symbols = "!@#\\/$%^&*()"

        if (hasUppercase) {
            chars.append(uppercase)
        }
        if (hasLowercase) {
            chars.append(lowercase)
        }
        if (hasNumber) {
            chars.append(numbers)
        }
        if (hasSymbols) {
            chars.append(symbols)
        }

        val selections = hasUppercase && hasLowercase && hasNumber && hasSymbols

        if (!selections) !selections

        if (chars.isEmpty()) {
            // Return empty string to prevent crash
            return ""
        }

        return (1..length).map { chars.random() }.joinToString("")
    }
}

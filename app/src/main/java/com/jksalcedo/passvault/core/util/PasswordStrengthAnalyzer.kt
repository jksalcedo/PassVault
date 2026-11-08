package com.jksalcedo.passvault.core.util

import kotlin.math.log2
import kotlin.math.min

/**
 * Analyzes password strength based on length, character diversity, and common patterns.
 * Lightweight implementation with zero dependencies (~8KB).
 */
object PasswordStrengthAnalyzer {

    enum class StrengthLevel {
        VERY_WEAK,
        WEAK,
        FAIR,
        GOOD,
        STRONG
    }

    data class StrengthResult(
        val level: StrengthLevel,
        val score: Int, // 0-100
        val feedback: List<String>
    )

    /**
     * Analyzes the strength of a password.
     * @param password The password to analyze
     * @return StrengthResult containing level, score, and feedback
     */
    fun analyze(password: String): StrengthResult {
        if (password.isEmpty()) {
            return StrengthResult(
                StrengthLevel.VERY_WEAK,
                0,
                listOf("Password cannot be empty")
            )
        }

        var score = 0
        val feedback = mutableListOf<String>()

        // Length scoring (0-35 points) - improved weighting
        score += when {
            password.length >= 16 -> 35
            password.length >= 14 -> 30
            password.length >= 12 -> 25
            password.length >= 10 -> 20
            password.length >= 8 -> 15
            else -> {
                feedback.add("Use at least 8 characters")
                password.length * 2
            }
        }

        // Character diversity (0-35 points) - with bonus for variety
        val hasLowercase = password.any { it.isLowerCase() }
        val hasUppercase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSymbol = password.any { !it.isLetterOrDigit() }

        val diversityCount = listOf(hasLowercase, hasUppercase, hasDigit, hasSymbol).count { it }
        score += when (diversityCount) {
            4 -> 35 // All types present - bonus!
            3 -> 25
            2 -> 15
            1 -> 5
            else -> 0
        }

        if (!hasLowercase) feedback.add("Add lowercase letters")
        if (!hasUppercase) feedback.add("Add uppercase letters")
        if (!hasDigit) feedback.add("Add numbers")
        if (!hasSymbol) feedback.add("Add symbols (!@#$%^&*)")

        // Entropy bonus (0-10 points) - rewards character variety
        val entropyBonus = min(10, (calculateEntropy(password) / 5).toInt())
        score += entropyBonus

        // Pattern detection penalties (0-20 points bonus for no patterns)
        var patternPenalty = 20

        // Check for keyboard patterns (qwerty, asdf, etc.)
        if (hasKeyboardPattern(password)) {
            patternPenalty -= 8
            feedback.add("Avoid keyboard patterns")
        }

        // Check for sequential characters (abc, 123, etc.)
        if (hasSequentialChars(password)) {
            patternPenalty -= 6
            feedback.add("Avoid sequential characters")
        }

        // Check for repeated characters (aaa, 111, etc.)
        if (hasRepeatedChars(password)) {
            patternPenalty -= 6
            feedback.add("Avoid repeated characters")
        }

        // Check for common passwords (severe penalty)
        if (isCommonPassword(password)) {
            score = min(score, 30) // Cap score at 30 for common passwords
            feedback.clear()
            feedback.add("This is a commonly used password")
            return StrengthResult(StrengthLevel.VERY_WEAK, score, feedback)
        }

        score += patternPenalty

        // Ensure score is within 0-100
        score = score.coerceIn(0, 100)

        // Determine strength level with adjusted thresholds
        val level = when {
            score >= 85 -> StrengthLevel.STRONG
            score >= 65 -> StrengthLevel.GOOD
            score >= 45 -> StrengthLevel.FAIR
            score >= 25 -> StrengthLevel.WEAK
            else -> StrengthLevel.VERY_WEAK
        }

        // Positive feedback for strong passwords
        if (feedback.isEmpty()) {
            feedback.add("Excellent password!")
        }

        return StrengthResult(level, score, feedback)
    }

    /**
     * Calculates password entropy (bits of randomness).
     */
    private fun calculateEntropy(password: String): Double {
        if (password.isEmpty()) return 0.0

        val charSetSize = when {
            password.any { it.isLowerCase() } && password.any { it.isUpperCase() } &&
                    password.any { it.isDigit() } && password.any { !it.isLetterOrDigit() } -> 94 // All types
            password.any { it.isLowerCase() } && password.any { it.isUpperCase() } &&
                    password.any { it.isDigit() } -> 62 // Letters + digits
            password.any { it.isLetter() } && password.any { it.isDigit() } -> 36 // Letters + digits (single case)
            password.any { it.isLetter() } -> 26 // Letters only
            password.any { it.isDigit() } -> 10 // Digits only
            else -> password.toSet().size // Unique characters
        }

        return password.length * log2(charSetSize.toDouble())
    }

    /**
     * Checks for common keyboard patterns.
     */
    private fun hasKeyboardPattern(password: String): Boolean {
        val patterns = listOf(
            "qwerty", "asdfgh", "zxcvbn", "qwertz", "azerty", // QWERTY layouts
            "123456", "098765", // Number rows
            "qweasd", "asdzxc", "zxcasd" // Vertical patterns
        )

        val lower = password.lowercase()
        return patterns.any { pattern ->
            lower.contains(pattern) || lower.contains(pattern.reversed())
        }
    }

    /**
     * Checks for sequential characters in the password.
     */
    private fun hasSequentialChars(password: String): Boolean {
        val lower = password.lowercase()
        for (i in 0 until lower.length - 2) {
            val char1 = lower[i]
            val char2 = lower[i + 1]
            val char3 = lower[i + 2]

            // Check for sequential letters or numbers
            if (char2 == char1 + 1 && char3 == char2 + 1) {
                return true
            }
            // Check for reverse sequential
            if (char2 == char1 - 1 && char3 == char2 - 1) {
                return true
            }
        }
        return false
    }

    /**
     * Checks for repeated characters in the password.
     */
    private fun hasRepeatedChars(password: String): Boolean {
        for (i in 0 until password.length - 2) {
            if (password[i] == password[i + 1] && password[i] == password[i + 2]) {
                return true
            }
        }
        return false
    }

    /**
     * Checks if the password is in a list of common passwords.
     * Expanded list of 50 most common passwords.
     */
    private fun isCommonPassword(password: String): Boolean {
        val commonPasswords = setOf(
            // Top 50 most common passwords
            "password", "123456", "12345678", "qwerty", "abc123", "monkey",
            "1234567", "letmein", "trustno1", "dragon", "baseball", "111111",
            "iloveyou", "master", "sunshine", "ashley", "bailey", "passw0rd",
            "shadow", "123123", "654321", "superman", "qazwsx", "michael",
            "football", "password1", "welcome", "admin", "login", "princess",
            "solo", "qwertyuiop", "starwars", "123456789", "1234567890",
            "password123", "1q2w3e4r", "12345", "1234", "000000", "password!",
            "abc12345", "123qwe", "monkey123", "1qaz2wsx", "password1!",
            "qwerty123", "123abc", "letmein123", "admin123"
        )
        return commonPasswords.contains(password.lowercase())
    }

    /**
     * Gets a color resource ID based on strength level.
     * Note: This returns a color name that should be defined in colors.xml
     */
    fun getStrengthColorName(level: StrengthLevel): String {
        return when (level) {
            StrengthLevel.VERY_WEAK -> "strength_very_weak"
            StrengthLevel.WEAK -> "strength_weak"
            StrengthLevel.FAIR -> "strength_fair"
            StrengthLevel.GOOD -> "strength_good"
            StrengthLevel.STRONG -> "strength_strong"
        }
    }

    /**
     * Gets a display string for the strength level.
     */
    fun getStrengthLabel(level: StrengthLevel): String {
        return when (level) {
            StrengthLevel.VERY_WEAK -> "Very Weak"
            StrengthLevel.WEAK -> "Weak"
            StrengthLevel.FAIR -> "Fair"
            StrengthLevel.GOOD -> "Good"
            StrengthLevel.STRONG -> "Strong"
        }
    }
}

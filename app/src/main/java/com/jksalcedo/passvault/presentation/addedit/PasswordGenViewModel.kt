package com.jksalcedo.passvault.presentation.addedit

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import com.jksalcedo.passvault.core.util.PasswordGenerator

/**
 * Options used to generate a password.
 */
data class GeneratorOptions(
    val length: Int = 16,
    val uppercase: Boolean = true,
    val lowercase: Boolean = true,
    val numbers: Boolean = true,
    val symbols: Boolean = false
)

/**
 * ViewModel for the password generator dialog.
 *
 * Owns the persisted generator options and the generation itself; the dialog only binds views.
 */
class PasswordGenViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs =
        application.getSharedPreferences("password_gen_prefs", Context.MODE_PRIVATE)

    var options: GeneratorOptions = loadOptions()
        private set

    fun saveOptions(options: GeneratorOptions) {
        this.options = options
        prefs.edit {
            putInt("length", options.length)
            putBoolean("uppercase", options.uppercase)
            putBoolean("lowercase", options.lowercase)
            putBoolean("numbers", options.numbers)
            putBoolean("symbols", options.symbols)
        }
    }

    fun generate(options: GeneratorOptions = this.options): String =
        PasswordGenerator.generate(
            length = options.length,
            hasUppercase = options.uppercase,
            hasLowercase = options.lowercase,
            hasNumber = options.numbers,
            hasSymbols = options.symbols
        )

    private fun loadOptions(): GeneratorOptions = GeneratorOptions(
        length = prefs.getInt("length", 16),
        uppercase = prefs.getBoolean("uppercase", true),
        lowercase = prefs.getBoolean("lowercase", true),
        numbers = prefs.getBoolean("numbers", true),
        symbols = prefs.getBoolean("symbols", false)
    )
}

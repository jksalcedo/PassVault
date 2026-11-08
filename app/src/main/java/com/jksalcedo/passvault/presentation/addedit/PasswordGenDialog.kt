package com.jksalcedo.passvault.presentation.addedit

import android.app.Dialog
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.core.util.PasswordStrengthAnalyzer
import com.jksalcedo.passvault.core.util.Utility
import com.jksalcedo.passvault.databinding.DialogPasswordGenBinding

/**
 * safe way to send the generated password back to the Activity
 */
interface PasswordDialogListener {
    fun onPasswordGenerated(password: String)
}

/**
 * A dialog for generating passwords.
 */
class PasswordGenDialog : DialogFragment() {

    private var generatedPassword: String = ""
    private lateinit var binding: DialogPasswordGenBinding
    private lateinit var viewModel: PasswordGenViewModel
    private var listener: PasswordDialogListener? = null

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        listener = context as? PasswordDialogListener
        if (listener == null) {
            throw ClassCastException("$context must implement PasswordDialogListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogPasswordGenBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[PasswordGenViewModel::class.java]

        // Load saved values or use defaults
        val savedOptions = viewModel.options
        binding.sbLength.progress = savedOptions.length
        binding.tvLength.text = savedOptions.length.toString()
        binding.sbLength.min = 4

        binding.sbUppercase.isChecked = savedOptions.uppercase
        binding.sbLowercase.isChecked = savedOptions.lowercase
        binding.sbNumbers.isChecked = savedOptions.numbers
        binding.cbSymbols.isChecked = savedOptions.symbols

        binding.sbLength.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // update length text
                binding.tvLength.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle(R.string.generate_password)
            .setCancelable(false)
            // This button generates the password and shows it, but does nor close the dialog
            .setPositiveButton(R.string.generate, null)

            .setNegativeButton(R.string.cancel) { _, _ ->
                dismiss() //  close the dialog
            }

            // Copy button
            .setNeutralButton(R.string.copy_use) { _, _ ->
                if (generatedPassword.isNotEmpty()) {
                    // Send the password back to the Activity
                    listener?.onPasswordGenerated(generatedPassword)
                    Utility.copyToClipboard(
                        requireContext(),
                        getString(R.string.password),
                        generatedPassword
                    )
                    Toast.makeText(requireContext(), R.string.password_saved, Toast.LENGTH_SHORT)
                        .show()
                    dismiss() // Close the dialog
                } else {
                    Toast.makeText(
                        requireContext(),
                        R.string.generate_first,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val length = binding.sbLength.progress
                val hasUppercase = binding.sbUppercase.isChecked
                val hasLowercase = binding.sbLowercase.isChecked
                val hasNumber = binding.sbNumbers.isChecked
                val hasSymbols = binding.cbSymbols.isChecked

                val options = GeneratorOptions(
                    length = length,
                    uppercase = hasUppercase,
                    lowercase = hasLowercase,
                    numbers = hasNumber,
                    symbols = hasSymbols
                )

                // Save preferences
                viewModel.saveOptions(options)

                // Generate the password
                generatedPassword = viewModel.generate(options)

                // Update the UI
                if (generatedPassword.isNotEmpty()) {
                    binding.tvPassword.text = generatedPassword
                    updatePasswordStrength(generatedPassword)
                } else {
                    Toast.makeText(
                        requireContext(),
                        R.string.select_char_type,
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.chipPasswordStrength.visibility = android.view.View.GONE
                }
            }
        }

        return dialog
    }

    /**
     * Updates the password strength indicator.
     */
    private fun updatePasswordStrength(password: String) {
        if (password.isEmpty()) {
            binding.chipPasswordStrength.visibility = android.view.View.GONE
            return
        }

        binding.chipPasswordStrength.visibility = android.view.View.VISIBLE
        val result = PasswordStrengthAnalyzer.analyze(password)
        val strengthLabel = PasswordStrengthAnalyzer.getStrengthLabel(result.level)
        binding.chipPasswordStrength.text = strengthLabel

        val colorResId = when (result.level) {
            PasswordStrengthAnalyzer.StrengthLevel.VERY_WEAK -> R.color.strength_very_weak
            PasswordStrengthAnalyzer.StrengthLevel.WEAK -> R.color.strength_weak
            PasswordStrengthAnalyzer.StrengthLevel.FAIR -> R.color.strength_fair
            PasswordStrengthAnalyzer.StrengthLevel.GOOD -> R.color.strength_good
            PasswordStrengthAnalyzer.StrengthLevel.STRONG -> R.color.strength_strong
        }
        binding.chipPasswordStrength.setChipBackgroundColorResource(colorResId)
        binding.chipPasswordStrength.setTextColor(
            ContextCompat.getColor(requireContext(), android.R.color.white)
        )
    }

    /**
     * Clean up the listener reference
     */
    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}

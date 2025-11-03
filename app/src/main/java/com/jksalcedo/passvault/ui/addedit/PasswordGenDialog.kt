package com.jksalcedo.passvault.ui.addedit

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.databinding.DialogPasswordGenBinding
import com.jksalcedo.passvault.utils.ClipboardUtil
import com.jksalcedo.passvault.utils.PasswordGenerator

/**
 * safe way to send the generated password back to the Activity
 */
interface PasswordDialogListener {
    fun onPasswordGenerated(password: String)
}

class PasswordGenDialog : DialogFragment() {

    private var generatedPassword: String = ""
    private lateinit var binding: DialogPasswordGenBinding
    private var listener: PasswordDialogListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? PasswordDialogListener
        if (listener == null) {
            throw ClassCastException("$context must implement PasswordDialogListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogPasswordGenBinding.inflate(layoutInflater)

        binding.sbLength.progress = 16 // default
        binding.tvLength.text = getString(R.string._16)
        binding.sbUppercase.isChecked = true
        binding.sbLowercase.isChecked = true
        binding.sbNumbers.isChecked = true

        binding.sbLength.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // update length text
                binding.tvLength.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setCancelable(false)

            // This button generates the password and shows it, but does nor close the dialog
            .setPositiveButton("Generate", null)

            .setNegativeButton("Cancel") { _, _ ->
                dismiss() //  close the dialog
            }

            // Copy button
            .setNeutralButton("Copy & Use") { _, _ ->
                if (generatedPassword.isNotEmpty()) {
                    // Send the password back to the Activity
                    listener?.onPasswordGenerated(generatedPassword)
                    ClipboardUtil.copyToClipboard(requireContext(), "password", generatedPassword)
                    Toast.makeText(requireContext(), "Password copied", Toast.LENGTH_SHORT).show()
                    dismiss() // Close the dialog
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Generate a password first",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                // Read the UI state right when the button is clicked
                val length = binding.sbLength.progress
                val hasUppercase = binding.sbUppercase.isChecked
                val hasLowercase = binding.sbLowercase.isChecked
                val hasNumber = binding.sbNumbers.isChecked
                val hasSymbols = binding.cbSymbols.isChecked

                // Generate the password
                generatedPassword = PasswordGenerator.generate(
                    length = length,
                    hasUppercase = hasUppercase,
                    hasLowercase = hasLowercase,
                    hasNumber = hasNumber,
                    hasSymbols = hasSymbols
                )

                // Update the UI
                if (generatedPassword.isNotEmpty()) {
                    binding.tvPassword.text = generatedPassword
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Select at least one character type",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        return dialog
    }

    /**
     * Clean up the listener reference
     */
    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
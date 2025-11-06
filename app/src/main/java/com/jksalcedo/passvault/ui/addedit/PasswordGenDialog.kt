package com.jksalcedo.passvault.ui.addedit

import android.app.Dialog
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.jksalcedo.passvault.databinding.DialogPasswordGenBinding
import com.jksalcedo.passvault.utils.PasswordGenerator

class PasswordGenDialog : DialogFragment() {

    interface PasswordGeneratedListener {
        fun onPasswordGenerated(password: String)
    }

    private var length = 12
    private var hasUppercase: Boolean = true
    private var hasLowercase: Boolean = true
    private var hasNumber: Boolean = true
    private var hasSymbols: Boolean = false

    private var password: String = ""
    private lateinit var binding: DialogPasswordGenBinding
    private var listener: PasswordGeneratedListener? = null

    fun setPasswordGeneratedListener(listener: PasswordGeneratedListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogPasswordGenBinding.inflate(layoutInflater)

        setupControls()
        generatePassword()

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("OK") { _, _ ->
                listener?.onPasswordGenerated(password)
            }.setNegativeButton("Cancel", null)
            .create()
    }

    private fun setupControls() {
        binding.sbLength.progress = length
        binding.tvLength.text = length.toString()
        binding.sbUppercase.isChecked = hasUppercase
        binding.sbLowercase.isChecked = hasLowercase
        binding.sbNumbers.isChecked = hasNumber
        binding.cbSymbols.isChecked = hasSymbols

        binding.sbLength.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                length = progress
                binding.tvLength.text = progress.toString()
                generatePassword()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.sbUppercase.setOnCheckedChangeListener { _, isChecked ->
            hasUppercase = isChecked
            generatePassword()
        }

        binding.sbLowercase.setOnCheckedChangeListener { _, isChecked ->
            hasLowercase = isChecked
            generatePassword()
        }

        binding.sbNumbers.setOnCheckedChangeListener { _, isChecked ->
            hasNumber = isChecked
            generatePassword()
        }

        binding.cbSymbols.setOnCheckedChangeListener { _, isChecked ->
            hasSymbols = isChecked
            generatePassword()
        }
    }

    private fun generatePassword() {
        password = PasswordGenerator.generate(
            length = length,
            hasUppercase = hasUppercase,
            hasLowercase = hasLowercase,
            hasNumber = hasNumber,
            hasSymbols = hasSymbols
        )
        binding.tvPassword.text = password
    }
}

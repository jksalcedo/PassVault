package com.jksalcedo.passvault.ui.auth

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment

/**
 * DialogFragment to set and store a master PIN.
 */
class SetPinFragment : DialogFragment() {

    interface OnPinSetListener {
        fun onPinSet(pin: String)
    }

    private var listener: OnPinSetListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = when {
            parentFragment is OnPinSetListener -> parentFragment as OnPinSetListener
            context is OnPinSetListener -> context
            else -> null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate a simple view programmatically to avoid adding a new binding dependency
        val root =
            inflater.inflate(com.jksalcedo.passvault.R.layout.fragment_set_pin, container, false)
        val etPin = root.findViewById<EditText>(com.jksalcedo.passvault.R.id.etNewPin)
        val etConfirm = root.findViewById<EditText>(com.jksalcedo.passvault.R.id.etConfirmPin)
        val btnSave = root.findViewById<Button>(com.jksalcedo.passvault.R.id.btnSavePin)
        val btnCancel = root.findViewById<Button>(com.jksalcedo.passvault.R.id.btnCancelPin)

        btnSave.setOnClickListener {
            val pin = etPin.text?.toString()?.trim().orEmpty()
            val confirm = etConfirm.text?.toString()?.trim().orEmpty()

            if (pin.length < 4) {
                etPin.error = "PIN must be 4 digits"
                return@setOnClickListener
            }
            if (pin != confirm) {
                etConfirm.error = "PINs do not match"
                return@setOnClickListener
            }

            val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
            prefs.edit { putString("pin", pin) }
            Toast.makeText(requireContext(), "PIN set successfully", Toast.LENGTH_SHORT).show()
            listener?.onPinSet(pin)
            dismiss()
        }

        btnCancel.setOnClickListener { dismiss() }

        return root
    }
}

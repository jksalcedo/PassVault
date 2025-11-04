package com.jksalcedo.passvault.ui.auth

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import com.jksalcedo.passvault.crypto.Encryption
import com.jksalcedo.passvault.databinding.FragmentSetPinBinding
import com.jksalcedo.passvault.utils.Utility

/**
 * DialogFragment to set and store a master PIN.
 */
class SetPinFragment : DialogFragment() {

    interface OnPinSetListener {
        fun onPinSet(pin: String)
    }

    private var listener: OnPinSetListener? = null
    private var _binding: FragmentSetPinBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = when {
            parentFragment is OnPinSetListener -> parentFragment as OnPinSetListener
            context is OnPinSetListener -> context
            else -> null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentSetPinBinding.inflate(layoutInflater)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = binding.root
        val etPin = binding.etNewPin
        val etConfirm = root.findViewById<EditText>(com.jksalcedo.passvault.R.id.etConfirmPin)
        val btnSave = root.findViewById<Button>(com.jksalcedo.passvault.R.id.btnSavePin)
        val btnCancel = root.findViewById<Button>(com.jksalcedo.passvault.R.id.btnCancelPin)

        btnSave.setOnClickListener {
            val pin = etPin.text?.toString()?.trim().orEmpty()
            val confirm = etConfirm.text?.toString()?.trim().orEmpty()

            if (pin.length < 4) {
                etPin.error = "PIN must be at least 4 digits and 6 digits at most"
                return@setOnClickListener
            }
            if (pin != confirm) {
                etConfirm.error = "PINs do not match"
                return@setOnClickListener
            }

            try {
                Encryption.ensureKeyExists()

                val (encryptedPin, iv) = Encryption.encrypt(pin)
                val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
                prefs.edit {
                    putString("pin", encryptedPin)
                    putString("pin_iv", iv)
                }

                Toast.makeText(requireContext(), "PIN set successfully", Toast.LENGTH_SHORT).show()
                listener?.onPinSet(pin)
                dismiss() // dismiss the dialog
            } catch (_: Exception) {
                Utility.showToast(requireContext(), "Error setting PIN")
                return@setOnClickListener
            }
        }

        btnCancel.setOnClickListener { dismiss() }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

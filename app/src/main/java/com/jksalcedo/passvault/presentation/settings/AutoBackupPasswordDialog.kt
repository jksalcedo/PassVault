package com.jksalcedo.passvault.presentation.settings

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.data.repository.PreferenceRepository

/**
 * Shared password setup dialog for automatic backups.
 *
 * Used both when the user enables auto backups and when an upgrade left auto backups
 * enabled without a password. The caller decides what to do on success/cancel so the
 * dialog stays independent of any particular screen.
 */
object AutoBackupPasswordDialog {

    fun show(
        activity: FragmentActivity,
        prefs: PreferenceRepository,
        onPasswordSet: (String) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        val layout = activity.layoutInflater.inflate(R.layout.dialog_set_password, null)
        val etPassword = layout.findViewById<TextInputEditText>(R.id.et_password)
        val etConfirmPassword = layout.findViewById<TextInputEditText>(R.id.et_confirm_password)
        val til1 = layout.findViewById<TextInputLayout>(R.id.til1)
        val til2 = layout.findViewById<TextInputLayout>(R.id.til2)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("Set password for automatic backups")
            .setMessage("This will be needed when you import or restore automatic backup files.")
            .setView(layout)
            .setCancelable(false)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel") { _, _ ->
                onCancel()
            }
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val password = etPassword.text.toString()
                val confirm = etConfirmPassword.text.toString()

                til1.error = null
                til2.error = null

                when {
                    password.isBlank() -> {
                        til1.error = "Password cannot be empty"
                    }

                    password.length < 4 -> {
                        til1.error = "Password too short"
                    }

                    password != confirm -> {
                        til2.error = "Passwords do not match"
                    }

                    else -> {
                        prefs.setPasswordForAutoBackups(password)
                        onPasswordSet(password)
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }
}

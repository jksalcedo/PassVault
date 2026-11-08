package com.jksalcedo.passvault.presentation.settings

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.core.ui.adapter.BackupAdapter
import com.jksalcedo.passvault.core.util.Utility
import com.jksalcedo.passvault.databinding.FragmentBackupsBinding

/**
 * A fragment for managing backups.
 */
class BackupsFragment : Fragment() {

    private var _binding: FragmentBackupsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: BackupAdapter
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsModelFactory(
            application = requireActivity().application
        )
    }
    private lateinit var backupItem: com.jksalcedo.passvault.data.model.BackupItem

    private val openFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    try {
                        settingsViewModel.copyBackupToUri(backupItem, uri)
                        Utility.showToast(requireContext(), "Backup file copied successfully!.")
                    } catch (_: Exception) {
                        Utility.showToast(requireContext(), "Process failed.")
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupsBinding.inflate(layoutInflater)

        adapter = BackupAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this.requireContext())
        binding.recyclerView.adapter = adapter

        (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.manage_backups)

        // Observe backups from the ViewModel
        settingsViewModel.backups.observe(viewLifecycleOwner) { items ->
            adapter.setBackups(items)
            val showEmpty = items.isEmpty()
            binding.tvNoBackup.visibility = if (showEmpty) View.VISIBLE else View.GONE
            binding.ivNoBackup.visibility = if (showEmpty) View.VISIBLE else View.GONE
        }
        settingsViewModel.refreshBackups()

        settingsViewModel.importUiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ImportUiState.Loading -> {
                }

                is ImportUiState.Success -> {
                    Utility.showToast(
                        requireContext(),
                        "Restored ${state.count} entries successfully!"
                    )
                    settingsViewModel.resetImportState()
                }

                is ImportUiState.Error -> {
                    Utility.showToast(
                        requireContext(),
                        "Restore failed: ${state.exception.message}"
                    )
                    settingsViewModel.resetImportState()
                }

                else -> {}
            }
        }

        adapter.onItemClick = { item ->
            backupItem = item
            var selectedPosition = 0
            MaterialAlertDialogBuilder(this.requireContext())
                .setTitle(item.name)
                .setSingleChoiceItems(R.array.backup_options, 0) { _, which ->
                    selectedPosition = which
                }
                .setPositiveButton("Proceed") { _, _ ->
                    when (selectedPosition) {
                        // Restore
                        0 -> {
                            showRestorePasswordDialog(item)
                        }
                        // Save & Copy
                        1 -> {
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "application/octet-stream"
                                putExtra(Intent.EXTRA_TITLE, item.name)
                            }
                            openFileLauncher.launch(intent)
                        }
                        // Delete
                        2 -> {
                            try {
                                if (settingsViewModel.deleteBackup(item)) {
                                    Utility.showToast(requireContext(), "Backup file deleted.")
                                    // Refresh the list after deleting
                                    settingsViewModel.refreshBackups()
                                } else {
                                    Utility.showToast(requireContext(), "Failed to delete backup.")
                                }
                            } catch (e: Exception) {
                                Utility.showToast(
                                    requireContext(),
                                    "Error deleting backup file: $e."
                                )
                            }
                        }
                        // Share
                        3 -> {
                            try {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    putExtra(Intent.EXTRA_STREAM, item.uri)
                                    type = "*/*"
                                }
                                startActivity(intent)
                            } catch (e: Exception) {
                                Utility.showToast(requireContext(), "Sharing failed: $e")
                                Log.e(this.toString(), e.toString())
                            }
                        }
                    }
                }
                .show()
        }

        return binding.root
    }

    private fun showRestorePasswordDialog(item: com.jksalcedo.passvault.data.model.BackupItem) {
        val layout = layoutInflater.inflate(R.layout.dialog_enter_password, null)
        val etPassword =
            layout.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_password)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Restore Backup")
            .setMessage("Enter the password for this backup (if encrypted). Leave empty if not encrypted.")
            .setView(layout)
            .setPositiveButton("Restore") { _, _ ->
                val password = etPassword.text.toString()
                settingsViewModel.restoreBackup(item, password)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.action_settings)
    }
}
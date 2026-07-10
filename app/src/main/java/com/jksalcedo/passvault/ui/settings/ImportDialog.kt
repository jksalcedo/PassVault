package com.jksalcedo.passvault.ui.settings

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jksalcedo.passvault.databinding.DialogImportBinding
import com.jksalcedo.passvault.data.enums.ImportType
import com.jksalcedo.passvault.utils.Utility
import com.jksalcedo.passvault.viewmodel.SettingsModelFactory
import com.jksalcedo.passvault.viewmodel.SettingsViewModel

/**
 * A bottom sheet dialog for importing passwords from a file.
 */
class ImportDialog : BottomSheetDialogFragment() {

    private var _binding: DialogImportBinding? = null
    private val binding get() = _binding!!
    private var settingsActivity: SettingsActivity? = null
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsModelFactory(
            application = requireActivity().application
        )
    }

    private lateinit var type: ImportType

    companion object {
        const val TAG = "ImportDialog"
    }

    private val openFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    when (type) {
                        ImportType.KEEPASS_KDBX -> {
                            settingsActivity?.ensurePasswordExists(true) { password ->
                                settingsViewModel.startImport(uri, type, password)
                            }
                        }

                        ImportType.PASSVAULT_JSON, ImportType.PASSVAULT_CSV -> {
                            settingsActivity?.ensurePasswordExists(true) { password ->
                                settingsViewModel.startImport(uri, type, password)
                            }
                        }

                        else -> {
                            settingsViewModel.startImport(uri, type, "")
                        }
                    }
                }
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is SettingsActivity) {
            settingsActivity = context
        } else {
            throw IllegalStateException("The activity must be SettingsActivity.")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogImportBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this.requireContext())
        dialog.apply {
            setTitle("Import from file")
            setContentView(binding.root)
            setOnShowListener {
                binding.tilPassword.visibility = View.GONE
                prepareImport(ImportType.PASSVAULT_JSON)
                val rg = binding.radioGroup
                rg.setOnCheckedChangeListener { _, checkedId ->
                    when (checkedId) {
                        binding.mrbBitwardenJson.id -> prepareImport(ImportType.BITWARDEN_JSON)
                        binding.mrbKeepassCsv.id -> prepareImport(ImportType.KEEPASS_CSV)
                        binding.mrbKeepassKdbx.id -> prepareImport(ImportType.KEEPASS_KDBX)
                        binding.mrbPassvaultJson.id -> prepareImport(ImportType.PASSVAULT_JSON)
                        binding.mrbPassvaultCsv.id -> prepareImport(ImportType.PASSVAULT_CSV)
                        binding.mrbChromeCsv.id -> prepareImport(ImportType.CHROME_CSV)
                    }
                }
            }
        }

        observeImportUiState()

        return dialog
    }

    /**
     * Prepares the import process based on the selected import type.
     * @param importType The type of import to prepare for.
     */
    private fun prepareImport(importType: ImportType) {
        binding.btProceed.setOnClickListener {
            type = importType
            openFileForImport()
        }
    }

    /**
     * Opens a file picker for the user to select a file to import.
     */
    private fun openFileForImport() {
        val mimeTypes = when (type) {
            ImportType.BITWARDEN_JSON, ImportType.PASSVAULT_JSON -> arrayOf(
                "application/json",
                "text/plain",
                "application/octet-stream"
            )

            ImportType.KEEPASS_CSV, ImportType.PASSVAULT_CSV, ImportType.CHROME_CSV -> arrayOf(
                "text/csv",
                "text/comma-separated-values",
                "text/plain",
                "application/csv",
                "application/excel",
                "application/vnd.ms-excel",
                "application/vnd.msexcel",
                "application/octet-stream"
            )

            ImportType.KEEPASS_KDBX -> arrayOf(
                "application/octet-stream",
                "application/x-keepass2",
                "application/kdbx"
            )
        }

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            this.type = "*/*" // Allow all, then filter with EXTRA_MIME_TYPES
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
        openFileLauncher.launch(intent)
    }

    /**
     * Observes the import UI state and updates the UI accordingly.
     */
    private fun observeImportUiState() {
        settingsViewModel.importUiState.observe(this) { state ->
            when (state) {
                is ImportUiState.Loading -> {
                    binding.progressImporter.visibility = View.VISIBLE
                    binding.tvProgress.visibility = View.VISIBLE
                    binding.tvProgress.text = "Importing ${state.progress} of ${state.total}..."
                    
                    binding.btProceed.isEnabled = false
                    for (i in 0 until binding.radioGroup.childCount) {
                        binding.radioGroup.getChildAt(i).isEnabled = false
                    }
                }

                is ImportUiState.Success -> {
                    binding.progressImporter.visibility = View.GONE
                    binding.tvProgress.visibility = View.GONE
                    ImportStatusDialog(state.results).show(
                        parentFragmentManager,
                        ImportStatusDialog.TAG
                    )
                    settingsViewModel.resetImportState()
                    dismiss()
                }

                is ImportUiState.Error -> {
                    binding.progressImporter.visibility = View.GONE
                    binding.tvProgress.visibility = View.GONE
                    binding.btProceed.isEnabled = true
                    for (i in 0 until binding.radioGroup.childCount) {
                        binding.radioGroup.getChildAt(i).isEnabled = true
                    }
                    Utility.showToast(requireContext(), "Error: ${state.exception.message}")
                    settingsViewModel.resetImportState()
                    dismiss()
                }

                is ImportUiState.Idle -> {
                    binding.progressImporter.visibility = View.GONE
                    binding.tvProgress.visibility = View.GONE
                    binding.btProceed.isEnabled = true
                    for (i in 0 until binding.radioGroup.childCount) {
                        binding.radioGroup.getChildAt(i).isEnabled = true
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
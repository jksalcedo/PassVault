package com.jksalcedo.passvault.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.core.ui.adapter.ImportStatusAdapter
import com.jksalcedo.passvault.data.model.ImportResult

class ImportStatusDialog(private val results: List<ImportResult>) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_import_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvStatus = view.findViewById<RecyclerView>(R.id.rv_import_status)
        val btClose = view.findViewById<MaterialButton>(R.id.bt_close)

        rvStatus.layoutManager = LinearLayoutManager(requireContext())
        rvStatus.adapter = ImportStatusAdapter(results)

        btClose.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        const val TAG = "ImportStatusDialog"
    }
}

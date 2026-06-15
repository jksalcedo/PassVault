package com.jksalcedo.passvault.ui.main

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jksalcedo.passvault.adapter.PVAdapter
import com.jksalcedo.passvault.databinding.ActivityTrashBinding
import com.jksalcedo.passvault.ui.base.BaseActivity
import com.jksalcedo.passvault.viewmodel.PasswordViewModel

class TrashActivity : BaseActivity() {

    private lateinit var binding: ActivityTrashBinding
    private lateinit var viewModel: PasswordViewModel
    private lateinit var adapter: PVAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[PasswordViewModel::class.java]
        
        viewModel.purgeOldDeletedEntries()

        adapter = PVAdapter(this)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        adapter.onItemClick = { entry ->
            MaterialAlertDialogBuilder(this)
                .setTitle(entry.title)
                .setItems(arrayOf("Restore", "Delete Permanently")) { _, which ->
                    when (which) {
                        0 -> viewModel.restoreFromTrash(entry.id)
                        1 -> {
                            MaterialAlertDialogBuilder(this)
                                .setTitle("Permanent Delete")
                                .setMessage("This action cannot be undone. Are you sure?")
                                .setPositiveButton("Delete") { _, _ ->
                                    viewModel.delete(entry)
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                    }
                }
                .show()
        }

        viewModel.getDeletedEntries().observe(this) { list ->
            adapter.submitList(list)
            if (list.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.layoutEmpty.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

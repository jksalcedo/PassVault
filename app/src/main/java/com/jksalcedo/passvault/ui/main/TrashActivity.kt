package com.jksalcedo.passvault.ui.main

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.adapter.PVAdapter
import com.jksalcedo.passvault.databinding.ActivityTrashBinding
import com.jksalcedo.passvault.ui.base.BaseActivity
import com.jksalcedo.passvault.viewmodel.PasswordViewModel

class TrashActivity : BaseActivity() {

    private lateinit var binding: ActivityTrashBinding
    private lateinit var viewModel: PasswordViewModel
    private lateinit var adapter: PVAdapter
    private var hasTrashedEntries = false

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
                .setItems(arrayOf(getString(R.string.restore), getString(R.string.delete_permanently))) { _, which ->
                    when (which) {
                        0 -> viewModel.restoreFromTrash(entry.id)
                        1 -> {
                            MaterialAlertDialogBuilder(this)
                                .setTitle(getString(R.string.delete_permanently))
                                .setMessage(getString(R.string.permanent_delete_confirmation))
                                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                                    viewModel.delete(entry)
                                }
                                .setNegativeButton(getString(R.string.cancel), null)
                                .show()
                        }
                    }
                }
                .show()
        }

        viewModel.getDeletedEntries().observe(this) { list ->
            adapter.submitList(list)
            hasTrashedEntries = list.isNotEmpty()
            invalidateOptionsMenu()
            if (list.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.layoutEmpty.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_trash, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val emptyItem = menu.findItem(R.id.action_empty_trash)
        emptyItem?.isVisible = hasTrashedEntries
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_empty_trash -> {
                showEmptyTrashDialog()
                true
            }
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showEmptyTrashDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.empty_trash_confirm_title))
            .setMessage(getString(R.string.empty_trash_confirm_msg))
            .setPositiveButton(getString(R.string.empty_trash)) { _, _ ->
                viewModel.emptyTrash()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

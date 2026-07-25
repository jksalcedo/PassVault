package com.jksalcedo.passvault.ui.main

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.adapter.AuditAdapter
import com.jksalcedo.passvault.adapter.HeaderAdapter
import com.jksalcedo.passvault.crypto.Encryption
import com.jksalcedo.passvault.data.PasswordEntry
import com.jksalcedo.passvault.data.enums.EntryType
import com.jksalcedo.passvault.databinding.ActivityHealthAuditBinding
import com.jksalcedo.passvault.ui.base.BaseActivity
import com.jksalcedo.passvault.ui.view.ViewEntryActivity
import com.jksalcedo.passvault.utils.PasswordStrengthAnalyzer
import com.jksalcedo.passvault.viewmodel.PasswordViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class HealthAuditActivity : BaseActivity() {

    private lateinit var binding: ActivityHealthAuditBinding
    private lateinit var viewModel: PasswordViewModel

    private lateinit var weakHeader: HeaderAdapter
    private lateinit var weakAdapter: AuditAdapter

    private lateinit var reusedHeader: HeaderAdapter
    private lateinit var reusedAdapter: AuditAdapter

    private lateinit var oldHeader: HeaderAdapter
    private lateinit var oldAdapter: AuditAdapter

    private val allAuditAdapters get() = listOf(weakAdapter, reusedAdapter, oldAdapter)

    /** Grouped by decrypted password value; used for the "Auto-clean" duplicate action. */
    private var duplicateGroups: List<List<PasswordEntry>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHealthAuditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[PasswordViewModel::class.java]

        setupRecyclerView()
        setupSelectionBar()
        setupBackPress()
        performAudit()
    }

    private fun setupRecyclerView() {
        val colorWeak = ContextCompat.getColor(this, R.color.strength_very_weak)
        val colorReused = ContextCompat.getColor(this, R.color.strength_weak)
        val colorOld = ContextCompat.getColor(this, R.color.strength_fair)

        weakAdapter = buildAuditAdapter(viewTypeId = 1)
        weakHeader = HeaderAdapter(
            title = getString(R.string.audit_header_weak),
            textColor = colorWeak,
            onToggleCollapse = { toggleSection(0) },
        )

        reusedAdapter = buildAuditAdapter(viewTypeId = 2)
        reusedHeader = HeaderAdapter(
            title = getString(R.string.audit_header_reused),
            textColor = colorReused,
            onToggleCollapse = { toggleSection(1) },
            onSmartAction = { showDuplicateDialog() },
        )

        oldAdapter = buildAuditAdapter(viewTypeId = 3)
        oldHeader = HeaderAdapter(
            title = getString(R.string.audit_header_old),
            textColor = colorOld,
            onToggleCollapse = { toggleSection(2) },
        )

        // isolateViewTypes=true: each adapter has its own ViewHolder pool, preventing
        // stale checked states from bleeding across sections.
        val config = ConcatAdapter.Config.Builder()
            .setIsolateViewTypes(true)
            .build()

        val concatAdapter = ConcatAdapter(
            config,
            weakHeader, weakAdapter,
            reusedHeader, reusedAdapter,
            oldHeader, oldAdapter,
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = concatAdapter
    }

    private fun buildAuditAdapter(viewTypeId: Int) = AuditAdapter(this, viewTypeId).apply {
        onItemClick = { entry -> startActivity(ViewEntryActivity.createIntent(this@HealthAuditActivity, entry)) }
        onSelectionModeChanged = { active -> onAnySelectionModeChanged(active) }
        onSelectionChanged = { _ -> refreshSelectionCount() }
    }

    // ── Selection bar ────────────────────────────────────────────────────────

    private fun setupSelectionBar() {
        binding.btnSelectAll.setOnClickListener {
            allAuditAdapters.filter { !it.isCollapsed }.forEach { it.selectAll() }
            refreshSelectionCount()
        }

        binding.btnMoveToTrash.setOnClickListener {
            val selected = collectSelected()
            if (selected.isEmpty()) return@setOnClickListener
            viewModel.moveToTrashBulk(selected.map { it.id })
            clearAllSelections()
        }

        binding.btnDeletePermanently.setOnClickListener {
            val selected = collectSelected()
            if (selected.isEmpty()) return@setOnClickListener
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_permanently)
                .setMessage(getString(R.string.audit_delete_confirm, selected.size))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete_permanently) { _, _ ->
                    viewModel.deleteBulk(selected)
                    clearAllSelections()
                }
                .show()
        }
    }

    private fun onAnySelectionModeChanged(active: Boolean) {
        val anyActive = allAuditAdapters.any { it.isSelectionMode }
        if (anyActive) {
            binding.selectionBar.visibility = View.VISIBLE
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
        } else {
            binding.selectionBar.visibility = View.GONE
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
        refreshSelectionCount()
    }

    private fun refreshSelectionCount() {
        val total = allAuditAdapters.sumOf { it.getSelectedCount() }
        binding.tvSelectionCount.text = getString(R.string.audit_selected_count, total)
    }

    private fun collectSelected(): List<PasswordEntry> =
        allAuditAdapters.flatMap { it.getSelectedEntries() }

    private fun clearAllSelections() {
        allAuditAdapters.forEach { it.clearSelection() }
        binding.selectionBar.visibility = View.GONE
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    // ── Collapse / expand ────────────────────────────────────────────────────

    private fun toggleSection(index: Int) {
        val (header, adapter) = when (index) {
            0 -> weakHeader to weakAdapter
            1 -> reusedHeader to reusedAdapter
            else -> oldHeader to oldAdapter
        }
        val nowCollapsed = !adapter.isCollapsed
        adapter.setCollapsed(nowCollapsed)
        header.setCollapsed(nowCollapsed)
    }

    // ── Duplicate smart-clean ────────────────────────────────────────────────

    private fun showDuplicateDialog() {
        if (duplicateGroups.isEmpty()) return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.audit_handle_duplicates)
            .setMessage(getString(R.string.audit_handle_duplicates_msg, duplicateGroups.size))
            .setNeutralButton(R.string.cancel, null)
            .setNegativeButton(R.string.audit_remove_all_duplicates) { _, _ ->
                val all = duplicateGroups.flatten()
                viewModel.moveToTrashBulk(all.map { it.id })
            }
            .setPositiveButton(R.string.audit_keep_one) { _, _ ->
                val toRemove = duplicateGroups.flatMap { group ->
                    group.sortedBy { it.createdAt }.drop(1)
                }
                viewModel.moveToTrashBulk(toRemove.map { it.id })
            }
            .show()
    }

    // ── Back press ───────────────────────────────────────────────────────────

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (allAuditAdapters.any { it.isSelectionMode }) {
                    clearAllSelections()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        if (allAuditAdapters.any { it.isSelectionMode }) {
            clearAllSelections()
            return true
        }
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // ── Audit computation ────────────────────────────────────────────────────

    private fun performAudit() {
        binding.progressIndicator.visibility = View.VISIBLE
        lifecycleScope.launch {
            val allEntries = withContext(Dispatchers.IO) { viewModel.passwordRepository.getAllEntries() }
            val passwordsOnly = allEntries.filter { it.type == EntryType.PASSWORD && !it.isDeleted }

            val weakEntries = mutableListOf<PasswordEntry>()
            val oldEntries = mutableListOf<PasswordEntry>()
            val passwordToEntries = mutableMapOf<String, MutableList<PasswordEntry>>()
            val sixMonthsAgo = System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000)

            withContext(Dispatchers.Default) {
                Encryption.ensureKeyExists()
                passwordsOnly.forEach { entry ->
                    try {
                        val plain = Encryption.decrypt(entry.passwordCipher, entry.passwordIv)

                        if (PasswordStrengthAnalyzer.analyze(plain).score < 45) {
                            weakEntries.add(entry)
                        }
                        if (plain.isNotEmpty()) {
                            passwordToEntries.getOrPut(plain) { mutableListOf() }.add(entry)
                        }
                        if (entry.updatedAt < sixMonthsAgo) {
                            oldEntries.add(entry)
                        }
                        yield()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            duplicateGroups = passwordToEntries.values.filter { it.size > 1 }
            val reusedEntries = duplicateGroups.flatten()

            weakHeader.updateCount(weakEntries.size)
            weakAdapter.submitList(weakEntries)

            reusedHeader.updateCount(reusedEntries.size)
            reusedAdapter.submitList(reusedEntries)

            oldHeader.updateCount(oldEntries.size)
            oldAdapter.submitList(oldEntries)

            binding.progressIndicator.visibility = View.GONE
        }
    }
}

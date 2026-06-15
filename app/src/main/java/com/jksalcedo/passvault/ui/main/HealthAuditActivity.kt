package com.jksalcedo.passvault.ui.main

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jksalcedo.passvault.adapter.PVAdapter
import com.jksalcedo.passvault.crypto.Encryption
import com.jksalcedo.passvault.data.PasswordEntry
import com.jksalcedo.passvault.databinding.ActivityHealthAuditBinding
import com.jksalcedo.passvault.ui.base.BaseActivity
import com.jksalcedo.passvault.ui.view.ViewEntryActivity
import com.jksalcedo.passvault.utils.PasswordStrengthAnalyzer
import com.jksalcedo.passvault.viewmodel.PasswordViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HealthAuditActivity : BaseActivity() {

    private lateinit var binding: ActivityHealthAuditBinding
    private lateinit var viewModel: PasswordViewModel

    private lateinit var weakAdapter: PVAdapter
    private lateinit var reusedAdapter: PVAdapter
    private lateinit var oldAdapter: PVAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHealthAuditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[PasswordViewModel::class.java]

        setupRecyclerViews()
        performAudit()
    }

    private fun setupRecyclerViews() {
        weakAdapter = PVAdapter(this).apply {
            onItemClick = { entry -> startActivity(ViewEntryActivity.createIntent(this@HealthAuditActivity, entry)) }
        }
        reusedAdapter = PVAdapter(this).apply {
            onItemClick = { entry -> startActivity(ViewEntryActivity.createIntent(this@HealthAuditActivity, entry)) }
        }
        oldAdapter = PVAdapter(this).apply {
            onItemClick = { entry -> startActivity(ViewEntryActivity.createIntent(this@HealthAuditActivity, entry)) }
        }

        binding.rvWeak.layoutManager = LinearLayoutManager(this)
        binding.rvWeak.adapter = weakAdapter

        binding.rvReused.layoutManager = LinearLayoutManager(this)
        binding.rvReused.adapter = reusedAdapter

        binding.rvOld.layoutManager = LinearLayoutManager(this)
        binding.rvOld.adapter = oldAdapter
    }

    private fun performAudit() {
        lifecycleScope.launch {
            val allEntries = withContext(Dispatchers.IO) { viewModel.passwordRepository.getAllEntries() }
            
            val weakEntries = mutableListOf<PasswordEntry>()
            val oldEntries = mutableListOf<PasswordEntry>()
            val passwordToEntries = mutableMapOf<String, MutableList<PasswordEntry>>()

            val sixMonthsAgo = System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000)

            withContext(Dispatchers.Default) {
                Encryption.ensureKeyExists()
                allEntries.forEach { entry ->
                    try {
                        val plain = Encryption.decrypt(entry.passwordCipher, entry.passwordIv)
                        
                        // Check Strength
                        val strength = PasswordStrengthAnalyzer.analyze(plain)
                        if (strength.score < 65) {
                            weakEntries.add(entry)
                        }

                        // Group for Reuse Check
                        if (plain.isNotEmpty()) {
                            passwordToEntries.getOrPut(plain) { mutableListOf() }.add(entry)
                        }

                        // Check Age
                        if (entry.updatedAt < sixMonthsAgo) {
                            oldEntries.add(entry)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val reusedEntries = passwordToEntries.values.filter { it.size > 1 }.flatten()

            weakAdapter.submitList(weakEntries)
            binding.tvWeakCount.text = "${weakEntries.size} entries"
            binding.rvWeak.visibility = if (weakEntries.isEmpty()) View.GONE else View.VISIBLE

            reusedAdapter.submitList(reusedEntries)
            binding.tvReusedCount.text = "${reusedEntries.size} entries"
            binding.rvReused.visibility = if (reusedEntries.isEmpty()) View.GONE else View.VISIBLE

            oldAdapter.submitList(oldEntries)
            binding.tvOldCount.text = "${oldEntries.size} entries"
            binding.rvOld.visibility = if (oldEntries.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

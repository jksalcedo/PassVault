package com.jksalcedo.passvault.ui.main

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.adapter.HeaderAdapter
import com.jksalcedo.passvault.adapter.PVAdapter
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
    private lateinit var weakAdapter: PVAdapter
    
    private lateinit var reusedHeader: HeaderAdapter
    private lateinit var reusedAdapter: PVAdapter
    
    private lateinit var oldHeader: HeaderAdapter
    private lateinit var oldAdapter: PVAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHealthAuditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[PasswordViewModel::class.java]

        setupRecyclerView()
        performAudit()
    }

    private fun setupRecyclerView() {
        val colorWeak = ContextCompat.getColor(this, R.color.strength_very_weak)
        val colorReused = ContextCompat.getColor(this, R.color.strength_weak)
        val colorOld = ContextCompat.getColor(this, R.color.strength_fair)

        weakHeader = HeaderAdapter("Weak Passwords", colorWeak)
        weakAdapter = PVAdapter(this).apply {
            onItemClick = { entry -> startActivity(ViewEntryActivity.createIntent(this@HealthAuditActivity, entry)) }
        }

        reusedHeader = HeaderAdapter("Reused Passwords", colorReused)
        reusedAdapter = PVAdapter(this).apply {
            onItemClick = { entry -> startActivity(ViewEntryActivity.createIntent(this@HealthAuditActivity, entry)) }
        }

        oldHeader = HeaderAdapter("Old Passwords (> 6 months)", colorOld)
        oldAdapter = PVAdapter(this).apply {
            onItemClick = { entry -> startActivity(ViewEntryActivity.createIntent(this@HealthAuditActivity, entry)) }
        }

        val concatAdapter = ConcatAdapter(
            weakHeader, weakAdapter,
            reusedHeader, reusedAdapter,
            oldHeader, oldAdapter
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = concatAdapter
    }

    private fun performAudit() {
        binding.progressIndicator.visibility = View.VISIBLE
        lifecycleScope.launch {
            val allEntries = withContext(Dispatchers.IO) { viewModel.passwordRepository.getAllEntries() }
            val passwordsOnly = allEntries.filter { 
                it.type == EntryType.PASSWORD && !it.isDeleted 
            }
            
            val weakEntries = mutableListOf<PasswordEntry>()
            val oldEntries = mutableListOf<PasswordEntry>()
            val passwordToEntries = mutableMapOf<String, MutableList<PasswordEntry>>()

            val sixMonthsAgo = System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000)

            withContext(Dispatchers.Default) {
                Encryption.ensureKeyExists()
                passwordsOnly.forEach { entry ->
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
                        
                        // Yield to keep the app responsive if there are many entries
                        yield()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val reusedEntries = passwordToEntries.values.filter { it.size > 1 }.flatten()

            // Update UI on main thread
            weakHeader.updateCount(weakEntries.size)
            weakAdapter.submitList(weakEntries)

            reusedHeader.updateCount(reusedEntries.size)
            reusedAdapter.submitList(reusedEntries)

            oldHeader.updateCount(oldEntries.size)
            oldAdapter.submitList(oldEntries)
            
            binding.progressIndicator.visibility = View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

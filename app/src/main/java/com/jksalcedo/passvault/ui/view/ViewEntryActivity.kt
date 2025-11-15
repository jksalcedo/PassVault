package com.jksalcedo.passvault.ui.view

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.crypto.Encryption
import com.jksalcedo.passvault.data.PasswordEntry
import com.jksalcedo.passvault.databinding.ActivityViewEntryBinding
import com.jksalcedo.passvault.ui.addedit.AddEditActivity
import com.jksalcedo.passvault.utils.Utility
import com.jksalcedo.passvault.utils.Utility.formatTime
import com.jksalcedo.passvault.viewmodel.PasswordViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ViewEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewEntryBinding
    private var currentEntry: PasswordEntry? = null

    private lateinit var viewModel: PasswordViewModel

    private var revealed: Boolean = false
    private var plainPassword: String = ""
    private var isExpanded: Boolean = false

    private val toTopAnim: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.to_top) }
    private val toBottomAnim: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.to_bottom
        )
    }
    private val clockwiseAnim: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.rotate_clockwise
        )
    }
    private val antiClockwiseAnim: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.rotate_anti_clockwise
        )
    }
    private val fadeIn: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.fade_in) }
    private val fadeOut: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.fade_out) }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[PasswordViewModel::class.java]

        currentEntry = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_ENTRY, PasswordEntry::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_ENTRY)
        }

        if (currentEntry == null) {
            Toast.makeText(this, "Missing entry data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentEntry?.let { entry ->
            plainPassword = try {
                Encryption.ensureKeyExists()
                Encryption.decrypt(entry.passwordCipher, entry.passwordIv)
            } catch (_: Exception) {
                ""
            }

            binding.tvTitle.text = entry.title
            binding.tvUsername.text = entry.username.orEmpty()
            binding.tvPassword.text = MASKED_PASSWORD
            binding.tvNotes.text = entry.notes.orEmpty()
            binding.tvMetadata.text =
                "Created: ${entry.createdAt.formatTime()} - Modified: ${entry.updatedAt.formatTime()}"

            binding.btnCopyUsername.setOnClickListener {
                if (entry.username?.isNotEmpty() == true) {
                    Utility.copyToClipboard(this, "username", entry.username)
                    Toast.makeText(this, "Username copied", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No username to copy", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnReveal.setOnClickListener {
            revealed = !revealed
            if (revealed) {
                binding.tvPassword.text = plainPassword
                binding.btnReveal.text = getString(R.string.hide)
            } else {
                binding.tvPassword.text = MASKED_PASSWORD
                binding.btnReveal.text = getString(R.string.reveal)
            }
        }

        binding.btnCopyPassword.setOnClickListener {
            if (plainPassword.isNotEmpty()) {
                Utility.copyToClipboard(this, "password", plainPassword)
                Toast.makeText(this, "Password copied", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No password to copy", Toast.LENGTH_SHORT).show()
            }
        }

        collapseFab()

        binding.fabActions.setOnClickListener {
            setFabVisibility(!isExpanded)
            isExpanded = !isExpanded
        }

        binding.fabEdit.setOnClickListener {
            currentEntry?.let {
                startActivity(AddEditActivity.createIntent(this, it))
                finish()
            }
        }

        binding.fabDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setPositiveButton("Cancel", null)
                .setNegativeButton("Delete") { _, _ ->
                    currentEntry?.let { viewModel.delete(it) }
                    onBackPressedDispatcher.onBackPressed()
                }
                .setTitle("Delete Confirmation")
                .setMessage("Proceed to delete this entry?")
                .show()
        }
    }

    private fun setFabVisibility(expanded: Boolean) {
        if (expanded) {
            binding.actions.visibility = View.VISIBLE
            binding.fabActions.startAnimation(clockwiseAnim)
            binding.actions.startAnimation(toTopAnim)
            binding.dim.startAnimation(fadeIn)
        } else {
            lifecycleScope.launch {
                delay(300L)
                binding.actions.visibility = View.GONE
            }
            binding.fabActions.startAnimation(antiClockwiseAnim)
            binding.actions.startAnimation(toBottomAnim)
            binding.dim.startAnimation(fadeOut)
        }
    }

    private fun collapseFab() {
        setFabVisibility(false)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_ENTRY = "extra_entry"
        private const val MASKED_PASSWORD = "••••••••"

        fun createIntent(context: Context, entry: PasswordEntry): Intent {
            return Intent(context, ViewEntryActivity::class.java).apply {
                putExtra(EXTRA_ENTRY, entry)
            }
        }
    }
}
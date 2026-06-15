package com.jksalcedo.passvault.ui.view

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.adapter.CustomFieldsAdapter
import com.jksalcedo.passvault.crypto.Encryption
import com.jksalcedo.passvault.data.CustomFieldsPayload
import com.jksalcedo.passvault.data.PasswordEntry
import com.jksalcedo.passvault.data.enums.EntryType
import com.jksalcedo.passvault.databinding.ActivityViewEntryBinding
import com.jksalcedo.passvault.ui.addedit.AddEditActivity
import com.jksalcedo.passvault.ui.base.BaseActivity
import com.jksalcedo.passvault.utils.PasswordStrengthAnalyzer
import com.jksalcedo.passvault.utils.Utility
import com.jksalcedo.passvault.utils.Utility.formatTime
import com.jksalcedo.passvault.viewmodel.CategoryViewModel
import com.jksalcedo.passvault.viewmodel.PasswordViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * An activity for viewing a password entry.
 */
class ViewEntryActivity : BaseActivity() {

    private lateinit var binding: ActivityViewEntryBinding
    private var currentEntry: PasswordEntry? = null

    private lateinit var viewModel: PasswordViewModel
    private lateinit var categoryViewModel: CategoryViewModel

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[PasswordViewModel::class.java]
        categoryViewModel = ViewModelProvider(this)[CategoryViewModel::class.java]

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

        supportActionBar?.title = currentEntry?.title.orEmpty()

        currentEntry?.let { entry ->
            val isNote = entry.type == EntryType.NOTE

            plainPassword = try {
                Encryption.ensureKeyExists()
                if (entry.passwordCipher.isNotEmpty()) {
                    Encryption.decrypt(entry.passwordCipher, entry.passwordIv)
                } else {
                    ""
                }
            } catch (_: Exception) {
                ""
            }

            // Username field
            if (entry.username.isNullOrEmpty()) {
                binding.cardUsername.visibility = View.GONE
            } else {
                binding.cardUsername.visibility = View.VISIBLE
                binding.tvUsername.text = entry.username
            }

            // Password field
            if (isNote || plainPassword.isEmpty()) {
                binding.cardPassword.visibility = View.GONE
            } else {
                binding.cardPassword.visibility = View.VISIBLE
                binding.tvPassword.text = MASKED_PASSWORD
            }

            // Email field
            if (entry.email.isNullOrEmpty()) {
                binding.cardEmail.visibility = View.GONE
            } else {
                binding.cardEmail.visibility = View.VISIBLE
                binding.tvEmail.text = entry.email
            }

            // URL field
            if (entry.url.isNullOrEmpty()) {
                binding.cardUrl.visibility = View.GONE
            } else {
                binding.cardUrl.visibility = View.VISIBLE
                binding.tvUrl.text = entry.url
            }

            // Notes field
            if (entry.notes.isNullOrEmpty()) {
                binding.cardNotes.visibility = View.GONE
            } else {
                binding.cardNotes.visibility = View.VISIBLE
                binding.tvNotes.text = entry.notes
            }

            binding.tvMetadata.text =
                buildString {
                    append("Created: ")
                    append(entry.createdAt.formatTime())
                    append("\nModified: ")
                    append(entry.updatedAt.formatTime())
                }

            // Set category chip with color (async lookup for custom categories)
            val category = entry.category ?: "General"
            binding.tvCategoryChip.text = category.uppercase()

            lifecycleScope.launch {
                val categoryEntity = categoryViewModel.getCategoryByName(category)
                val colorHex = categoryEntity?.colorHex
                val color =
                    Utility.getCategoryColor(this@ViewEntryActivity, entry.category, colorHex)
                binding.tvCategoryChip.setTextColor(color)
                binding.tvCategoryChip.background?.setTint(color.and(0x00FFFFFF).or(0x10000000))
            }

            // Copy username
            binding.btnCopyUsername.setOnClickListener {
                entry.username?.let {
                    Utility.copyToClipboard(this, "username", it)
                    Toast.makeText(this, "Username copied", Toast.LENGTH_SHORT).show()
                }
            }

            // Copy email
            binding.btnCopyEmail.setOnClickListener {
                entry.email?.let {
                    Utility.copyToClipboard(this, "email", it)
                    Toast.makeText(this, "Email copied", Toast.LENGTH_SHORT).show()
                }
            }

            binding.btnCopyNotes.setOnClickListener {
                entry.notes?.let { notes ->
                    Utility.copyToClipboard(this, "notes", notes)
                    Toast.makeText(this, "Notes copied", Toast.LENGTH_SHORT).show()
                }
            }

            // Open URL in browser
            binding.btnOpenUrl.setOnClickListener {
                entry.url?.let { url ->
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        if (!url.startsWith("http")) "https://$url".toUri() else url.toUri()
                    )
                    try {
                        startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Display password strength
            val strengthResult = PasswordStrengthAnalyzer.analyze(plainPassword)
            val strengthLabel = PasswordStrengthAnalyzer.getStrengthLabel(strengthResult.level)
            binding.chipPasswordStrength.text = strengthLabel

            val colorResId = when (strengthResult.level) {
                PasswordStrengthAnalyzer.StrengthLevel.VERY_WEAK -> R.color.strength_very_weak
                PasswordStrengthAnalyzer.StrengthLevel.WEAK -> R.color.strength_weak
                PasswordStrengthAnalyzer.StrengthLevel.FAIR -> R.color.strength_fair
                PasswordStrengthAnalyzer.StrengthLevel.GOOD -> R.color.strength_good
                PasswordStrengthAnalyzer.StrengthLevel.STRONG -> R.color.strength_strong
            }
            binding.chipPasswordStrength.setChipBackgroundColorResource(colorResId)
            binding.chipPasswordStrength.setTextColor(
                ContextCompat.getColor(this, android.R.color.white)
            )

            // Load custom fields
            if (entry.customFieldsCipher != null && entry.customFieldsIv != null) {
                try {
                    val json = Encryption.decrypt(entry.customFieldsCipher, entry.customFieldsIv)
                    val payload = Json.decodeFromString<CustomFieldsPayload>(json)

                    if (payload.fields.isNotEmpty()) {
                        val adapter = CustomFieldsAdapter(
                            isReadOnly = true,
                            onCopyClick = { field ->
                                Utility.copyToClipboard(this, field.name, field.value)
                                Toast.makeText(this, "${field.name} copied", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        )
                        binding.rvCustomFields.layoutManager = LinearLayoutManager(this)
                        binding.rvCustomFields.adapter = adapter
                        adapter.submitList(payload.fields)
                        binding.rvCustomFields.visibility = View.VISIBLE
                    } else {
                        binding.rvCustomFields.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    binding.rvCustomFields.visibility = View.GONE
                }
            } else {
                binding.rvCustomFields.visibility = View.GONE
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
            MaterialAlertDialogBuilder(this)
                .setPositiveButton("Cancel", null)
                .setNegativeButton("Delete") { _, _ ->
                    currentEntry?.let { viewModel.moveToTrash(it.id) }
                    onBackPressedDispatcher.onBackPressed()
                }
                .setTitle("Move to Trash")
                .setMessage("This entry will be moved to the Trash and permanently deleted after 30 days.")
                .show()
        }
    }

    /**
     * Sets the visibility of the FAB menu.
     * @param expanded True to expand the FAB menu, false to collapse it.
     */
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

    /**
     * Collapses the FAB menu.
     */
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

        /**
         * Creates an intent to start [ViewEntryActivity].
         * @param context The context.
         * @param entry The password entry to view.
         * @return An intent to start [ViewEntryActivity].
         */
        fun createIntent(context: Context, entry: PasswordEntry): Intent {
            return Intent(context, ViewEntryActivity::class.java).apply {
                putExtra(EXTRA_ENTRY, entry)
            }
        }
    }
}
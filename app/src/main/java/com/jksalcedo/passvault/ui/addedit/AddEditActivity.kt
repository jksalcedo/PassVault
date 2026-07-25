package com.jksalcedo.passvault.ui.addedit

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.adapter.CustomFieldsAdapter
import com.jksalcedo.passvault.crypto.Encryption
import com.jksalcedo.passvault.data.CustomField
import com.jksalcedo.passvault.data.CustomFieldsPayload
import com.jksalcedo.passvault.data.PasswordEntry
import com.jksalcedo.passvault.data.enums.EntryType
import com.jksalcedo.passvault.databinding.ActivityAddEditBinding
import com.jksalcedo.passvault.ui.base.BaseActivity
import com.jksalcedo.passvault.utils.PasswordStrengthAnalyzer
import com.jksalcedo.passvault.viewmodel.CategoryViewModel
import com.jksalcedo.passvault.viewmodel.PasswordViewModel
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * An activity for adding and editing password entries.
 */
class AddEditActivity : BaseActivity(), PasswordDialogListener {
    private lateinit var binding: ActivityAddEditBinding
    private lateinit var viewModel: PasswordViewModel
    private lateinit var categoryViewModel: CategoryViewModel

    private var currentEntry: PasswordEntry? = null
    private lateinit var customFieldsAdapter: CustomFieldsAdapter
    private val customFields = mutableListOf<CustomField>()
    private var currentType: EntryType = EntryType.PASSWORD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[PasswordViewModel::class.java]
        categoryViewModel = ViewModelProvider(this)[CategoryViewModel::class.java]

        setupCustomFields()
        setupTypeToggle()

        // Load categories for dropdown
        categoryViewModel.allCategories.observe(this) { categories ->
            val categoryNames = categories.mapNotNull { it.name.takeIf(String::isNotBlank) }
            val adapter =
                ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
            binding.etCategory.setAdapter(adapter)

            // Set default category for new entries
            if (currentEntry == null && binding.etCategory.text.isNullOrEmpty()) {
                binding.etCategory.setText(getString(R.string.general), false)
            }
        }

        val entryFromIntent: PasswordEntry? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.setExtrasClassLoader(PasswordEntry::class.java.classLoader)
                intent.getParcelableExtra(EXTRA_ENTRY, PasswordEntry::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_ENTRY)
            }

        if (entryFromIntent != null) {
            // If the full object is passed, use it directly
            currentEntry = entryFromIntent
            populateUi(entryFromIntent)
            binding.toolbar.title = getString(R.string.edit_password)
        } else if (intent.hasExtra(EXTRA_ID)) {
            // Fallback for when only the ID is provided.
            val id = intent.getLongExtra(EXTRA_ID, -1)
            viewModel.getEntryById(id).observe(this) { entry ->
                entry?.let {
                    currentEntry = it
                    populateUi(it)
                }
            }
            binding.toolbar.title = getString(R.string.edit_password)
        } else {
            // Check for Autofill data
            val autoTitle = intent.getStringExtra(EXTRA_AUTOFILL_TITLE)
            val autoUser = intent.getStringExtra(EXTRA_AUTOFILL_USERNAME)
            val autoPass = intent.getStringExtra(EXTRA_AUTOFILL_PASSWORD)
            val autoUrl = intent.getStringExtra(EXTRA_AUTOFILL_URL)
            val autoEmail = intent.getStringExtra(EXTRA_AUTOFILL_EMAIL)

            if (autoTitle != null || autoUser != null || autoPass != null) {
                binding.etTitle.setText(autoTitle)
                binding.etUsername.setText(autoUser)
                binding.etPassword.setText(autoPass)
                binding.etUrl.setText(autoUrl)
                binding.etEmail.setText(autoEmail)

                // If we have a password, trigger strength check immediately
                if (!autoPass.isNullOrEmpty()) {
                    updatePasswordStrength(autoPass)
                }
            }
        }

        binding.btnSave.setOnClickListener {
            saveEntry()
        }

        // Password generator button
        binding.btnGeneratePassword.setOnClickListener {
            PasswordGenDialog().apply {
                isCancelable = false
                show(supportFragmentManager, "PasswordGenDialog")
            }
        }

        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updatePasswordStrength(s?.toString() ?: "")
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

    }

    private fun setupTypeToggle() {
        binding.toggleGroupType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentType = when (checkedId) {
                    R.id.btnTypeNote -> EntryType.NOTE
                    else -> EntryType.PASSWORD
                }
                updateUiForType(currentType)
            }
        }
        // Initial state
        updateUiForType(currentType)
    }

    private fun updateUiForType(type: EntryType) {
        val isPassword = type == EntryType.PASSWORD
        binding.tilUsername.visibility = if (isPassword) View.VISIBLE else View.GONE
        binding.tilPassword.visibility = if (isPassword) View.VISIBLE else View.GONE
        binding.layoutPasswordStrength.visibility =
            if (isPassword && binding.etPassword.text?.isNotEmpty() == true) View.VISIBLE else View.GONE
        binding.btnGeneratePassword.visibility = if (isPassword) View.VISIBLE else View.GONE
        binding.tilEmail.visibility = if (isPassword) View.VISIBLE else View.GONE
        binding.tilUrl.visibility = if (isPassword) View.VISIBLE else View.GONE

        binding.toolbar.title = if (currentEntry == null) {
            if (isPassword) getString(R.string.add_password) else getString(R.string.add_note)
        } else {
            if (isPassword) getString(R.string.edit_password) else getString(R.string.edit_note)
        }
    }

    private fun setupCustomFields() {
        customFieldsAdapter = CustomFieldsAdapter(
            onEditClick = { field -> showAddEditFieldDialog(field) },
            onDeleteClick = { field ->
                customFields.remove(field)
                customFieldsAdapter.submitList(customFields.toList())
            },
            onCopyClick = { field ->
                com.jksalcedo.passvault.utils.Utility.copyToClipboard(this, field.name, field.value)
                Toast.makeText(this, getString(R.string.copied_to_clipboard, field.name), Toast.LENGTH_SHORT).show()
            },
            onStartDrag = { viewHolder ->
                itemTouchHelper.startDrag(viewHolder)
            }
        )

        binding.rvCustomFields.apply {
            layoutManager = LinearLayoutManager(this@AddEditActivity)
            adapter = customFieldsAdapter
        }

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                java.util.Collections.swap(customFields, fromPos, toPos)
                customFieldsAdapter.notifyItemMoved(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }
        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvCustomFields)

        binding.btnAddCustomField.setOnClickListener {
            showAddEditFieldDialog(null)
        }
    }

    private lateinit var itemTouchHelper: ItemTouchHelper

    private fun showAddEditFieldDialog(field: CustomField?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_custom_field, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.et_field_name)
        val etValue = dialogView.findViewById<TextInputEditText>(R.id.et_field_value)
        val switchSecret = dialogView.findViewById<SwitchMaterial>(R.id.switch_is_secret)

        field?.let {
            etName.setText(it.name)
            etValue.setText(it.value)
            switchSecret.isChecked = it.isSecret
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(if (field == null) getString(R.string.add_field) else getString(R.string.edit_field))
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = etName.text.toString().trim()
                val value = etValue.text.toString().trim()
                val isSecret = switchSecret.isChecked

                if (name.isNotEmpty()) {
                    if (field == null) {
                        val newField = CustomField(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            value = value,
                            isSecret = isSecret
                        )
                        customFields.add(newField)
                    } else {
                        val index = customFields.indexOfFirst { it.id == field.id }
                        if (index != -1) {
                            customFields[index] = field.copy(
                                name = name,
                                value = value,
                                isSecret = isSecret
                            )
                        }
                    }
                    customFieldsAdapter.submitList(customFields.toList())
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Populates the UI with the given password entry.
     * @param entry The password entry to populate the UI with.
     */
    private fun populateUi(entry: PasswordEntry) {
        currentType = entry.type
        binding.toggleGroupType.check(if (currentType == EntryType.NOTE) R.id.btnTypeNote else R.id.btnTypePassword)
        updateUiForType(currentType)

        binding.etTitle.setText(entry.title)
        binding.etUsername.setText(entry.username)
        binding.etNotes.setText(entry.notes)

        // Decrypt and set password
        try {
            Encryption.ensureKeyExists()
            if (entry.passwordCipher.isNotEmpty()) {
                val decryptedPassword = Encryption.decrypt(entry.passwordCipher, entry.passwordIv)
                binding.etPassword.setText(decryptedPassword)
            }
        } catch (_: Exception) {
            binding.etPassword.setText("")
            Toast.makeText(this, R.string.failed_to_decrypt, Toast.LENGTH_SHORT).show()
        }

        binding.etCategory.setText(entry.category ?: getString(R.string.general), false)
        binding.etEmail.setText(entry.email)
        binding.etUrl.setText(entry.url)

        // Load custom fields
        if (entry.customFieldsCipher != null && entry.customFieldsIv != null) {
            try {
                val json = Encryption.decrypt(entry.customFieldsCipher, entry.customFieldsIv)
                val payload = Json.decodeFromString<CustomFieldsPayload>(json)
                customFields.clear()
                customFields.addAll(payload.fields)
                customFieldsAdapter.submitList(customFields.toList())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Saves the current password entry.
     */
    private fun saveEntry() {
        // Clear previous errors
        binding.tilTitle.error = null
        binding.tilUsername.error = null
        binding.tilPassword.error = null

        val title = binding.etTitle.text.toString()
        val username =
            if (currentType == EntryType.PASSWORD) binding.etUsername.text.toString() else null
        val rawPassword =
            if (currentType == EntryType.PASSWORD) binding.etPassword.text.toString() else ""
        val notes = binding.etNotes.text.toString()
        val category = binding.etCategory.text.toString()
        val email = if (currentType == EntryType.PASSWORD) binding.etEmail.text.toString() else null
        val url = if (currentType == EntryType.PASSWORD) binding.etUrl.text.toString() else null

        if (title.isEmpty()) {
            binding.tilTitle.error = getString(R.string.title_empty_error)
            return
        }

        if (currentType == EntryType.PASSWORD && email?.isNotEmpty() == true && !android.util.Patterns.EMAIL_ADDRESS.matcher(
                email
            ).matches()
        ) {
            binding.tilEmail.error = getString(R.string.invalid_email_error)
            return
        }

        if (currentType == EntryType.PASSWORD && url?.isNotEmpty() == true && !android.util.Patterns.WEB_URL.matcher(
                url
            ).matches()
        ) {
            binding.tilUrl.error = getString(R.string.invalid_url_error)
            return
        }

        try {
            Encryption.ensureKeyExists()
            val (cipherText, iv) = if (rawPassword.isNotEmpty()) {
                Encryption.encrypt(rawPassword)
            } else {
                Pair("", "")
            }

            val entry = currentEntry?.copy(
                title = title,
                username = username,
                passwordCipher = cipherText,
                passwordIv = iv,
                notes = notes,
                email = email,
                url = url,
                category = category.ifEmpty { getString(R.string.general) },
                type = currentType,
                updatedAt = System.currentTimeMillis()
            ) ?: PasswordEntry(
                title = title,
                username = username,
                passwordCipher = cipherText,
                passwordIv = iv,
                notes = notes,
                email = email,
                url = url,
                category = category.ifEmpty { getString(R.string.general) },
                type = currentType,
                updatedAt = System.currentTimeMillis()
            )

            // Encrypt custom fields
            var customFieldsCipher: String? = null
            var customFieldsIv: String? = null
            if (customFields.isNotEmpty()) {
                val payload = CustomFieldsPayload(fields = customFields)
                val json = Json.encodeToString(payload)
                val (cfCipher, cfIv) = Encryption.encrypt(json)
                customFieldsCipher = cfCipher
                customFieldsIv = cfIv
            }

            val finalEntry = entry.copy(
                customFieldsCipher = customFieldsCipher,
                customFieldsIv = customFieldsIv
            )

            if (currentEntry == null) {
                viewModel.insert(finalEntry)
            } else {
                viewModel.update(finalEntry)
            }
            finish()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.encryption_failed, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Updates the password strength indicator based on the current password.
     */
    private fun updatePasswordStrength(password: String) {
        if (password.isEmpty()) {
            binding.layoutPasswordStrength.visibility = View.GONE
            return
        }

        binding.layoutPasswordStrength.visibility = View.VISIBLE
        val result = PasswordStrengthAnalyzer.analyze(password)

        // Update progress bar
        binding.progressPasswordStrength.setProgress(result.score, true)

        // Update color based on strength level
        val colorResId = when (result.level) {
            PasswordStrengthAnalyzer.StrengthLevel.VERY_WEAK -> R.color.strength_very_weak
            PasswordStrengthAnalyzer.StrengthLevel.WEAK -> R.color.strength_weak
            PasswordStrengthAnalyzer.StrengthLevel.FAIR -> R.color.strength_fair
            PasswordStrengthAnalyzer.StrengthLevel.GOOD -> R.color.strength_good
            PasswordStrengthAnalyzer.StrengthLevel.STRONG -> R.color.strength_strong
        }
        val color = ContextCompat.getColor(this, colorResId)
        binding.progressPasswordStrength.setIndicatorColor(color)

        // Update label
        val strengthLabel = PasswordStrengthAnalyzer.getStrengthLabel(result.level)
        binding.tvPasswordStrength.text = getString(R.string.password_strength_label, strengthLabel)
        binding.tvPasswordStrength.setTextColor(color)

        // Update feedback
        binding.tvStrengthFeedback.text = result.feedback.firstOrNull() ?: ""
        binding.tvStrengthFeedback.setTextColor(color)
    }

    override fun onPasswordGenerated(password: String) {
        binding.etPassword.text =
            Editable.Factory.getInstance().newEditable((password.ifEmpty { "" }))
        updatePasswordStrength(password)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_ENTRY = "extra_entry"
        const val EXTRA_AUTOFILL_TITLE = "extra_autofill_title"
        const val EXTRA_AUTOFILL_USERNAME = "extra_autofill_username"
        const val EXTRA_AUTOFILL_PASSWORD = "extra_autofill_password"
        const val EXTRA_AUTOFILL_URL = "extra_autofill_url"
        const val EXTRA_AUTOFILL_EMAIL = "extra_autofill_email"

        /**
         * Creates an intent to start [AddEditActivity].
         * @param context The context.
         * @param entry The password entry to edit.
         * @return An intent to start [AddEditActivity].
         */
        fun createIntent(context: Context, entry: PasswordEntry? = null): Intent {
            return Intent(context, AddEditActivity::class.java).apply {
                entry?.let {
                    putExtra(EXTRA_ID, it.id)
                    putExtra(EXTRA_ENTRY, it)
                }
            }
        }
    }
}

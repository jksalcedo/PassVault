package com.jksalcedo.passvault.ui.main

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.chip.Chip
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.adapter.PVAdapter
import com.jksalcedo.passvault.data.enums.SortOption
import com.jksalcedo.passvault.databinding.ActivityMainBinding
import com.jksalcedo.passvault.repositories.PreferenceRepository
import com.jksalcedo.passvault.ui.addedit.AddEditActivity
import com.jksalcedo.passvault.ui.addedit.PasswordDialogListener
import com.jksalcedo.passvault.ui.addedit.PasswordGenDialog
import com.jksalcedo.passvault.ui.base.BaseActivity
import com.jksalcedo.passvault.ui.category.ManageCategoriesDialog
import com.jksalcedo.passvault.ui.settings.SettingsActivity
import com.jksalcedo.passvault.ui.view.ViewEntryActivity
import com.jksalcedo.passvault.utils.SessionManager
import com.jksalcedo.passvault.viewmodel.CategoryViewModel
import com.jksalcedo.passvault.viewmodel.PasswordViewModel

/**
 * The main activity of the app.
 */
class MainActivity : BaseActivity(), PasswordDialogListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: PVAdapter
    private lateinit var viewModel: PasswordViewModel
    private lateinit var categoryViewModel: CategoryViewModel


    @SuppressLint("InternalInsetResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.getStringExtra("shortcut_action") == "generate_password") {
            // Wait for UI to load
            binding.root.post {
                showPasswordGeneratorDialog()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomAppBar) { view, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom

            val newBottomMargin = if (imeVisible) {
                // Keyboard is OPEN: Move up by the difference
                // We prevent negative numbers just in case
                (imeHeight - navBarHeight).coerceAtLeast(0)
            } else {
                // Keyboard is CLOSED: Reset to 0 (let CoordinatorLayout handle the nav bar)
                0
            }

            view.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                bottomMargin = newBottomMargin
            }

            view.updatePadding(bottom = if (!imeVisible) navBarHeight else 0)

            insets
        }

        val prefsRepository = PreferenceRepository(this)
        val useBottomAppBar = prefsRepository.getUseBottomAppBar()

        if (useBottomAppBar) {
            binding.bottomAppBar.apply {
                fabCradleMargin = (8 * resources.displayMetrics.density)
                fabAnchorMode = BottomAppBar.FAB_ANCHOR_MODE_CRADLE
                fabCradleRoundedCornerRadius = (10 * resources.displayMetrics.density)
                cradleVerticalOffset = 0F
            }

            binding.toolbar.visibility = View.GONE
            binding.bottomAppBar.visibility = View.VISIBLE
            setSupportActionBar(binding.bottomAppBar)

            // Ensure FAB is anchored to BottomAppBar
            val params =
                binding.fabAdd.layoutParams as CoordinatorLayout.LayoutParams
            params.anchorId = binding.bottomAppBar.id
            params.setMargins(0, 0, 0, 0)
            binding.fabAdd.layoutParams = params

            // Add top padding to chips to avoid status bar overlap
            ViewCompat.setOnApplyWindowInsetsListener(binding.chipScrollView) { view, insets ->
                val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top

                // Convert 16dp to pixels
                val extraPadding = (16 * resources.displayMetrics.density).toInt()

                view.setPadding(
                    view.paddingLeft,
                    statusBarHeight + extraPadding,
                    view.paddingRight,
                    view.paddingBottom
                )

                insets
            }
        } else {
            binding.toolbar.visibility = View.VISIBLE
            binding.bottomAppBar.visibility = View.GONE
            setSupportActionBar(binding.toolbar)

            // Remove FAB anchor
            val params =
                binding.fabAdd.layoutParams as CoordinatorLayout.LayoutParams
            params.anchorId = View.NO_ID
            params.gravity = Gravity.BOTTOM or Gravity.END
            val margin = (16 * resources.displayMetrics.density).toInt()
            params.setMargins(0, 0, margin, margin)
            binding.fabAdd.layoutParams = params
        }

        adapter = PVAdapter(this)
        binding.contentMain.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.contentMain.recyclerView.adapter = adapter

        // Add padding to content to prevent BottomAppBar from covering elements
        ViewCompat.setOnApplyWindowInsetsListener(binding.contentMain.recyclerView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navBarHeight = systemBars.bottom

            val bottomAppBarHeight = if (prefsRepository.getUseBottomAppBar()) {
                val tv = TypedValue()
                if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                    TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
                } else {
                    0
                }
            } else {
                0
            }

            val extraPadding = (16 * resources.displayMetrics.density).toInt()
            view.updatePadding(bottom = navBarHeight + bottomAppBarHeight + extraPadding)
            insets
        }

        // View the entry
        adapter.onItemClick = { entry ->
            startActivity(ViewEntryActivity.createIntent(this, entry))
        }

        viewModel = ViewModelProvider(this)[PasswordViewModel::class.java]
        categoryViewModel = ViewModelProvider(this)[CategoryViewModel::class.java]
        var pendingEmptyStateRunnable: Runnable? = null
        viewModel.filteredEntries.observe(this) { list ->
            adapter.submitList(list)

            // Cancel any pending empty state show
            pendingEmptyStateRunnable?.let { binding.root.removeCallbacks(it) }

//            val showEmpty = list.isEmpty() && !viewModel.isSearching()
//            val showNotFound = list.isEmpty() && viewModel.isSearching()

            if (list.isNotEmpty()) {
                binding.contentMain.tvMessage.visibility = View.GONE
                binding.contentMain.ivIllustration.visibility = View.GONE
            } else {
                // Delay showing empty state to avoid flash on initial load
                pendingEmptyStateRunnable = Runnable {
                    if (viewModel.filteredEntries.value?.isEmpty() == true) {
                        binding.contentMain.tvMessage.visibility = View.VISIBLE
                        binding.contentMain.ivIllustration.visibility = View.VISIBLE
                        if (viewModel.isSearching()) {
                            binding.contentMain.ivIllustration.setImageResource(R.drawable.il_not_found)
                            binding.contentMain.tvMessage.text =
                                getString(R.string.no_results_found)
                        } else {
                            binding.contentMain.ivIllustration.setImageResource(R.drawable.il_no_entries)
                            binding.contentMain.tvMessage.text =
                                getString(R.string.click_the_button_to_add_your_first_password)
                        }
                    }
                }
                binding.root.postDelayed(pendingEmptyStateRunnable, 150)
            }
        }

        // Add entry
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddEditActivity::class.java))
        }
        // Delay showing FAB until icon is loaded to prevent flash on older Android
        binding.fabAdd.post {
            binding.fabAdd.show()
            if (binding.bottomAppBar.isVisible) {
                // Re-set the anchor mode to force the cradle to be drawn
                binding.bottomAppBar.fabAnchorMode = BottomAppBar.FAB_ANCHOR_MODE_CRADLE
            }
        }

        // Dynamically load category chips from database
        categoryViewModel.allCategories.observe(this) { categories ->
            binding.chipGroupCategories.removeAllViews()

            // Build category color map and pass to adapter
            val categoryColors = categories.associate { it.name to it.colorHex }
            adapter.setCategoryColors(categoryColors)

            // Add "All" chip
            val chipAll =
                layoutInflater.inflate(R.layout.chip, binding.chipGroupCategories, false) as Chip
            chipAll.apply {
                id = View.generateViewId()
                text = buildString {
                    append("All (")
                    append(viewModel.allEntries.value?.size ?: 0)
                    append(")")
                }
                isCheckable = true
                isChecked = true
                setChipBackgroundColorResource(R.color.chip_background_selector)
            }
            binding.chipGroupCategories.addView(chipAll)

            // Add category chips
            categories.forEach { category ->
                val chip = layoutInflater.inflate(
                    R.layout.chip,
                    binding.chipGroupCategories,
                    false
                ) as Chip
                chip.apply {
                    id = View.generateViewId()
                    val count =
                        viewModel.allEntries.value?.count { it.category == category.name } ?: 0
                    text = buildString {
                        append(category.name)
                        append(" (")
                        append(count)
                        append(")")
                    }
                    isCheckable = true
                    setChipBackgroundColorResource(R.color.chip_background_selector)
                    tag = category.name // Store category name in tag
                }
                binding.chipGroupCategories.addView(chip)
            }

            // Set up click listener for all chips
            binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
                if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

                val selectedChip = findViewById<Chip>(checkedIds[0])
                val categoryName = selectedChip?.tag as? String

                viewModel.filterByCategory(categoryName)
                updateCategoryCounts()
            }
        }

        // Update counts when entries change
        viewModel.allEntries.observe(this) {
            updateCategoryCounts()
        }

        checkFirstLaunch(prefsRepository)
    }

    private fun checkFirstLaunch(prefsRepository: PreferenceRepository) {
        if (prefsRepository.isFirstLaunch()) {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Important Note")
                .setMessage(
                    "Welcome to PassVault!\n\n" +
                            "To ensure your data is always safe, please enable Auto Backups in Settings > Vault & Data and keep it enabled.\n\n" +
                            "It is highly recommended to use a strong password for your backups to ensure their security.\n\n" +
                            "Regular backups are crucial for data recovery in case of device loss or app reinstallation."
                )
                .setPositiveButton("Got it") { _, _ ->
                    prefsRepository.setFirstLaunch(false)
                }
                .setCancelable(false)
                .show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the intent
        handleShortcutIntent(intent)
    }

    private fun handleShortcutIntent(intent: Intent?) {
        // Only run this if we are actually unlocked
        if (SessionManager.isUnlocked && intent?.getStringExtra("shortcut_action") == "generate_password") {
            binding.root.post {
                showPasswordGeneratorDialog()
                intent.removeExtra("shortcut_action")
            }
        }
    }

    private fun showPasswordGeneratorDialog() {
        val dialog = PasswordGenDialog()
        dialog.isCancelable = false
        dialog.show(supportFragmentManager, null)
    }

    private fun updateCategoryCounts() {
        val entries = viewModel.allEntries.value ?: return

        for (i in 0 until binding.chipGroupCategories.childCount) {
            val chip = binding.chipGroupCategories.getChildAt(i) as? Chip ?: continue
            val categoryName = chip.tag as? String

            if (categoryName == null) {
                // "All" chip
                chip.text = buildString {
                    append("All (")
                    append(entries.size)
                    append(")")
                }
            } else {
                val count = entries.count { it.category == categoryName }
                chip.text = buildString {
                    append(categoryName)
                    append(" (")
                    append(count)
                    append(")")
                }
            }
        }
    }

    private fun performSearch(query: String?) {
        viewModel.setSearchQuery(query ?: "")
    }

    @SuppressLint("DiscouragedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        val searchPlate = searchView.findViewById<View>(androidx.appcompat.R.id.search_plate)
        searchPlate?.setBackgroundColor(Color.TRANSPARENT)

        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                binding.fabAdd.hide()
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                binding.fabAdd.show()
                return true
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                performSearch(newText)
                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }

            R.id.action_generator -> {
                val dialog = PasswordGenDialog()
                dialog.isCancelable = false
                dialog.show(supportFragmentManager, null)
                true
            }

            R.id.action_manage_categories -> {
                val dialog = ManageCategoriesDialog()
                dialog.show(supportFragmentManager, "ManageCategoriesDialog")
                true
            }

            R.id.sort_name_asc -> {
                viewModel.setSortOption(SortOption.NAME_ASC)
                true
            }

            R.id.sort_name_desc -> {
                viewModel.setSortOption(SortOption.NAME_DESC)
                true
            }

            R.id.sort_date_created -> {
                viewModel.setSortOption(SortOption.DATE_CREATED_DESC)
                true
            }

            R.id.sort_date_modified -> {
                viewModel.setSortOption(SortOption.DATE_MODIFIED_DESC)
                true
            }

            R.id.sort_category -> {
                viewModel.setSortOption(SortOption.CATEGORY_ASC)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPasswordGenerated(password: String) {
        // No need to handle
    }

}
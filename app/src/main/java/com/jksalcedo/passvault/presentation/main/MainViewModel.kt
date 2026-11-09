package com.jksalcedo.passvault.presentation.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jksalcedo.passvault.data.enums.SortOption
import com.jksalcedo.passvault.data.local.entity.Category
import com.jksalcedo.passvault.data.local.entity.PasswordEntry
import com.jksalcedo.passvault.data.repository.CategoryRepository
import com.jksalcedo.passvault.data.repository.PasswordRepository
import com.jksalcedo.passvault.data.repository.PreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.jksalcedo.passvault.PassVaultApplication as PassVaultApp

/**
 * ViewModel for the main vault screen.
 *
 * Owns all list filtering/sorting/search state so the Activity is limited to rendering.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val passwordRepository: PasswordRepository =
        (application as PassVaultApp).container.passwordRepository
    private val categoryRepository: CategoryRepository =
        (application as PassVaultApp).container.categoryRepository
    private val preferenceRepository: PreferenceRepository =
        (application as PassVaultApp).container.preferenceRepository

    val allEntries: LiveData<List<PasswordEntry>> = passwordRepository.getAll()
    val categories: LiveData<List<Category>> = categoryRepository.allCategories

    private val _currentCategory = MutableLiveData<String?>(null)
    val currentCategory: String? get() = _currentCategory.value

    private val _searchQuery = MutableLiveData("")
    private val _currentSortOption = MutableLiveData(
        SortOption.fromString(preferenceRepository.getSortOption())
    )

    private val _filteredEntries = MediatorLiveData<List<PasswordEntry>>()
    val filteredEntries: LiveData<List<PasswordEntry>> = _filteredEntries

    /** Category chips with the number of entries in each. `name == null` is the "All" chip. */
    private val _categoryFilters = MediatorLiveData<List<CategoryFilter>>()
    val categoryFilters: LiveData<List<CategoryFilter>> = _categoryFilters

    init {
        _filteredEntries.addSource(allEntries) { combineFilters() }
        _filteredEntries.addSource(_currentCategory) { combineFilters() }
        _filteredEntries.addSource(_searchQuery) { combineFilters() }
        _filteredEntries.addSource(_currentSortOption) { combineFilters() }

        _categoryFilters.addSource(allEntries) { rebuildCategoryFilters() }
        _categoryFilters.addSource(categories) { rebuildCategoryFilters() }
    }

    private fun rebuildCategoryFilters() {
        val entries = allEntries.value ?: emptyList()
        val categories = categories.value ?: emptyList()
        _categoryFilters.value = buildList {
            add(CategoryFilter(name = null, count = entries.size))
            categories.forEach { category ->
                add(
                    CategoryFilter(
                        name = category.name,
                        count = entries.count { it.category == category.name }
                    )
                )
            }
        }
    }

    private fun combineFilters() {
        val entries = allEntries.value ?: emptyList()
        val category = _currentCategory.value
        val query = _searchQuery.value ?: ""
        val sortOption = _currentSortOption.value ?: SortOption.NAME_ASC

        var result = entries

        if (!category.isNullOrEmpty()) {
            result = result.filter { it.category == category }
        }

        if (query.isNotEmpty()) {
            val q = query.lowercase()
            result = result.filter { entry ->
                entry.title.lowercase().contains(q) ||
                        (entry.username?.lowercase()?.contains(q) == true) ||
                        (entry.email?.lowercase()?.contains(q) == true) ||
                        (entry.url?.lowercase()?.contains(q) == true) ||
                        (entry.notes?.lowercase()?.contains(q) == true) ||
                        (entry.category?.lowercase()?.contains(q) == true)
            }
        }

        _filteredEntries.value = applySorting(result, sortOption)
    }

    private fun applySorting(list: List<PasswordEntry>, option: SortOption): List<PasswordEntry> {
        return when (option) {
            SortOption.NAME_ASC -> list.sortedBy { it.title.lowercase() }
            SortOption.NAME_DESC -> list.sortedByDescending { it.title.lowercase() }
            SortOption.DATE_CREATED_DESC -> list.sortedByDescending { it.createdAt }
            SortOption.DATE_CREATED_ASC -> list.sortedBy { it.createdAt }
            SortOption.DATE_MODIFIED_DESC -> list.sortedByDescending { it.updatedAt }
            SortOption.DATE_MODIFIED_ASC -> list.sortedBy { it.updatedAt }
            SortOption.CATEGORY_ASC -> list.sortedBy { it.category ?: "" }
        }
    }

    fun setSortOption(option: SortOption) {
        _currentSortOption.value = option
        preferenceRepository.setSortOption(option.name)
    }

    fun filterByCategory(category: String?) {
        _currentCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun isSearching(): Boolean = !_searchQuery.value.isNullOrEmpty()

    fun moveToTrashBulk(ids: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            ids.forEach { passwordRepository.moveToTrash(it) }
        }
    }

    suspend fun getEntriesForPrint(): List<PasswordEntry> = passwordRepository.getAllEntries()

    fun backupTimestampFormat(): String = preferenceRepository.getBackupTimestampFormat()

    fun useBottomAppBar(): Boolean = preferenceRepository.getUseBottomAppBar()

    fun isFirstLaunch(): Boolean = preferenceRepository.isFirstLaunch()

    fun markFirstLaunchHandled() = preferenceRepository.setFirstLaunch(false)
}

/**
 * A single category filter entry rendered as a chip.
 */
data class CategoryFilter(
    val name: String?,
    val count: Int
)

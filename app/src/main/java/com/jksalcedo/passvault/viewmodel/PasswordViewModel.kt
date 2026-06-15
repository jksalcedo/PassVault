package com.jksalcedo.passvault.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.jksalcedo.passvault.data.PasswordEntry
import com.jksalcedo.passvault.data.enums.SortOption
import com.jksalcedo.passvault.repositories.PasswordRepository
import com.jksalcedo.passvault.repositories.PreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PasswordViewModel(app: Application) : AndroidViewModel(app) {
    val passwordRepository = PasswordRepository(app.applicationContext)
    val preferenceRepository = PreferenceRepository(app.applicationContext)
    val allEntries: LiveData<List<PasswordEntry>> = passwordRepository.getAll()

    private val _currentCategory = MutableLiveData<String?>(null)
    private val _searchQuery = MutableLiveData<String>("")
    private val _currentSortOption = MutableLiveData<SortOption>()

    private val _filteredEntries = androidx.lifecycle.MediatorLiveData<List<PasswordEntry>>()
    val filteredEntries: LiveData<List<PasswordEntry>> = _filteredEntries

    init {
        val savedSort = preferenceRepository.getSortOption()
        _currentSortOption.value = SortOption.fromString(savedSort)

        _filteredEntries.addSource(allEntries) { combineFilters() }
        _filteredEntries.addSource(_currentCategory) { combineFilters() }
        _filteredEntries.addSource(_searchQuery) { combineFilters() }
        _filteredEntries.addSource(_currentSortOption) { combineFilters() }
    }

    private fun combineFilters() {
        val entries = allEntries.value ?: emptyList()
        val category = _currentCategory.value
        val query = _searchQuery.value ?: ""
        val sortOption = _currentSortOption.value ?: SortOption.NAME_ASC

        var result = entries

        // 1. Filter by Category
        if (!category.isNullOrEmpty()) {
            result = result.filter { it.category == category }
        }

        // 2. Filter by Search Query
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

        // 3. Apply Sorting
        result = applySorting(result, sortOption)

        _filteredEntries.value = result
    }

    fun setSortOption(option: SortOption) {
        _currentSortOption.value = option
        preferenceRepository.setSortOption(option.name)
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

    fun filterByCategory(category: String?) {
        _currentCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun isSearching(): Boolean {
        return !_searchQuery.value.isNullOrEmpty()
    }

    fun getEntryById(id: Long): LiveData<PasswordEntry> {
        return passwordRepository.getEntryById(id)
    }

    fun insert(entry: PasswordEntry) {
        viewModelScope.launch(Dispatchers.IO) { passwordRepository.insert(entry) }
    }

    fun update(entry: PasswordEntry) {
        viewModelScope.launch(Dispatchers.IO) { passwordRepository.update(entry) }
    }

    fun delete(entry: PasswordEntry) {
        viewModelScope.launch(Dispatchers.IO) { passwordRepository.delete(entry) }
    }

    fun moveToTrash(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { passwordRepository.moveToTrash(id) }
    }

    fun restoreFromTrash(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { passwordRepository.restoreFromTrash(id) }
    }

    fun getDeletedEntries(): LiveData<List<PasswordEntry>> {
        return passwordRepository.getDeletedEntries()
    }

    fun purgeOldDeletedEntries() {
        viewModelScope.launch(Dispatchers.IO) { passwordRepository.purgeOldDeletedEntries() }
    }
}

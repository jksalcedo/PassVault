package com.jksalcedo.passvault.presentation.category

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.jksalcedo.passvault.data.local.entity.Category
import com.jksalcedo.passvault.data.repository.CategoryRepository
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CategoryRepository =
        (application as com.jksalcedo.passvault.PassVaultApplication).container.categoryRepository
    val allCategories: LiveData<List<Category>> = repository.allCategories

    suspend fun getAllCategoriesSync(): List<Category> {
        return repository.getAllCategoriesSync()
    }

    suspend fun getCategoryByName(name: String): Category? {
        return repository.getCategoryByName(name)
    }

    fun insertCategory(category: Category) = viewModelScope.launch {
        repository.insert(category)
    }

    fun updateCategory(category: Category) = viewModelScope.launch {
        repository.update(category)
    }

    fun deleteCategory(category: Category) = viewModelScope.launch {
        repository.delete(category)
    }

    fun deleteCustomCategory(name: String) = viewModelScope.launch {
        repository.deleteCustomCategory(name)
    }
}

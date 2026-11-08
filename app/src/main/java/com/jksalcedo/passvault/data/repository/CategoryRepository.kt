package com.jksalcedo.passvault.data.repository

import androidx.lifecycle.LiveData
import com.jksalcedo.passvault.data.local.dao.CategoryDao
import com.jksalcedo.passvault.data.local.entity.Category

class CategoryRepository(private val categoryDao: CategoryDao) {

    val allCategories: LiveData<List<Category>> = categoryDao.getAllCategories()

    suspend fun getAllCategoriesSync(): List<Category> {
        return categoryDao.getAllCategoriesSync()
    }

    suspend fun getCategoryByName(name: String): Category? {
        return categoryDao.getCategoryByName(name)
    }

    suspend fun insert(category: Category): Long {
        return categoryDao.insert(category)
    }

    suspend fun insertAll(categories: List<Category>) {
        categoryDao.insertAll(categories)
    }

    suspend fun update(category: Category) {
        categoryDao.update(category)
    }

    suspend fun delete(category: Category) {
        categoryDao.delete(category)
    }

    suspend fun deleteCustomCategory(name: String) {
        categoryDao.deleteCustomCategory(name)
    }

    suspend fun initializeDefaultCategories() {
        val existingCategories = getAllCategoriesSync()
        if (existingCategories.isNotEmpty()) return
        
        val defaultCategories = listOf(
            Category(name = "General", colorHex = "#9E9E9E"),
            Category(name = "Social", colorHex = "#2196F3"),
            Category(name = "Work", colorHex = "#FF9800"),
            Category(name = "Personal", colorHex = "#9C27B0"),
            Category(name = "Finance", colorHex = "#4CAF50"),
            Category(name = "Entertainment", colorHex = "#F44336")
        )
        insertAll(defaultCategories)
    }
}

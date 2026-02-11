package com.example.hisabapp.viewmodel

import androidx.compose.animation.core.copy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hisabapp.data.model.Category
import com.example.hisabapp.data.dao.CategoryDao
import com.example.hisabapp.data.toEntity
import com.example.hisabapp.data.toCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CategoryViewModel(private val categoryDao: CategoryDao) : ViewModel() {
    val categories: Flow<List<Category>> = categoryDao.getAllCategories().map { entities ->
        entities.map { it.toCategory() }
    }

    fun addCategory(category: Category) {
        viewModelScope.launch {
            // Ensure ID is 0 for new categories if autoGenerate is true
            categoryDao.insertCategory(category.copy(id = 0).toEntity())
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.updateCategory(category.toEntity())
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.deleteCategory(category.toEntity())
        }
    }

    suspend fun getCategory(categoryId: Long): Category? {
        return categoryDao.getCategoryById(categoryId)?.toCategory()
    }
}

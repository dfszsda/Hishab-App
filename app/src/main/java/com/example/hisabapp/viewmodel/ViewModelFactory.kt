package com.example.hisabapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hisabapp.data.dao.CategoryDao
import com.example.hisabapp.data.database.AppDatabase
import com.example.hisabapp.data.database.TransactionDao

class ViewModelFactory(private val database: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST", "CAST_NEVER_SUCCEEDS")
            return TransactionViewModel(database.transactionDao()) as T
        }
        if (modelClass.isAssignableFrom(CategoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoryViewModel(database.categoryDao() as CategoryDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }

    private fun TransactionViewModel(application: TransactionDao) {
        TODO("Not yet implemented")
    }
}
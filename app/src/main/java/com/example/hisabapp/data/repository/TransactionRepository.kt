package com.example.hisabapp.data.repository

import com.example.hisabapp.data.dao.TransactionDao
import com.example.hisabapp.data.model.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun insert(transaction: Transaction) {
        transactionDao.insert(transaction)
    }

    suspend fun getTotalByType(type: String): Double {
        return transactionDao.getTotalByType(type)
    }
}
@file:Suppress("UNCHECKED_CAST")

package com.example.hisabapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hisabapp.data.database.AppDatabase
import com.example.hisabapp.data.model.Transaction
import com.example.hisabapp.data.repository.TransactionRepository
import com.example.hisabapp.data.dao.TransactionDao
import com.example.hisabapp.data.database.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application, private val dao: TransactionDao) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    val allTransactions: Flow<List<Transaction>>

    init {
        val transactionDao = AppDatabase.getDatabase(application).transactionDao()
        repository = TransactionRepository(transactionDao = transactionDao as TransactionDao)
        allTransactions = repository.allTransactions
    }

    val transactions: List<Transaction> = dao.getAll().map { entities ->
        entities.map { it.toTransaction() }
    } as List<Transaction>

    fun insert(transaction: Transaction) = viewModelScope.launch {
        repository.insert(transaction)
    }

    fun getTotalByType(type: String): LiveData<Double> {
        val liveData = MutableLiveData<Double>()
        viewModelScope.launch {
            liveData.postValue(repository.getTotalByType(type))
        }
        return liveData
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            dao.insert(transaction.toEntity())
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            dao.update(transaction.toEntity())
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            dao.delete(transaction.toEntity())
        }
    }
}

// Extension function to convert TransactionEntity to Transaction
fun TransactionEntity.toTransaction(): Transaction {
    return Transaction(
        id = this.id,
        amount = this.amount,
        type = this.type,
        category = this.category,
        date = this.date,
        description = this.description,
        name = TODO(),
        mobileNumber = TODO()
    )
}

// Extension function to convert Transaction to TransactionEntity
fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = this.id,
        amount = this.amount,
        type = this.type,
        category = this.category,
        date = this.date,
        description = this.description,
        name = TODO(),
        mobileNumber = TODO(),
        note = TODO(),
        paymentMethod = TODO()
    )
}
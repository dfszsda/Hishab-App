package com.example.hisabapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import com.example.hisabapp.data.database.TransactionEntity
import com.example.hisabapp.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity)

    @Query("SELECT * FROM \"transaction\" ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM \"transaction\" WHERE type = :type")
    suspend fun getTotalByType(type: String): Double

    @Delete
    suspend fun delete(transaction: TransactionEntity)
    fun getAll(): Flow<List<TransactionEntity>>
    fun update(toEntity: Any)
    fun insert(transaction: Transaction)
}
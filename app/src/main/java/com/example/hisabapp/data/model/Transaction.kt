package com.example.hisabapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String?,
    val mobileNumber: String?,
    val amount: Double,
    val category: String,
    val date: String,
    val type: String,
    val description: String?
)
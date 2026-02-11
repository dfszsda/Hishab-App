// In your CategoryEntity.kt file (Room entity)
package com.example.hisabapp.data.model // Or your actual package

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L, // id is Long
    val name: String
    // Add other fields as needed
)
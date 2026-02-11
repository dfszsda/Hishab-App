@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package com.example.hisabapp.data

import com.example.hisabapp.data.database.Transaction
import com.example.hisabapp.data.database.TransactionEntity

// Assuming your Room Database Entity is named 'TransactionEntity'
// and your domain/model class is 'com.example.hisabapp.data.Transaction'.
// Adjust property names if they are different in your actual classes.

private val TransactionEntity.note: Any // Or a more specific type if you know it
    get() {
        TODO("Not yet implemented")
    }

/**
 * Converts a [TransactionEntity] (database object) to a [Transaction] (domain/model object).
 */
fun TransactionEntity.toTransaction(): Transaction { // Refers to com.example.hisabapp.data.Transaction
    return Transaction(
        id = this.id,
        amount = this.amount,
        type = this.type,
        category = this.category,
        date = this.date, // Make sure the Date types are compatible or convert accordingly
        note = this.note,
        paymentMethod = this.paymentMethod,
        name = TODO(),
        mobileNumber = TODO(),
        description = TODO() // Add any other properties you have
    )
}

/**
 * Converts a [Transaction] (domain/model object) to a [TransactionEntity] (database object).
 * This is the counterpart to `toTransaction` and is used when saving to the database.
 */
fun Transaction.toEntity(): TransactionEntity { // Refers to com.example.hisabapp.data.Transaction
    return TransactionEntity(
        id = this.id,
        amount = this.amount,
        type = this.type,
        category = this.category,
        date = this.date, // Make sure the Date types are compatible or convert accordingly
        note = this.note,
        paymentMethod = this.paymentMethod,
        name = TODO(),
        mobileNumber = TODO(),
        description = TODO() // Add any other properties you have
    )
}

// You would also need to define your Transaction and TransactionEntity data classes
// if they are not already defined. For context, they might look something like this:

/*
// In a file like com/example/hisabapp/data/model/Transaction.kt (Your domain/model class)
// This is the class your ViewModel and UI might primarily work with.
package com.example.hisabapp.data.model // Or com.example.hisabapp.data if you prefer

import java.util.Date

data class Transaction(
    val id: Long = 0, // Assuming 0 for auto-generate by Room for new entries
    val amount: Double,
    val type: String, // e.g., "Income", "Expense"
    val category: String,
    val date: Date,
    val note: String?,
    val paymentMethod: String?
)
*/

/*
// In a file like com/example/hisabapp/data/database/TransactionEntity.kt (Your Room Entity)
package com.example.hisabapp.data.database // Or com.example.hisabapp.data if you prefer

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "transactions") // Or your actual table name
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: String,
    val category: String,
    val date: Date, // Consider using a TypeConverter for Date in Room
    val note: String?,
    val paymentMethod: String?
)
*/

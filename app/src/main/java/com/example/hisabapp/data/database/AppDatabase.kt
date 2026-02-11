package com.example.hisabapp.data.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
// It's good practice to add @TypeConverters here, even if initially empty,
// if you anticipate needing them later.
// import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

// --- Transaction Data ---
// Model class (not an entity, typically used in UI/domain layers)
data class Transaction(
    val id: Long = 0L,
    val name: String?,
    val mobileNumber: String?,
    val amount: Double,
    val type: String, // "Income" or "Expense"
    val category: String, // This will now come from selected Category.name
    val description: String?,
    val date: String,
    val note: String?, // Changed from Any to String?
    val paymentMethod: String? // Changed from Any to String?
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String?,
    val mobileNumber: String?,
    val amount: Double,
    val type: String,
    val category: String,
    val description: String?,
    val date: String,
    val note: String?, // <<--- CHANGED from Any to String?
    val paymentMethod: String? // <<--- CHANGED from Any to String?
)

// The following mapping functions might need adjustment if _root_ide_package_.com.example.hisabapp.data.model.Transaction
// is different from the Transaction data class defined above.
// Assuming it's the same or similar structure:

fun TransactionEntity.toTransaction() =
    com.example.hisabapp.data.database.Transaction( // Assuming you want to map to the Transaction data class in this file
        id = id,
        name = name,
        mobileNumber = mobileNumber,
        amount = amount,
        type = type,
        category = category,
        description = description,
        date = date,
        note = note, // Pass through the String?
        paymentMethod = paymentMethod // Pass through the String?
    )

fun Transaction.toEntity() = com.example.hisabapp.data.database.TransactionEntity( // Assuming you want to map from the Transaction data class in this file
    id = id,
    name = name,
    mobileNumber = mobileNumber,
    amount = amount,
    type = type,
    category = category,
    description = description,
    date = date,
    note = note, // Pass through the String?
    paymentMethod = paymentMethod // Pass through the String?
)

@Dao
interface TransactionDao { // Removed inheritance from com.example.hisabapp.data.dao.TransactionDao if it's causing issues or not defined
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAll(): Flow<List<TransactionEntity>> // Changed to fun from override fun

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity) // Changed to suspend fun from override suspend fun

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity) // Changed to suspend fun from override suspend fun
}

// --- Category Data ---
// Model class
data class Category(
    val id: Long = 0L,
    val name: String,
    val photoUri: String?, // Store URI of the selected image as String
    val defaultPrice: Double?, // Optional
    val description: String?
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val photoUri: String?,
    val defaultPrice: Double?,
    val description: String?
)

fun CategoryEntity.toCategory() = Category(
    id = id,
    name = name,
    photoUri = photoUri,
    defaultPrice = defaultPrice,
    description = description
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    photoUri = photoUri,
    defaultPrice = defaultPrice,
    description = description
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Long): CategoryEntity?
}

// --- App Database ---
@Database(entities = [TransactionEntity::class, CategoryEntity::class], version = 3, exportSchema = false) // Incremented version due to schema change (Any -> String?)
// @TypeConverters(Converters::class) // Uncomment this if you add TypeConverter classes
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hisab_database"
                )
                    // IMPORTANT: For production, you need a proper migration strategy.
                    // .fallbackToDestructiveMigration() will clear the database on schema change.
                    // Consider what happens to existing user data if you change the schema.
                    // If this is still in development and data loss is acceptable,
                    // fallbackToDestructiveMigration is okay. Otherwise, implement a migration.
                    .fallbackToDestructiveMigration(false) // Changed to true for simplicity during development if version incremented
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Example TypeConverter class (if you need it for other types in the future)
// You would place this in its own file or within this file if preferred.
/*
import androidx.room.TypeConverter
import com.google.gson.Gson // Example, add dependency if using Gson
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // Example for a custom object if 'note' or 'paymentMethod' were complex:
    // data class PaymentDetails(val type: String, val transactionId: String?)
    //
    // @TypeConverter
    // fun fromPaymentDetails(paymentDetails: PaymentDetails?): String? {
    //     return paymentDetails?.let { Gson().toJson(it) }
    // }
    //
    // @TypeConverter
    // fun toPaymentDetails(json: String?): PaymentDetails? {
    //     return json?.let { Gson().fromJson(it, PaymentDetails::class.java) }
    // }
}
*/

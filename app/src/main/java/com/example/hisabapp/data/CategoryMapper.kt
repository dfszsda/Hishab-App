package com.example.hisabapp.data

import com.example.hisabapp.data.model.Category
import com.example.hisabapp.data.model.CategoryEntity

/**
 * Converts a domain [Category] model to a database [CategoryEntity].
 */
fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = this.id.toLong(), // Convert Int to Long here
        name = this.name
    )
}

/**
 * Converts a database [CategoryEntity] to a domain [Category] model.
 */
fun CategoryEntity.toCategory(): Category {
    // If Category.id is Int and CategoryEntity.id is Long,
    // you might need to decide how to handle potential data loss
    // if the Long value is too large for an Int.
    // For typical auto-generated IDs, this is usually safe.
    return Category(
        id = this.id.toInt(), // Convert Long to Int here
        name = this.name
    )
}
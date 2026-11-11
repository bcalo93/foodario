package com.foodario.scanning.domain.model

import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.QuantityUnit

/** Item parsed from a receipt. Ephemeral: never persisted (decision §0.3). */
data class ScannedReceiptItem(
    val name: String,
    val category: FoodCategory,
    val quantity: Double,
    val unit: QuantityUnit,
    val rawLine: String,
)

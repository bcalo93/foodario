package com.foodario.scanning.domain.model

import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.QuantityUnit

/** Item parseado de una factura. Efímero: nunca se persiste (decisión §0.3). */
data class ScannedReceiptItem(
    val name: String,
    val category: FoodCategory,
    val quantity: Double,
    val unit: QuantityUnit,
    val rawLine: String,
)

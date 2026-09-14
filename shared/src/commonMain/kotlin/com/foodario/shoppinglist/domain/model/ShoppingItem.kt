package com.foodario.shoppinglist.domain.model

import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.QuantityUnit
import kotlin.time.Instant

data class ShoppingItem(
    val id: Long,
    val name: String,
    val category: FoodCategory?,
    val quantity: Double?,
    val unit: QuantityUnit?,
    val createdAt: Instant,
)

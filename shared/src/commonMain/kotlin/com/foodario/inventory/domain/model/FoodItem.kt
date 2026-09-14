package com.foodario.inventory.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class FoodItem(
    val id: Long,
    val name: String,
    val category: FoodCategory,
    val quantity: Double,
    val unit: QuantityUnit,
    val isFrozen: Boolean,
    val expirationDate: LocalDate?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

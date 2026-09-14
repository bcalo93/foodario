package com.foodario.shoppinglist.data

import com.foodario.database.Shopping_item
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.shoppinglist.domain.model.ShoppingItem
import kotlin.time.Instant

fun Shopping_item.toDomain(): ShoppingItem = ShoppingItem(
    id = id,
    name = name,
    category = category?.let { FoodCategory.valueOf(it) },
    quantity = quantity,
    unit = unit?.let { QuantityUnit.valueOf(it) },
    createdAt = Instant.fromEpochMilliseconds(created_at),
)

fun ShoppingItem.toEntity(): Shopping_item = Shopping_item(
    id = id,
    name = name,
    category = category?.name,
    quantity = quantity,
    unit = unit?.name,
    created_at = createdAt.toEpochMilliseconds(),
)

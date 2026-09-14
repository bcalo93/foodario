package com.foodario.inventory.data

import com.foodario.database.Food_item
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

private const val MILLIS_PER_DAY = 86_400_000L

fun Food_item.toDomain(): FoodItem = FoodItem(
    id = id,
    name = name,
    category = FoodCategory.valueOf(category),
    quantity = quantity,
    unit = QuantityUnit.valueOf(unit),
    isFrozen = is_frozen == 1L,
    expirationDate = expiration_date?.toLocalDate(),
    createdAt = Instant.fromEpochMilliseconds(created_at),
    updatedAt = Instant.fromEpochMilliseconds(updated_at),
)

fun FoodItem.toEntity(): Food_item = Food_item(
    id = id,
    name = name,
    category = category.name,
    quantity = quantity,
    unit = unit.name,
    is_frozen = if (isFrozen) 1L else 0L,
    expiration_date = expirationDate?.toEpochMillis(),
    created_at = createdAt.toEpochMilliseconds(),
    updated_at = updatedAt.toEpochMilliseconds(),
)

private fun Long.toLocalDate(): LocalDate =
    LocalDate.fromEpochDays((this / MILLIS_PER_DAY).toInt())

private fun LocalDate.toEpochMillis(): Long =
    toEpochDays() * MILLIS_PER_DAY

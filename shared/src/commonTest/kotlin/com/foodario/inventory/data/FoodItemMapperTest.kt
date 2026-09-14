package com.foodario.inventory.data

import com.foodario.database.Food_item
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class FoodItemMapperTest {

    private val createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L)
    private val updatedAt = Instant.fromEpochMilliseconds(1_700_000_100_000L)

    @Test
    fun `toDomain maps all entity fields`() {
        val entity = Food_item(
            id = 1L,
            name = "Leche",
            category = "DAIRY",
            quantity = 2.0,
            unit = "UNIT",
            is_frozen = 0L,
            expiration_date = null,
            created_at = createdAt.toEpochMilliseconds(),
            updated_at = updatedAt.toEpochMilliseconds(),
        )

        val domain = entity.toDomain()

        assertEquals(1L, domain.id)
        assertEquals("Leche", domain.name)
        assertEquals(FoodCategory.DAIRY, domain.category)
        assertEquals(2.0, domain.quantity)
        assertEquals(QuantityUnit.UNIT, domain.unit)
        assertEquals(false, domain.isFrozen)
        assertEquals(null, domain.expirationDate)
        assertEquals(createdAt, domain.createdAt)
        assertEquals(updatedAt, domain.updatedAt)
    }

    @Test
    fun `toDomain maps frozen flag and expiration date`() {
        val entity = Food_item(
            id = 2L,
            name = "Carne",
            category = "MEAT",
            quantity = 0.3,
            unit = "KILOGRAMS",
            is_frozen = 1L,
            expiration_date = 1_700_100_000_000L,
            created_at = createdAt.toEpochMilliseconds(),
            updated_at = updatedAt.toEpochMilliseconds(),
        )

        val domain = entity.toDomain()

        assertEquals(true, domain.isFrozen)
        assertEquals(LocalDate.fromEpochDays((1_700_100_000_000L / 86_400_000L).toInt()), domain.expirationDate)
    }

    @Test
    fun `toEntity maps domain fields back`() {
        val domain = FoodItem(
            id = 1L,
            name = "Leche",
            category = FoodCategory.DAIRY,
            quantity = 2.0,
            unit = QuantityUnit.UNIT,
            isFrozen = false,
            expirationDate = null,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

        val entity = domain.toEntity()

        assertEquals(1L, entity.id)
        assertEquals("Leche", entity.name)
        assertEquals("DAIRY", entity.category)
        assertEquals(2.0, entity.quantity)
        assertEquals("UNIT", entity.unit)
        assertEquals(0L, entity.is_frozen)
        assertEquals(null, entity.expiration_date)
        assertEquals(createdAt.toEpochMilliseconds(), entity.created_at)
        assertEquals(updatedAt.toEpochMilliseconds(), entity.updated_at)
    }

    @Test
    fun `round trip preserves data`() {
        val domain = FoodItem(
            id = 3L,
            name = "Queso",
            category = FoodCategory.DAIRY,
            quantity = 300.0,
            unit = QuantityUnit.GRAMS,
            isFrozen = true,
            expirationDate = LocalDate.fromEpochDays(20_000),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

        assertEquals(domain, domain.toEntity().toDomain())
    }
}

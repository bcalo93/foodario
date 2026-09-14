package com.foodario.shoppinglist.data

import com.foodario.database.Shopping_item
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.shoppinglist.domain.model.ShoppingItem
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ShoppingItemMapperTest {

    private val createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L)

    @Test
    fun `toDomain maps all entity fields`() {
        val entity = Shopping_item(
            id = 1L,
            name = "Huevos",
            category = "PANTRY",
            quantity = 12.0,
            unit = "UNIT",
            created_at = createdAt.toEpochMilliseconds(),
        )

        val domain = entity.toDomain()

        assertEquals(1L, domain.id)
        assertEquals("Huevos", domain.name)
        assertEquals(FoodCategory.PANTRY, domain.category)
        assertEquals(12.0, domain.quantity)
        assertEquals(QuantityUnit.UNIT, domain.unit)
        assertEquals(createdAt, domain.createdAt)
    }

    @Test
    fun `toDomain maps nullable fields to null`() {
        val entity = Shopping_item(
            id = 2L,
            name = "Pan",
            category = null,
            quantity = null,
            unit = null,
            created_at = createdAt.toEpochMilliseconds(),
        )

        val domain = entity.toDomain()

        assertEquals(null, domain.category)
        assertEquals(null, domain.quantity)
        assertEquals(null, domain.unit)
    }

    @Test
    fun `toEntity maps domain fields back`() {
        val domain = ShoppingItem(
            id = 1L,
            name = "Huevos",
            category = FoodCategory.PANTRY,
            quantity = 12.0,
            unit = QuantityUnit.UNIT,
            createdAt = createdAt,
        )

        val entity = domain.toEntity()

        assertEquals(1L, entity.id)
        assertEquals("Huevos", entity.name)
        assertEquals("PANTRY", entity.category)
        assertEquals(12.0, entity.quantity)
        assertEquals("UNIT", entity.unit)
        assertEquals(createdAt.toEpochMilliseconds(), entity.created_at)
    }

    @Test
    fun `round trip preserves data`() {
        val domain = ShoppingItem(
            id = 3L,
            name = "Pan",
            category = null,
            quantity = null,
            unit = null,
            createdAt = createdAt,
        )

        assertEquals(domain, domain.toEntity().toDomain())
    }
}

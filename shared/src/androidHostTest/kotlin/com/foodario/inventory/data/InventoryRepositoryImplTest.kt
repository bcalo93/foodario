package com.foodario.inventory.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.foodario.database.FoodarioDatabase
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InventoryRepositoryImplTest {

    private fun inMemoryRepository(): InventoryRepositoryImpl {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FoodarioDatabase.Schema.create(driver)
        val database = FoodarioDatabase(driver)
        return InventoryRepositoryImpl(database.foodItemQueries, Dispatchers.Unconfined)
    }

    private fun foodItem(
        name: String,
        category: FoodCategory = FoodCategory.DAIRY,
        quantity: Double = 1.0,
    ) = FoodItem(
        id = 0L,
        name = name,
        category = category,
        quantity = quantity,
        unit = QuantityUnit.UNIT,
        isFrozen = false,
        expirationDate = null,
        createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    )

    @Test
    fun `add persists item and returns generated id`() = runTest {
        val repository = inMemoryRepository()

        val added = repository.add(foodItem("Leche", quantity = 2.0))

        assertEquals(1L, added.id)
        val items = repository.observeInventory("", null).first()
        assertEquals(listOf("Leche"), items.map { it.name })
        assertEquals(2.0, items.first().quantity)
    }

    @Test
    fun `observeInventory filters by category`() = runTest {
        val repository = inMemoryRepository()
        repository.add(foodItem("Leche", FoodCategory.DAIRY))
        repository.add(foodItem("Manzana", FoodCategory.FRUITS))

        val dairy = repository.observeInventory("", FoodCategory.DAIRY).first()

        assertEquals(listOf("Leche"), dairy.map { it.name })
    }

    @Test
    fun `observeInventory filters by query`() = runTest {
        val repository = inMemoryRepository()
        repository.add(foodItem("Leche", FoodCategory.DAIRY))
        repository.add(foodItem("Manzana", FoodCategory.FRUITS))

        val result = repository.observeInventory("lech", null).first()

        assertEquals(listOf("Leche"), result.map { it.name })
    }

    @Test
    fun `updateQuantity updates quantity`() = runTest {
        val repository = inMemoryRepository()
        val added = repository.add(foodItem("Leche", quantity = 2.0))

        repository.updateQuantity(added.id, 5.0)

        assertEquals(5.0, repository.getById(added.id)?.quantity)
    }

    @Test
    fun `setFrozen toggles frozen flag`() = runTest {
        val repository = inMemoryRepository()
        val added = repository.add(foodItem("Carne", FoodCategory.MEAT))

        repository.setFrozen(added.id, true)

        assertEquals(true, repository.getById(added.id)?.isFrozen)
    }

    @Test
    fun `delete removes item`() = runTest {
        val repository = inMemoryRepository()
        val added = repository.add(foodItem("Leche"))

        repository.delete(added.id)

        assertNull(repository.getById(added.id))
    }
}

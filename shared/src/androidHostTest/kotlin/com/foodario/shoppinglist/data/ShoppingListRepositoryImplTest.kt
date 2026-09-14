package com.foodario.shoppinglist.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.foodario.database.FoodarioDatabase
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.shoppinglist.domain.model.ShoppingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShoppingListRepositoryImplTest {

    private fun inMemoryRepository(): ShoppingListRepositoryImpl {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FoodarioDatabase.Schema.create(driver)
        val database = FoodarioDatabase(driver)
        return ShoppingListRepositoryImpl(database.shoppingItemQueries, Dispatchers.Unconfined)
    }

    private fun shoppingItem(name: String) = ShoppingItem(
        id = 0L,
        name = name,
        category = FoodCategory.PANTRY,
        quantity = 1.0,
        unit = QuantityUnit.UNIT,
        createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    )

    @Test
    fun `add persists item and observe emits it`() = runTest {
        val repository = inMemoryRepository()

        repository.add(shoppingItem("Huevos"))

        val items = repository.observeShoppingList().first()
        assertEquals(listOf("Huevos"), items.map { it.name })
    }

    @Test
    fun `remove deletes item`() = runTest {
        val repository = inMemoryRepository()
        repository.add(shoppingItem("Huevos"))
        val added = repository.observeShoppingList().first().first()

        repository.remove(added.id)

        assertNull(repository.getById(added.id))
    }

    @Test
    fun `getById maps entity to domain`() = runTest {
        val repository = inMemoryRepository()
        repository.add(shoppingItem("Pan"))
        val added = repository.observeShoppingList().first().first()

        val result = repository.getById(added.id)

        assertEquals("Pan", result?.name)
        assertEquals(FoodCategory.PANTRY, result?.category)
    }
}

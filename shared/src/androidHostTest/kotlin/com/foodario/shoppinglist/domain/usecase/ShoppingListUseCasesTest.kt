package com.foodario.shoppinglist.domain.usecase

import com.foodario.catchError
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.repository.InventoryRepository
import com.foodario.shoppinglist.domain.model.ShoppingItem
import com.foodario.shoppinglist.domain.repository.ShoppingListRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ShoppingListUseCasesTest {

    private val createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L)

    private fun shoppingItem(id: Long = 1L) = ShoppingItem(
        id = id,
        name = "Huevos",
        category = FoodCategory.PANTRY,
        quantity = 12.0,
        unit = QuantityUnit.UNIT,
        createdAt = createdAt,
    )

    @Test
    fun `addToShoppingList delegates to repository`() = runTest {
        val repository = mockk<ShoppingListRepository>()
        coEvery { repository.add(any()) } returns Unit
        val useCase = AddToShoppingListUseCase(repository)

        useCase(
            AddToShoppingListParams(
                name = "Huevos",
                category = FoodCategory.PANTRY,
                quantity = 12.0,
                unit = QuantityUnit.UNIT,
            )
        )

        coVerify {
            repository.add(match { it.name == "Huevos" && it.category == FoodCategory.PANTRY && it.quantity == 12.0 })
        }
    }

    @Test
    fun `addToShoppingList rejects blank name`() = runTest {
        val repository = mockk<ShoppingListRepository>()
        val useCase = AddToShoppingListUseCase(repository)

        val error = catchError { useCase(AddToShoppingListParams(name = " ")) }

        assertIs<IllegalArgumentException>(error)
        coVerify(exactly = 0) { repository.add(any()) }
    }

    @Test
    fun `removeFromShoppingList delegates to repository`() = runTest {
        val repository = mockk<ShoppingListRepository>()
        coEvery { repository.remove(any()) } returns Unit
        val useCase = RemoveFromShoppingListUseCase(repository)

        useCase(RemoveFromShoppingListParams(itemId = 1L))

        coVerify { repository.remove(1L) }
    }

    @Test
    fun `moveToInventory adds to inventory and removes from shopping list`() = runTest {
        val shoppingListRepository = mockk<ShoppingListRepository>()
        val inventoryRepository = mockk<InventoryRepository>()
        coEvery { shoppingListRepository.getById(1L) } returns shoppingItem()
        coEvery { shoppingListRepository.remove(any()) } returns Unit
        coEvery { inventoryRepository.add(any()) } returns FoodItem(
            id = 99L,
            name = "Huevos",
            category = FoodCategory.PANTRY,
            quantity = 12.0,
            unit = QuantityUnit.UNIT,
            isFrozen = false,
            expirationDate = null,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
        val useCase = MoveToInventoryUseCase(shoppingListRepository, inventoryRepository)

        val result = useCase(MoveToInventoryParams(shoppingItemId = 1L))

        assertEquals(99L, result.id)
        coVerify { inventoryRepository.add(match { it.name == "Huevos" && it.category == FoodCategory.PANTRY }) }
        coVerify { shoppingListRepository.remove(1L) }
    }

    @Test
    fun `moveToInventory applies defaults when item has nullable fields`() = runTest {
        val shoppingListRepository = mockk<ShoppingListRepository>()
        val inventoryRepository = mockk<InventoryRepository>()
        coEvery { shoppingListRepository.getById(1L) } returns ShoppingItem(
            id = 1L,
            name = "Pan",
            category = null,
            quantity = null,
            unit = null,
            createdAt = createdAt,
        )
        coEvery { shoppingListRepository.remove(any()) } returns Unit
        coEvery { inventoryRepository.add(any()) } returns FoodItem(
            id = 99L,
            name = "Pan",
            category = FoodCategory.OTHER,
            quantity = 1.0,
            unit = QuantityUnit.UNIT,
            isFrozen = false,
            expirationDate = null,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
        val useCase = MoveToInventoryUseCase(shoppingListRepository, inventoryRepository)

        useCase(MoveToInventoryParams(shoppingItemId = 1L))

        coVerify {
            inventoryRepository.add(
                match {
                    it.category == FoodCategory.OTHER && it.quantity == 1.0 && it.unit == QuantityUnit.UNIT
                }
            )
        }
    }

    @Test
    fun `observeShoppingList delegates to repository`() = runTest {
        val repository = mockk<ShoppingListRepository>()
        every { repository.observeShoppingList() } returns emptyFlow()
        val useCase = ObserveShoppingListUseCase(repository)

        useCase(ObserveShoppingListParams)

        verify { repository.observeShoppingList() }
    }
}

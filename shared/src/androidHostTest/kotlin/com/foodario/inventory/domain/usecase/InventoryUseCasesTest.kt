package com.foodario.inventory.domain.usecase

import com.foodario.catchError
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.repository.InventoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InventoryUseCasesTest {

    private fun foodItem(id: Long = 1L, quantity: Double = 2.0, isFrozen: Boolean = false) = FoodItem(
        id = id,
        name = "Leche",
        category = FoodCategory.DAIRY,
        quantity = quantity,
        unit = QuantityUnit.UNIT,
        isFrozen = isFrozen,
        expirationDate = null,
        createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    )

    @Test
    fun `add delegates to repository and returns item`() = runTest {
        val repository = mockk<InventoryRepository>()
        val added = foodItem(id = 42L)
        coEvery { repository.add(any()) } returns added
        val useCase = AddFoodItemUseCase(repository)

        val result = useCase(AddFoodItemParams(name = "Leche", category = FoodCategory.DAIRY, quantity = 2.0))

        assertEquals(added, result)
        coVerify {
            repository.add(
                match {
                    it.name == "Leche" && it.category == FoodCategory.DAIRY && it.quantity == 2.0
                }
            )
        }
    }

    @Test
    fun `add rejects blank name`() = runTest {
        val repository = mockk<InventoryRepository>()
        val useCase = AddFoodItemUseCase(repository)

        val error = catchError { useCase(AddFoodItemParams(name = "   ", category = FoodCategory.DAIRY)) }

        assertIs<IllegalArgumentException>(error)
        coVerify(exactly = 0) { repository.add(any()) }
    }

    @Test
    fun `add rejects non positive quantity`() = runTest {
        val repository = mockk<InventoryRepository>()
        val useCase = AddFoodItemUseCase(repository)

        val error = catchError { useCase(AddFoodItemParams(name = "Leche", category = FoodCategory.DAIRY, quantity = 0.0)) }

        assertIs<IllegalArgumentException>(error)
        coVerify(exactly = 0) { repository.add(any()) }
    }

    @Test
    fun `updateQuantity rejects negative quantity`() = runTest {
        val repository = mockk<InventoryRepository>()
        val useCase = UpdateQuantityUseCase(repository)

        val error = catchError { useCase(UpdateQuantityParams(itemId = 1L, newQuantity = -1.0)) }

        assertIs<IllegalArgumentException>(error)
        coVerify(exactly = 0) { repository.updateQuantity(any(), any()) }
    }

    @Test
    fun `updateQuantity delegates to repository`() = runTest {
        val repository = mockk<InventoryRepository>()
        coEvery { repository.updateQuantity(any(), any()) } returns Unit
        val useCase = UpdateQuantityUseCase(repository)

        useCase(UpdateQuantityParams(itemId = 1L, newQuantity = 5.0))

        coVerify { repository.updateQuantity(1L, 5.0) }
    }

    @Test
    fun `updateCategory delegates to repository`() = runTest {
        val repository = mockk<InventoryRepository>()
        coEvery { repository.setCategory(any(), any()) } returns Unit
        val useCase = UpdateCategoryUseCase(repository)

        useCase(UpdateCategoryParams(itemId = 1L, category = FoodCategory.FRUITS))

        coVerify { repository.setCategory(1L, FoodCategory.FRUITS) }
    }

    @Test
    fun `updateUnit delegates to repository`() = runTest {
        val repository = mockk<InventoryRepository>()
        coEvery { repository.setUnit(any(), any()) } returns Unit
        val useCase = UpdateUnitUseCase(repository)

        useCase(UpdateUnitParams(itemId = 1L, unit = QuantityUnit.GRAMS))

        coVerify { repository.setUnit(1L, QuantityUnit.GRAMS) }
    }

    @Test
    fun `consume reduces quantity`() = runTest {
        val repository = mockk<InventoryRepository>()
        coEvery { repository.getById(1L) } returns foodItem(quantity = 5.0)
        coEvery { repository.updateQuantity(any(), any()) } returns Unit
        val useCase = ConsumeFoodItemUseCase(repository)

        useCase(ConsumeFoodItemParams(itemId = 1L, amount = 2.0))

        coVerify { repository.updateQuantity(1L, 3.0) }
    }

    @Test
    fun `consume rejects consuming more than available`() = runTest {
        val repository = mockk<InventoryRepository>()
        coEvery { repository.getById(1L) } returns foodItem(quantity = 1.0)
        val useCase = ConsumeFoodItemUseCase(repository)

        val error = catchError { useCase(ConsumeFoodItemParams(itemId = 1L, amount = 2.0)) }

        assertIs<IllegalArgumentException>(error)
        coVerify(exactly = 0) { repository.updateQuantity(any(), any()) }
    }

    @Test
    fun `delete delegates to repository`() = runTest {
        val repository = mockk<InventoryRepository>()
        coEvery { repository.delete(any()) } returns Unit
        val useCase = DeleteFoodItemUseCase(repository)

        useCase(DeleteFoodItemParams(itemId = 1L))

        coVerify { repository.delete(1L) }
    }

    @Test
    fun `toggleFrozen flips frozen flag`() = runTest {
        val repository = mockk<InventoryRepository>()
        coEvery { repository.getById(1L) } returns foodItem(isFrozen = false)
        coEvery { repository.setFrozen(any(), any()) } returns Unit
        val useCase = ToggleFrozenUseCase(repository)

        useCase(ToggleFrozenParams(itemId = 1L))

        coVerify { repository.setFrozen(1L, true) }
    }

    @Test
    fun `observeInventory delegates to repository`() = runTest {
        val repository = mockk<InventoryRepository>()
        every { repository.observeInventory("lech", FoodCategory.DAIRY) } returns emptyFlow()
        val useCase = ObserveInventoryUseCase(repository)

        useCase(ObserveInventoryParams(query = "lech", category = FoodCategory.DAIRY))

        verify { repository.observeInventory("lech", FoodCategory.DAIRY) }
    }

    @Test
    fun `updateExpiration delegates to repository`() = runTest {
        val repository = mockk<InventoryRepository>()
        val date = LocalDate.fromEpochDays(20_000)
        coEvery { repository.setExpirationDate(1L, date) } returns Unit
        val useCase = UpdateExpirationUseCase(repository)

        useCase(UpdateExpirationParams(itemId = 1L, expirationDate = date))

        coVerify { repository.setExpirationDate(1L, date) }
    }

    @Test
    fun `updateExpiration with null clears expiration`() = runTest {
        val repository = mockk<InventoryRepository>()
        coEvery { repository.setExpirationDate(1L, null) } returns Unit
        val useCase = UpdateExpirationUseCase(repository)

        useCase(UpdateExpirationParams(itemId = 1L, expirationDate = null))

        coVerify { repository.setExpirationDate(1L, null) }
    }
}

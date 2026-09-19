package com.foodario.inventory.domain.usecase

import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.repository.InventoryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveFoodItemUseCaseTest {

    private fun foodItem(id: Long = 1L) = FoodItem(
        id = id,
        name = "Leche",
        category = FoodCategory.DAIRY,
        quantity = 2.0,
        unit = QuantityUnit.UNIT,
        isFrozen = false,
        expirationDate = null,
        createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    )

    @Test
    fun `observeFoodItem delegates to repository`() = runTest {
        val repository = mockk<InventoryRepository>()
        every { repository.observeById(1L) } returns flowOf(foodItem())
        val useCase = ObserveFoodItemUseCase(repository)

        useCase(ObserveFoodItemParams(itemId = 1L))

        verify { repository.observeById(1L) }
    }

    @Test
    fun `observeFoodItem emits item from repository`() = runTest {
        val repository = mockk<InventoryRepository>()
        val item = foodItem()
        every { repository.observeById(1L) } returns flowOf(item)
        val useCase = ObserveFoodItemUseCase(repository)

        val result = useCase(ObserveFoodItemParams(itemId = 1L)).first()

        assertEquals(item, result)
    }
}

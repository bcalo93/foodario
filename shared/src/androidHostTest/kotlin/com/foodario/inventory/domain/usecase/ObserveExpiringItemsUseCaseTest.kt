package com.foodario.inventory.domain.usecase

import app.cash.turbine.test
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.repository.InventoryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveExpiringItemsUseCaseTest {

    private val today = LocalDate(2026, 10, 11)

    private val item = FoodItem(
        id = 1L,
        name = "Leche",
        category = FoodCategory.DAIRY,
        quantity = 2.0,
        unit = QuantityUnit.UNIT,
        isFrozen = false,
        expirationDate = today,
        createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    )

    @Test
    fun `observes items expiring from today`() = runTest {
        val repository = mockk<InventoryRepository>()
        every { repository.observeExpiringFrom(today) } returns flowOf(listOf(item))
        val useCase = ObserveExpiringItemsUseCase(repository) { today }

        useCase(ObserveExpiringItemsParams).test {
            assertEquals(listOf(item), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        verify { repository.observeExpiringFrom(today) }
    }
}

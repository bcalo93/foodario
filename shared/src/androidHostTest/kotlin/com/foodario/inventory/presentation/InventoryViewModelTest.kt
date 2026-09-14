package com.foodario.inventory.presentation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.usecase.ObserveInventoryParams
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    private val observeInventory = mockk<ObserveUseCase<ObserveInventoryParams, List<FoodItem>>>()

    private val leche = FoodItem(
        id = 1L,
        name = "Leche",
        category = FoodCategory.DAIRY,
        quantity = 2.0,
        unit = QuantityUnit.UNIT,
        isFrozen = false,
        expirationDate = null,
        createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emits items when observing`() = runTest {
        every { observeInventory(ObserveInventoryParams()) } returns flowOf(listOf(leche))
        val viewModel = InventoryViewModel(observeInventory)

        viewModel.uiState.test {
            val loaded = awaitLoaded()
            assertEquals(listOf(leche), loaded.items)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updates search query via event`() = runTest {
        every { observeInventory(any()) } returns flowOf(listOf(leche))
        val viewModel = InventoryViewModel(observeInventory)

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(InventoryEvent.SearchQueryChanged("lech"))
            val state = awaitLoaded()
            assertEquals("lech", state.searchQuery)
            cancelAndIgnoreRemainingEvents()
        }

        verify { observeInventory(ObserveInventoryParams(query = "lech")) }
    }

    @Test
    fun `filters by category via event`() = runTest {
        every { observeInventory(any()) } returns flowOf(listOf(leche))
        val viewModel = InventoryViewModel(observeInventory)

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(InventoryEvent.CategorySelected(FoodCategory.DAIRY))
            val state = awaitLoaded()
            assertEquals(FoodCategory.DAIRY, state.selectedCategory)
            cancelAndIgnoreRemainingEvents()
        }

        verify { observeInventory(ObserveInventoryParams(category = FoodCategory.DAIRY)) }
    }

    private suspend fun ReceiveTurbine<InventoryUiState>.awaitLoaded(): InventoryUiState {
        while (true) {
            val state = awaitItem()
            if (!state.isLoading && state.error == null) return state
        }
    }
}

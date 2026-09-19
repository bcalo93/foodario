package com.foodario.inventory.presentation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.core.domain.usecase.UseCase
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.usecase.AddFoodItemParams
import com.foodario.inventory.domain.usecase.DeleteFoodItemParams
import com.foodario.inventory.domain.usecase.ObserveInventoryParams
import com.foodario.inventory.domain.usecase.UpdateQuantityParams
import io.mockk.coEvery
import io.mockk.coVerify
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
    private val addFoodItem = mockk<UseCase<AddFoodItemParams, FoodItem>>()
    private val updateQuantity = mockk<UseCase<UpdateQuantityParams, Unit>>()
    private val deleteFoodItem = mockk<UseCase<DeleteFoodItemParams, Unit>>()

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
        val viewModel = viewModel()

        viewModel.uiState.test {
            val loaded = awaitLoaded()
            assertEquals(listOf(leche), loaded.items)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updates search query via event`() = runTest {
        every { observeInventory(any()) } returns flowOf(listOf(leche))
        val viewModel = viewModel()

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
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(InventoryEvent.CategorySelected(FoodCategory.DAIRY))
            val state = awaitLoaded()
            assertEquals(FoodCategory.DAIRY, state.selectedCategory)
            cancelAndIgnoreRemainingEvents()
        }

        verify { observeInventory(ObserveInventoryParams(category = FoodCategory.DAIRY)) }
    }

    @Test
    fun `quick add calls add use case with defaults`() = runTest {
        every { observeInventory(any()) } returns flowOf(listOf(leche))
        coEvery { addFoodItem(any()) } returns leche
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(InventoryEvent.QuickAddCategorySelected(FoodCategory.DAIRY))
            viewModel.onEvent(InventoryEvent.QuickAdd("Leche"))
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { addFoodItem(AddFoodItemParams(name = "Leche", category = FoodCategory.DAIRY)) }
    }

    @Test
    fun `quick add category selected updates quick add category flow`() = runTest {
        every { observeInventory(any()) } returns flowOf(listOf(leche))
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(InventoryEvent.QuickAddCategorySelected(FoodCategory.FRUITS))
            assertEquals(FoodCategory.FRUITS, viewModel.quickAddCategory.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `increase quantity calls update quantity with one more`() = runTest {
        every { observeInventory(any()) } returns flowOf(listOf(leche))
        coEvery { updateQuantity(any()) } returns Unit
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(InventoryEvent.IncreaseQuantity(leche.id, leche.quantity))
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { updateQuantity(UpdateQuantityParams(leche.id, 3.0)) }
    }

    @Test
    fun `delete calls delete use case`() = runTest {
        every { observeInventory(any()) } returns flowOf(listOf(leche))
        coEvery { deleteFoodItem(any()) } returns Unit
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(InventoryEvent.Delete(leche.id))
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { deleteFoodItem(DeleteFoodItemParams(leche.id)) }
    }

    private suspend fun ReceiveTurbine<InventoryUiState>.awaitLoaded(): InventoryUiState {
        while (true) {
            val state = awaitItem()
            if (!state.isLoading && state.error == null) return state
        }
    }

    private fun viewModel() = InventoryViewModel(
        observeInventory = observeInventory,
        addFoodItem = addFoodItem,
        updateQuantity = updateQuantity,
        deleteFoodItem = deleteFoodItem,
    )
}

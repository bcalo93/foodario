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
    fun `quick add emits item added effect`() = runTest {
        every { observeInventory(any()) } returns flowOf(emptyList())
        coEvery { addFoodItem(any()) } returns leche
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(InventoryEvent.QuickAdd("Leche"))
            assertEquals(
                InventoryEffect.ItemAdded(
                    itemId = leche.id,
                    name = leche.name,
                    category = leche.category,
                ),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `increase quantity uses the selected unit step`() = runTest {
        every { observeInventory(any()) } returns flowOf(listOf(leche))
        coEvery { updateQuantity(any()) } returns Unit
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(
                InventoryEvent.IncreaseQuantity(
                    itemId = leche.id,
                    currentQuantity = leche.quantity,
                    unit = leche.unit,
                )
            )
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { updateQuantity(UpdateQuantityParams(leche.id, 3.0)) }
    }

    @Test
    fun `increase quantity uses grams step`() = runTest {
        val cheese = leche.copy(quantity = 300.0, unit = QuantityUnit.GRAMS)
        every { observeInventory(any()) } returns flowOf(listOf(cheese))
        coEvery { updateQuantity(any()) } returns Unit
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(
                InventoryEvent.IncreaseQuantity(
                    itemId = cheese.id,
                    currentQuantity = cheese.quantity,
                    unit = cheese.unit,
                )
            )
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { updateQuantity(UpdateQuantityParams(cheese.id, 350.0)) }
    }

    @Test
    fun `increase quantity uses decimal step for kilograms`() = runTest {
        val meat = leche.copy(quantity = 1.0, unit = QuantityUnit.KILOGRAMS)
        every { observeInventory(any()) } returns flowOf(listOf(meat))
        coEvery { updateQuantity(any()) } returns Unit
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(
                InventoryEvent.IncreaseQuantity(
                    itemId = meat.id,
                    currentQuantity = meat.quantity,
                    unit = meat.unit,
                )
            )
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { updateQuantity(UpdateQuantityParams(meat.id, 1.1)) }
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

package com.foodario.shoppinglist.presentation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.core.domain.usecase.UseCase
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.shoppinglist.domain.model.ShoppingItem
import com.foodario.shoppinglist.domain.usecase.AddToShoppingListParams
import com.foodario.shoppinglist.domain.usecase.MoveToInventoryParams
import com.foodario.shoppinglist.domain.usecase.ObserveShoppingListParams
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
class ShoppingListViewModelTest {

    private val observeShoppingList = mockk<ObserveUseCase<ObserveShoppingListParams, List<ShoppingItem>>>()
    private val moveToInventory = mockk<UseCase<MoveToInventoryParams, FoodItem>>()
    private val addToShoppingList = mockk<UseCase<AddToShoppingListParams, Unit>>()

    private val leche = ShoppingItem(
        id = 1L,
        name = "Leche",
        category = FoodCategory.DAIRY,
        quantity = 2.0,
        unit = QuantityUnit.UNIT,
        createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    )

    private val foodItem = FoodItem(
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
        every { observeShoppingList(ObserveShoppingListParams) } returns flowOf(listOf(leche))
        val viewModel = ShoppingListViewModel(observeShoppingList, moveToInventory, addToShoppingList)

        viewModel.uiState.test {
            val loaded = awaitLoaded()
            assertEquals(listOf(leche), loaded.items)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggle checked adds id to checkedIds`() = runTest {
        every { observeShoppingList(ObserveShoppingListParams) } returns flowOf(listOf(leche))
        val viewModel = ShoppingListViewModel(observeShoppingList, moveToInventory, addToShoppingList)

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(ShoppingListEvent.ToggleChecked(1L))
            val state = awaitItem()
            assertEquals(setOf(1L), state.checkedIds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggle checked twice removes id from checkedIds`() = runTest {
        every { observeShoppingList(ObserveShoppingListParams) } returns flowOf(listOf(leche))
        val viewModel = ShoppingListViewModel(observeShoppingList, moveToInventory, addToShoppingList)

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(ShoppingListEvent.ToggleChecked(1L))
            awaitItem()
            viewModel.onEvent(ShoppingListEvent.ToggleChecked(1L))
            val state = awaitItem()
            assertEquals(emptySet(), state.checkedIds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `move to inventory calls move use case`() = runTest {
        every { observeShoppingList(ObserveShoppingListParams) } returns flowOf(listOf(leche))
        coEvery { moveToInventory(any()) } returns foodItem
        val viewModel = ShoppingListViewModel(observeShoppingList, moveToInventory, addToShoppingList)

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(ShoppingListEvent.MoveToInventory(1L))
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { moveToInventory(MoveToInventoryParams(1L)) }
    }

    @Test
    fun `quick add calls add use case with selected category`() = runTest {
        every { observeShoppingList(ObserveShoppingListParams) } returns flowOf(listOf(leche))
        coEvery { addToShoppingList(any()) } returns Unit
        val viewModel = ShoppingListViewModel(observeShoppingList, moveToInventory, addToShoppingList)

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(ShoppingListEvent.QuickAddCategorySelected(FoodCategory.DAIRY))
            viewModel.onEvent(ShoppingListEvent.QuickAdd("Leche"))
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { addToShoppingList(AddToShoppingListParams(name = "Leche", category = FoodCategory.DAIRY)) }
    }

    @Test
    fun `quick add defaults to other category`() = runTest {
        every { observeShoppingList(ObserveShoppingListParams) } returns flowOf(listOf(leche))
        coEvery { addToShoppingList(any()) } returns Unit
        val viewModel = ShoppingListViewModel(observeShoppingList, moveToInventory, addToShoppingList)

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(ShoppingListEvent.QuickAdd("Pan"))
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { addToShoppingList(AddToShoppingListParams(name = "Pan", category = FoodCategory.OTHER)) }
    }

    private suspend fun ReceiveTurbine<ShoppingListUiState>.awaitLoaded(): ShoppingListUiState {
        while (true) {
            val state = awaitItem()
            if (!state.isLoading && state.error == null) return state
        }
    }
}

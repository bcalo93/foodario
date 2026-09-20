package com.foodario.inventory.presentation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.core.domain.usecase.UseCase
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.usecase.ConsumeFoodItemParams
import com.foodario.inventory.domain.usecase.DeleteFoodItemParams
import com.foodario.inventory.domain.usecase.ObserveFoodItemParams
import com.foodario.inventory.domain.usecase.ToggleFrozenParams
import com.foodario.inventory.domain.usecase.UpdateCategoryParams
import com.foodario.inventory.domain.usecase.UpdateExpirationParams
import com.foodario.inventory.domain.usecase.UpdateQuantityParams
import com.foodario.inventory.domain.usecase.UpdateUnitParams
import com.foodario.shoppinglist.domain.usecase.AddToShoppingListParams
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
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FoodDetailViewModelTest {

    private val itemId = 1L

    private val observeFoodItem = mockk<ObserveUseCase<ObserveFoodItemParams, FoodItem?>>()
    private val updateQuantity = mockk<UseCase<UpdateQuantityParams, Unit>>()
    private val updateCategory = mockk<UseCase<UpdateCategoryParams, Unit>>()
    private val updateUnit = mockk<UseCase<UpdateUnitParams, Unit>>()
    private val consumeFoodItem = mockk<UseCase<ConsumeFoodItemParams, Unit>>()
    private val toggleFrozen = mockk<UseCase<ToggleFrozenParams, Unit>>()
    private val updateExpiration = mockk<UseCase<UpdateExpirationParams, Unit>>()
    private val deleteFoodItem = mockk<UseCase<DeleteFoodItemParams, Unit>>()
    private val addToShoppingList = mockk<UseCase<AddToShoppingListParams, Unit>>()

    private val leche = FoodItem(
        id = itemId,
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

    private fun viewModel() = FoodDetailViewModel(
        itemId = itemId,
        observeFoodItem = observeFoodItem,
        updateQuantity = updateQuantity,
        updateCategory = updateCategory,
        updateUnit = updateUnit,
        consumeFoodItem = consumeFoodItem,
        toggleFrozen = toggleFrozen,
        updateExpiration = updateExpiration,
        deleteFoodItem = deleteFoodItem,
        addToShoppingList = addToShoppingList,
    )

    @Test
    fun `emits item when observing`() = runTest {
        every { observeFoodItem(ObserveFoodItemParams(itemId)) } returns flowOf(leche)
        val viewModel = viewModel()

        viewModel.uiState.test {
            val loaded = awaitLoaded()
            assertEquals(leche, loaded.item)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `increment quantity calls update quantity`() = runTest {
        every { observeFoodItem(ObserveFoodItemParams(itemId)) } returns flowOf(leche)
        coEvery { updateQuantity(any()) } returns Unit
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(FoodDetailEvent.IncrementQuantity)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { updateQuantity(UpdateQuantityParams(itemId, leche.quantity + 1.0)) }
    }

    @Test
    fun `decrement quantity does not go below zero`() = runTest {
        val single = leche.copy(quantity = 1.0)
        every { observeFoodItem(ObserveFoodItemParams(itemId)) } returns flowOf(single)
        coEvery { updateQuantity(any()) } returns Unit
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(FoodDetailEvent.DecrementQuantity)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { updateQuantity(UpdateQuantityParams(itemId, 0.0)) }
    }

    @Test
    fun `consume calls consume use case`() = runTest {
        every { observeFoodItem(ObserveFoodItemParams(itemId)) } returns flowOf(leche)
        coEvery { consumeFoodItem(any()) } returns Unit
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(FoodDetailEvent.Consume)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { consumeFoodItem(ConsumeFoodItemParams(itemId)) }
    }

    @Test
    fun `consume at zero emits quantity depleted effect`() = runTest {
        every { observeFoodItem(ObserveFoodItemParams(itemId)) } returns flowOf(leche.copy(quantity = 1.0))
        coEvery { consumeFoodItem(any()) } returns Unit
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.effects.test {
                viewModel.onEvent(FoodDetailEvent.Consume)
                assertEquals(FoodDetailEffect.QuantityDepleted, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggle frozen calls toggle use case`() = runTest {
        every { observeFoodItem(ObserveFoodItemParams(itemId)) } returns flowOf(leche)
        coEvery { toggleFrozen(any()) } returns Unit
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(FoodDetailEvent.ToggleFrozen)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { toggleFrozen(ToggleFrozenParams(itemId)) }
    }

    @Test
    fun `category changed calls update category use case`() = runTest {
        every { observeFoodItem(ObserveFoodItemParams(itemId)) } returns flowOf(leche)
        coEvery { updateCategory(any()) } returns Unit
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(FoodDetailEvent.CategoryChanged(FoodCategory.FRUITS))
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { updateCategory(UpdateCategoryParams(itemId, FoodCategory.FRUITS)) }
    }

    @Test
    fun `unit changed calls update unit use case`() = runTest {
        every { observeFoodItem(ObserveFoodItemParams(itemId)) } returns flowOf(leche)
        coEvery { updateUnit(any()) } returns Unit
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(FoodDetailEvent.UnitChanged(QuantityUnit.GRAMS))
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { updateUnit(UpdateUnitParams(itemId, QuantityUnit.GRAMS)) }
    }

    @Test
    fun `expiration date changed calls update expiration`() = runTest {
        val date = LocalDate.fromEpochDays(20_000)
        every { observeFoodItem(ObserveFoodItemParams(itemId)) } returns flowOf(leche)
        coEvery { updateExpiration(any()) } returns Unit
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onEvent(FoodDetailEvent.ExpirationDateChanged(date))
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { updateExpiration(UpdateExpirationParams(itemId, date)) }
    }

    @Test
    fun `delete calls delete use case`() = runTest {
        every { observeFoodItem(ObserveFoodItemParams(itemId)) } returns flowOf(leche)
        coEvery { deleteFoodItem(any()) } returns Unit
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.effects.test {
                viewModel.onEvent(FoodDetailEvent.Delete)
                assertEquals(FoodDetailEffect.Deleted, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { deleteFoodItem(DeleteFoodItemParams(itemId)) }
    }

    @Test
    fun `add to shopping list calls add use case with item data`() = runTest {
        every { observeFoodItem(ObserveFoodItemParams(itemId)) } returns flowOf(leche)
        coEvery { addToShoppingList(any()) } returns Unit
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.effects.test {
                viewModel.onEvent(FoodDetailEvent.AddToShoppingList)
                assertEquals(FoodDetailEffect.AddedToShoppingList, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }

        coVerify {
            addToShoppingList(
                AddToShoppingListParams(
                    name = leche.name,
                    category = leche.category,
                    quantity = leche.quantity,
                    unit = leche.unit,
                )
            )
        }
    }

    private suspend fun ReceiveTurbine<FoodDetailUiState>.awaitLoaded(): FoodDetailUiState {
        while (true) {
            val state = awaitItem()
            if (!state.isLoading && state.error == null) return state
        }
    }
}

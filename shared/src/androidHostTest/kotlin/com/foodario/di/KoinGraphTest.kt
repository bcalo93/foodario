package com.foodario.di

import com.foodario.database.DatabaseDriverFactory
import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.repository.InventoryRepository
import com.foodario.inventory.domain.usecase.ObserveFoodItemParams
import com.foodario.inventory.domain.usecase.ObserveFoodItemUseCase
import com.foodario.inventory.domain.usecase.ObserveInventoryParams
import com.foodario.inventory.domain.usecase.ObserveInventoryUseCase
import com.foodario.inventory.presentation.FoodDetailViewModel
import com.foodario.inventory.presentation.InventoryViewModel
import com.foodario.shoppinglist.domain.model.ShoppingItem
import com.foodario.shoppinglist.domain.repository.ShoppingListRepository
import com.foodario.shoppinglist.domain.usecase.ObserveShoppingListParams
import com.foodario.shoppinglist.domain.usecase.ObserveShoppingListUseCase
import com.foodario.shoppinglist.presentation.ShoppingListViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.dsl.module
import org.koin.dsl.koinApplication
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.test.verify.verify
import kotlin.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class KoinGraphTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `verify koin graph resolves all definitions`() {
        val module = module {
            includes(dataModule, domainModule, presentationModule)
        }
        module.verify(
            extraTypes = listOf(DatabaseDriverFactory::class, Long::class),
        ).verify()
    }

    @Test
    fun `resolve view models with the use cases for their feature`() = runTest {
        val foodItem = FoodItem(
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
        val shoppingItem = ShoppingItem(
            id = 2L,
            name = "Huevos",
            category = FoodCategory.PANTRY,
            quantity = 12.0,
            unit = QuantityUnit.UNIT,
            createdAt = foodItem.createdAt,
        )
        val inventoryRepository = mockk<InventoryRepository>()
        val shoppingListRepository = mockk<ShoppingListRepository>()
        every { inventoryRepository.observeInventory("", null) } returns flowOf(listOf(foodItem))
        every { inventoryRepository.observeById(1L) } returns flowOf(foodItem)
        every { shoppingListRepository.observeShoppingList() } returns flowOf(listOf(shoppingItem))

        val application = koinApplication {
            modules(
                module {
                    single<InventoryRepository> { inventoryRepository }
                    single<ShoppingListRepository> { shoppingListRepository }
                },
                domainModule,
                presentationModule,
            )
        }

        val koin = application.koin
        assertIs<ObserveInventoryUseCase>(
            koin.get<ObserveUseCase<ObserveInventoryParams, List<FoodItem>>>(named("observeInventory")),
        )
        assertIs<ObserveFoodItemUseCase>(
            koin.get<ObserveUseCase<ObserveFoodItemParams, FoodItem?>>(named("observeFoodItem")),
        )
        assertIs<ObserveShoppingListUseCase>(
            koin.get<ObserveUseCase<ObserveShoppingListParams, List<ShoppingItem>>>(named("observeShoppingList")),
        )

        val inventoryState = koin.get<InventoryViewModel>().uiState.first { !it.isLoading }
        val shoppingState = koin.get<ShoppingListViewModel>().uiState.first { !it.isLoading }
        val detailState = koin
            .get<FoodDetailViewModel> { parametersOf(1L) }
            .uiState
            .first { !it.isLoading }
        application.close()

        assertEquals(listOf(foodItem), inventoryState.items)
        assertEquals(null, inventoryState.error)
        assertEquals(listOf(shoppingItem), shoppingState.items)
        assertEquals(null, shoppingState.error)
        assertEquals(foodItem, detailState.item)
        assertEquals(null, detailState.error)
    }
}

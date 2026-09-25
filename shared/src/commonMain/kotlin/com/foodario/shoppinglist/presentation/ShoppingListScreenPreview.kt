package com.foodario.shoppinglist.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.foodario.core.domain.usecase.UseCase
import com.foodario.core.presentation.preview.previewObserveUseCase
import com.foodario.core.presentation.preview.previewUnitUseCase
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.shoppinglist.domain.model.ShoppingItem
import com.foodario.shoppinglist.domain.usecase.AddToShoppingListParams
import com.foodario.shoppinglist.domain.usecase.MoveToInventoryParams
import kotlin.time.Instant

private fun sampleShoppingList(): List<ShoppingItem> =
    listOf(
        sampleShoppingItem(1L, "Leche", FoodCategory.DAIRY, 2.0, QuantityUnit.UNIT),
        sampleShoppingItem(2L, "Pechuga", FoodCategory.MEAT, 300.0, QuantityUnit.GRAMS),
        sampleShoppingItem(3L, "Manzanas", FoodCategory.FRUITS, 1.0, QuantityUnit.KILOGRAMS),
    )

private fun sampleShoppingItem(
    id: Long,
    name: String,
    category: FoodCategory,
    quantity: Double,
    unit: QuantityUnit,
) = ShoppingItem(
    id = id,
    name = name,
    category = category,
    quantity = quantity,
    unit = unit,
    createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
)

private fun fakeMoveToInventoryUseCase(): UseCase<MoveToInventoryParams, FoodItem> =
    object : UseCase<MoveToInventoryParams, FoodItem> {
        override suspend fun invoke(params: MoveToInventoryParams): FoodItem =
            FoodItem(
                id = 0L,
                name = "",
                category = FoodCategory.OTHER,
                quantity = 1.0,
                unit = QuantityUnit.UNIT,
                isFrozen = false,
                expirationDate = null,
                createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
                updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
            )
    }

private fun fakeAddToShoppingListUseCase(): UseCase<AddToShoppingListParams, Unit> =
    object : UseCase<AddToShoppingListParams, Unit> {
        override suspend fun invoke(params: AddToShoppingListParams) = Unit
    }

@Preview
@Composable
private fun ShoppingListScreenLightPreview() {
    FoodarioTheme(darkTheme = false) {
        ShoppingListScreen(
            viewModel = ShoppingListViewModel(
                observeShoppingList = previewObserveUseCase(sampleShoppingList()),
                moveToInventory = fakeMoveToInventoryUseCase(),
                addToShoppingList = fakeAddToShoppingListUseCase(),
                removeFromShoppingList = previewUnitUseCase(),
            ),
        )
    }
}

@Preview
@Composable
private fun ShoppingListScreenDarkPreview() {
    FoodarioTheme(darkTheme = true) {
        ShoppingListScreen(
            viewModel = ShoppingListViewModel(
                observeShoppingList = previewObserveUseCase(sampleShoppingList()),
                moveToInventory = fakeMoveToInventoryUseCase(),
                addToShoppingList = fakeAddToShoppingListUseCase(),
                removeFromShoppingList = previewUnitUseCase(),
            ),
        )
    }
}

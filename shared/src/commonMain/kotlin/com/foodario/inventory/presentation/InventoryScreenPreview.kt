package com.foodario.inventory.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.foodario.core.domain.usecase.UseCase
import com.foodario.core.presentation.preview.previewObserveUseCase
import com.foodario.core.presentation.preview.previewUnitUseCase
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.usecase.AddFoodItemParams
import kotlin.time.Instant

private fun sampleInventory(): List<FoodItem> =
    listOf(
        sampleItem(1L, "Leche", FoodCategory.DAIRY, 2.0, QuantityUnit.UNIT, isFrozen = false),
        sampleItem(2L, "Pechuga", FoodCategory.MEAT, 300.0, QuantityUnit.GRAMS, isFrozen = true),
        sampleItem(3L, "Manzanas", FoodCategory.FRUITS, 1.0, QuantityUnit.KILOGRAMS, isFrozen = false),
    )

private fun sampleItem(
    id: Long,
    name: String,
    category: FoodCategory,
    quantity: Double,
    unit: QuantityUnit,
    isFrozen: Boolean,
) = FoodItem(
    id = id,
    name = name,
    category = category,
    quantity = quantity,
    unit = unit,
    isFrozen = isFrozen,
    expirationDate = null,
    createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
)

private fun fakeAddFoodItemUseCase(): UseCase<AddFoodItemParams, FoodItem> =
    object : UseCase<AddFoodItemParams, FoodItem> {
        override suspend fun invoke(params: AddFoodItemParams): FoodItem =
            sampleItem(
                id = 0L,
                name = params.name,
                category = params.category,
                quantity = params.quantity,
                unit = params.unit,
                isFrozen = params.isFrozen,
            )
    }

@Preview
@Composable
private fun InventoryScreenLightPreview() {
    FoodarioTheme(darkTheme = false) {
        InventoryScreen(
            viewModel = InventoryViewModel(
                observeInventory = previewObserveUseCase(sampleInventory()),
                addFoodItem = fakeAddFoodItemUseCase(),
                updateQuantity = previewUnitUseCase(),
                deleteFoodItem = previewUnitUseCase(),
            )
        )
    }
}

@Preview
@Composable
private fun InventoryScreenDarkPreview() {
    FoodarioTheme(darkTheme = true) {
        InventoryScreen(
            viewModel = InventoryViewModel(
                observeInventory = previewObserveUseCase(sampleInventory()),
                addFoodItem = fakeAddFoodItemUseCase(),
                updateQuantity = previewUnitUseCase(),
                deleteFoodItem = previewUnitUseCase(),
            )
        )
    }
}

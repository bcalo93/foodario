package com.foodario.inventory.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.foodario.core.presentation.preview.previewObserveUseCase
import com.foodario.core.presentation.preview.previewUnitUseCase
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

private fun sampleItem() = FoodItem(
    id = 1L,
    name = "Leche",
    category = FoodCategory.DAIRY,
    quantity = 2.0,
    unit = QuantityUnit.UNIT,
    isFrozen = false,
    expirationDate = LocalDate.fromEpochDays(20_000),
    createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
)

@Preview
@Composable
private fun FoodDetailScreenLightPreview() {
    FoodarioTheme(darkTheme = false) {
        FoodDetailScreen(
            itemId = 1L,
            viewModel = FoodDetailViewModel(
                itemId = 1L,
                observeFoodItem = previewObserveUseCase(sampleItem()),
                updateQuantity = previewUnitUseCase(),
                updateCategory = previewUnitUseCase(),
                updateUnit = previewUnitUseCase(),
                toggleFrozen = previewUnitUseCase(),
                updateExpiration = previewUnitUseCase(),
                deleteFoodItem = previewUnitUseCase(),
                addToShoppingList = previewUnitUseCase(),
            ),
        )
    }
}

@Preview
@Composable
private fun FoodDetailScreenDarkPreview() {
    FoodarioTheme(darkTheme = true) {
        FoodDetailScreen(
            itemId = 1L,
            viewModel = FoodDetailViewModel(
                itemId = 1L,
                observeFoodItem = previewObserveUseCase(sampleItem()),
                updateQuantity = previewUnitUseCase(),
                updateCategory = previewUnitUseCase(),
                updateUnit = previewUnitUseCase(),
                toggleFrozen = previewUnitUseCase(),
                updateExpiration = previewUnitUseCase(),
                deleteFoodItem = previewUnitUseCase(),
                addToShoppingList = previewUnitUseCase(),
            ),
        )
    }
}

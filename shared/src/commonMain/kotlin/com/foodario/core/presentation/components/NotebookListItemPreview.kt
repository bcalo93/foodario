package com.foodario.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.QuantityUnit

@Preview
@Composable
private fun NotebookListItemLightPreview() {
    FoodarioTheme(darkTheme = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper),
        ) {
            NotebookListItem(
                name = "Leche",
                category = FoodCategory.DAIRY,
                quantity = 2.0,
                unit = QuantityUnit.UNIT,
                expiresInDays = 1,
            )
            NotebookListItem(
                name = "Pechuga",
                category = FoodCategory.MEAT,
                quantity = 300.0,
                unit = QuantityUnit.GRAMS,
                isFrozen = true,
            )
            NotebookListItem(
                name = "Manzanas",
                category = FoodCategory.FRUITS,
                quantity = 1.0,
                unit = QuantityUnit.KILOGRAMS,
            )
        }
    }
}

@Preview
@Composable
private fun NotebookListItemDarkPreview() {
    FoodarioTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper),
        ) {
            NotebookListItem(
                name = "Leche",
                category = FoodCategory.DAIRY,
                quantity = 2.0,
                unit = QuantityUnit.UNIT,
                expiresInDays = 1,
            )
            NotebookListItem(
                name = "Pechuga",
                category = FoodCategory.MEAT,
                quantity = 300.0,
                unit = QuantityUnit.GRAMS,
                isFrozen = true,
            )
            NotebookListItem(
                name = "Manzanas",
                category = FoodCategory.FRUITS,
                quantity = 1.0,
                unit = QuantityUnit.KILOGRAMS,
            )
        }
    }
}

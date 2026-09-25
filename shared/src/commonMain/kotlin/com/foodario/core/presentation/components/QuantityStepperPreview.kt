package com.foodario.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.inventory.domain.model.QuantityUnit

@Preview
@Composable
private fun QuantityStepperUnitLightPreview() {
    FoodarioTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper)
                .padding(FoodarioTheme.dimensions.lg),
        ) {
            QuantityStepper(
                quantity = 2.0,
                unit = QuantityUnit.UNIT,
                onIncrement = {},
                onDecrement = {},
                onQuantityClick = {},
                onUnitClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun QuantityStepperUnitDarkPreview() {
    FoodarioTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper)
                .padding(FoodarioTheme.dimensions.lg),
        ) {
            QuantityStepper(
                quantity = 2.0,
                unit = QuantityUnit.UNIT,
                onIncrement = {},
                onDecrement = {},
                onQuantityClick = {},
                onUnitClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun QuantityStepperGramsLightPreview() {
    FoodarioTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper)
                .padding(FoodarioTheme.dimensions.lg),
        ) {
            QuantityStepper(
                quantity = 300.0,
                unit = QuantityUnit.GRAMS,
                onIncrement = {},
                onDecrement = {},
                onQuantityClick = {},
                onUnitClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun QuantityStepperGramsDarkPreview() {
    FoodarioTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper)
                .padding(FoodarioTheme.dimensions.lg),
        ) {
            QuantityStepper(
                quantity = 300.0,
                unit = QuantityUnit.GRAMS,
                onIncrement = {},
                onDecrement = {},
                onQuantityClick = {},
                onUnitClick = {},
            )
        }
    }
}

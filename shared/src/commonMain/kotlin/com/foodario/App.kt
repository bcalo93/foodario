package com.foodario

import androidx.compose.runtime.Composable
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.navigation.FoodarioNavHost

@Composable
fun App() {
    FoodarioTheme {
        FoodarioNavHost()
    }
}

package com.foodario.core.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.foodario.inventory.domain.model.FoodCategory

@Composable
fun categoryColor(category: FoodCategory): Color {
    val base = categoryColors.getValue(category)
    return if (FoodarioTheme.isDark) base.copy(alpha = 0.28f) else base
}

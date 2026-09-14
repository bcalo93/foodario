package com.foodario.core.presentation.theme

import androidx.compose.ui.graphics.Color
import com.foodario.inventory.domain.model.FoodCategory

val paperLight = Color(0xFFFAF6EE)
val paperElevatedLight = Color(0xFFFFFFFF)
val inkLight = Color(0xFF1F2A44)
val inkSoftLight = Color(0xFF5A6378)
val penBlueLight = Color(0xFF35507E)
val marginRedLight = Color(0xFFD9544F)
val pencilGrayLight = Color(0xFF9AA1B0)

val paperDark = Color(0xFF242119)
val paperElevatedDark = Color(0xFF2F2B22)
val inkDark = Color(0xFFECE6D8)
val inkSoftDark = Color(0xFFA8A294)
val penBlueDark = Color(0xFF9DB8DE)
val marginRedDark = Color(0xFFE08A82)
val pencilGrayDark = Color(0xFF6B665A)

val categoryColors = mapOf(
    FoodCategory.DAIRY to Color(0xFFF3E3B2),
    FoodCategory.MEAT to Color(0xFFF0C8C0),
    FoodCategory.VEGETABLES to Color(0xFFD2E8B8),
    FoodCategory.FRUITS to Color(0xFFF6D3A8),
    FoodCategory.PANTRY to Color(0xFFE4D9C8),
    FoodCategory.FROZEN to Color(0xFFC6E0EC),
    FoodCategory.BEVERAGES to Color(0xFFDCD2EA),
    FoodCategory.COOKED to Color(0xFFF2CCD8),
    FoodCategory.OTHER to Color(0xFFE0E0E0),
)

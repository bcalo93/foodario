package com.foodario.core.presentation.components

import com.foodario.inventory.domain.model.FoodCategory

val FoodCategory.emoji: String
    get() = when (this) {
        FoodCategory.DAIRY -> "🥛"
        FoodCategory.MEAT -> "🥩"
        FoodCategory.VEGETABLES -> "🥦"
        FoodCategory.FRUITS -> "🍎"
        FoodCategory.PANTRY -> "🥫"
        FoodCategory.FROZEN -> "🧊"
        FoodCategory.BEVERAGES -> "🥤"
        FoodCategory.COOKED -> "🍲"
        FoodCategory.OTHER -> "🍽️"
    }

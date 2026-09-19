package com.foodario.inventory.presentation

import com.foodario.inventory.domain.model.FoodItem

data class FoodDetailUiState(
    val item: FoodItem? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

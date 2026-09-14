package com.foodario.inventory.presentation

import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem

data class InventoryUiState(
    val items: List<FoodItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: FoodCategory? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

package com.foodario.shoppinglist.presentation

import com.foodario.shoppinglist.domain.model.ShoppingItem

data class ShoppingListUiState(
    val items: List<ShoppingItem> = emptyList(),
    val checkedIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

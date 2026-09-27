package com.foodario.shoppinglist.presentation

import com.foodario.shoppinglist.domain.model.ShoppingItem

data class ShoppingListUiState(
    val items: List<ShoppingItem> = emptyList(),
    val pendingIds: Set<Long> = emptySet(),
    val checkedIds: Set<Long> = emptySet(),
    val showCheckedSection: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)

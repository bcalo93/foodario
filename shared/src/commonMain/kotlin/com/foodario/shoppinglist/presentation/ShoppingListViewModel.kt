package com.foodario.shoppinglist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.core.domain.usecase.UseCase
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.shoppinglist.domain.model.ShoppingItem
import com.foodario.shoppinglist.domain.usecase.AddToShoppingListParams
import com.foodario.shoppinglist.domain.usecase.MoveToInventoryParams
import com.foodario.shoppinglist.domain.usecase.ObserveShoppingListParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ShoppingListEvent {
    data class ToggleChecked(val itemId: Long) : ShoppingListEvent
    data class MoveToInventory(val itemId: Long) : ShoppingListEvent
    data class QuickAdd(val name: String) : ShoppingListEvent
    data class QuickAddCategorySelected(val category: FoodCategory) : ShoppingListEvent
}

class ShoppingListViewModel(
    private val observeShoppingList: ObserveUseCase<ObserveShoppingListParams, List<ShoppingItem>>,
    private val moveToInventory: UseCase<MoveToInventoryParams, FoodItem>,
    private val addToShoppingList: UseCase<AddToShoppingListParams, Unit>,
) : ViewModel() {

    private val checkedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val addCategory = MutableStateFlow(FoodCategory.OTHER)

    val quickAddCategory: StateFlow<FoodCategory> = addCategory.asStateFlow()

    val uiState: StateFlow<ShoppingListUiState> =
        combine(
            observeShoppingList(ObserveShoppingListParams)
                .map { items -> ShoppingListUiState(items = items, isLoading = false) }
                .onStart { emit(ShoppingListUiState(isLoading = true)) }
                .catch { e -> emit(ShoppingListUiState(isLoading = false, error = e.message)) },
            checkedIds,
        ) { state, checked -> state.copy(checkedIds = checked) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ShoppingListUiState(isLoading = true),
            )

    fun onEvent(event: ShoppingListEvent) {
        when (event) {
            is ShoppingListEvent.ToggleChecked -> checkedIds.update { current ->
                if (event.itemId in current) current - event.itemId else current + event.itemId
            }
            is ShoppingListEvent.MoveToInventory -> viewModelScope.launch {
                runCatching { moveToInventory(MoveToInventoryParams(event.itemId)) }
            }
            is ShoppingListEvent.QuickAddCategorySelected -> addCategory.value = event.category
            is ShoppingListEvent.QuickAdd -> viewModelScope.launch {
                runCatching {
                    addToShoppingList(
                        AddToShoppingListParams(
                            name = event.name,
                            category = addCategory.value,
                        )
                    )
                }
            }
        }
    }
}

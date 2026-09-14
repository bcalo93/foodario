package com.foodario.inventory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.usecase.ObserveInventoryParams
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

sealed interface InventoryEvent {
    data class SearchQueryChanged(val query: String) : InventoryEvent
    data class CategorySelected(val category: FoodCategory?) : InventoryEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModel(
    private val observeInventory: ObserveUseCase<ObserveInventoryParams, List<FoodItem>>,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<FoodCategory?>(null)

    val uiState: StateFlow<InventoryUiState> =
        combine(searchQuery, selectedCategory) { q, c -> q to c }
            .flatMapLatest { (q, c) ->
                observeInventory(ObserveInventoryParams(query = q, category = c))
                    .map { items ->
                        InventoryUiState(
                            items = items,
                            searchQuery = q,
                            selectedCategory = c,
                            isLoading = false,
                        )
                    }
                    .onStart {
                        emit(
                            InventoryUiState(
                                searchQuery = q,
                                selectedCategory = c,
                                isLoading = true,
                            )
                        )
                    }
                    .catch { e ->
                        emit(
                            InventoryUiState(
                                searchQuery = q,
                                selectedCategory = c,
                                isLoading = false,
                                error = e.message,
                            )
                        )
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = InventoryUiState(isLoading = true),
            )

    fun onEvent(event: InventoryEvent) {
        when (event) {
            is InventoryEvent.SearchQueryChanged -> searchQuery.value = event.query
            is InventoryEvent.CategorySelected -> selectedCategory.value = event.category
        }
    }
}

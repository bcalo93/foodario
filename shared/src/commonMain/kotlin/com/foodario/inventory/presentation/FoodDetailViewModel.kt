package com.foodario.inventory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.core.domain.usecase.UseCase
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.usecase.ConsumeFoodItemParams
import com.foodario.inventory.domain.usecase.DeleteFoodItemParams
import com.foodario.inventory.domain.usecase.ObserveFoodItemParams
import com.foodario.inventory.domain.usecase.ToggleFrozenParams
import com.foodario.inventory.domain.usecase.UpdateExpirationParams
import com.foodario.inventory.domain.usecase.UpdateQuantityParams
import com.foodario.shoppinglist.domain.usecase.AddToShoppingListParams
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

sealed interface FoodDetailEvent {
    data object IncrementQuantity : FoodDetailEvent
    data object DecrementQuantity : FoodDetailEvent
    data object Consume : FoodDetailEvent
    data object ToggleFrozen : FoodDetailEvent
    data class ExpirationDateChanged(val date: LocalDate?) : FoodDetailEvent
    data object Delete : FoodDetailEvent
    data object AddToShoppingList : FoodDetailEvent
}

class FoodDetailViewModel(
    private val itemId: Long,
    private val observeFoodItem: ObserveUseCase<ObserveFoodItemParams, FoodItem?>,
    private val updateQuantity: UseCase<UpdateQuantityParams, Unit>,
    private val consumeFoodItem: UseCase<ConsumeFoodItemParams, Unit>,
    private val toggleFrozen: UseCase<ToggleFrozenParams, Unit>,
    private val updateExpiration: UseCase<UpdateExpirationParams, Unit>,
    private val deleteFoodItem: UseCase<DeleteFoodItemParams, Unit>,
    private val addToShoppingList: UseCase<AddToShoppingListParams, Unit>,
) : ViewModel() {

    val uiState: StateFlow<FoodDetailUiState> =
        observeFoodItem(ObserveFoodItemParams(itemId))
            .map { item -> FoodDetailUiState(item = item, isLoading = false) }
            .onStart { emit(FoodDetailUiState(isLoading = true)) }
            .catch { e -> emit(FoodDetailUiState(isLoading = false, error = e.message)) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = FoodDetailUiState(isLoading = true),
            )

    fun onEvent(event: FoodDetailEvent) {
        when (event) {
            is FoodDetailEvent.IncrementQuantity -> mutateQuantity { it + 1.0 }
            is FoodDetailEvent.DecrementQuantity -> mutateQuantity { (it - 1.0).coerceAtLeast(0.0) }
            is FoodDetailEvent.Consume -> viewModelScope.launch {
                runCatching { consumeFoodItem(ConsumeFoodItemParams(itemId)) }
            }
            is FoodDetailEvent.ToggleFrozen -> viewModelScope.launch {
                runCatching { toggleFrozen(ToggleFrozenParams(itemId)) }
            }
            is FoodDetailEvent.ExpirationDateChanged -> viewModelScope.launch {
                runCatching { updateExpiration(UpdateExpirationParams(itemId, event.date)) }
            }
            is FoodDetailEvent.Delete -> viewModelScope.launch {
                runCatching { deleteFoodItem(DeleteFoodItemParams(itemId)) }
            }
            is FoodDetailEvent.AddToShoppingList -> viewModelScope.launch {
                val item = uiState.value.item ?: return@launch
                runCatching {
                    addToShoppingList(
                        AddToShoppingListParams(
                            name = item.name,
                            category = item.category,
                            quantity = item.quantity,
                            unit = item.unit,
                        )
                    )
                }
            }
        }
    }

    private fun mutateQuantity(transform: (Double) -> Double) {
        val item = uiState.value.item ?: return
        val newQuantity = transform(item.quantity)
        viewModelScope.launch {
            runCatching { updateQuantity(UpdateQuantityParams(itemId, newQuantity)) }
        }
    }
}

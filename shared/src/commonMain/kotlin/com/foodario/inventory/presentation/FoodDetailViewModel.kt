package com.foodario.inventory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.core.domain.usecase.UseCase
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.model.snapToStep
import com.foodario.inventory.domain.usecase.DeleteFoodItemParams
import com.foodario.inventory.domain.usecase.ObserveFoodItemParams
import com.foodario.inventory.domain.usecase.ToggleFrozenParams
import com.foodario.inventory.domain.usecase.UpdateCategoryParams
import com.foodario.inventory.domain.usecase.UpdateExpirationParams
import com.foodario.inventory.domain.usecase.UpdateQuantityParams
import com.foodario.inventory.domain.usecase.UpdateUnitParams
import com.foodario.shoppinglist.domain.usecase.AddToShoppingListParams
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

sealed interface FoodDetailEvent {
    data object IncrementQuantity : FoodDetailEvent
    data object DecrementQuantity : FoodDetailEvent
    data class QuantitySet(val quantity: Double) : FoodDetailEvent
    data object ToggleFrozen : FoodDetailEvent
    data class CategoryChanged(val category: FoodCategory) : FoodDetailEvent
    data class ExpirationDateChanged(val date: LocalDate?) : FoodDetailEvent
    data class UnitChanged(val unit: QuantityUnit) : FoodDetailEvent
    data object Delete : FoodDetailEvent
    data object AddToShoppingList : FoodDetailEvent
}

sealed interface FoodDetailEffect {
    data object AddedToShoppingList : FoodDetailEffect
    data object QuantityDepleted : FoodDetailEffect
    data object Deleted : FoodDetailEffect
}

class FoodDetailViewModel(
    private val itemId: Long,
    private val observeFoodItem: ObserveUseCase<ObserveFoodItemParams, FoodItem?>,
    private val updateQuantity: UseCase<UpdateQuantityParams, Unit>,
    private val updateCategory: UseCase<UpdateCategoryParams, Unit>,
    private val updateUnit: UseCase<UpdateUnitParams, Unit>,
    private val toggleFrozen: UseCase<ToggleFrozenParams, Unit>,
    private val updateExpiration: UseCase<UpdateExpirationParams, Unit>,
    private val deleteFoodItem: UseCase<DeleteFoodItemParams, Unit>,
    private val addToShoppingList: UseCase<AddToShoppingListParams, Unit>,
) : ViewModel() {

    private val effectChannel = Channel<FoodDetailEffect>(Channel.BUFFERED)

    val effects = effectChannel.receiveAsFlow()

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
            is FoodDetailEvent.IncrementQuantity -> mutateQuantity { current, unit ->
                snapToStep(current + unit.step, unit.step)
            }
            is FoodDetailEvent.DecrementQuantity -> mutateQuantity { current, unit ->
                snapToStep((current - unit.step).coerceAtLeast(0.0), unit.step)
            }
            is FoodDetailEvent.QuantitySet -> mutateQuantity { _, _ ->
                event.quantity.coerceAtLeast(0.0)
            }
            is FoodDetailEvent.ToggleFrozen -> viewModelScope.launch {
                runCatching { toggleFrozen(ToggleFrozenParams(itemId)) }
            }
            is FoodDetailEvent.CategoryChanged -> viewModelScope.launch {
                runCatching { updateCategory(UpdateCategoryParams(itemId, event.category)) }
            }
            is FoodDetailEvent.ExpirationDateChanged -> viewModelScope.launch {
                runCatching { updateExpiration(UpdateExpirationParams(itemId, event.date)) }
            }
            is FoodDetailEvent.UnitChanged -> viewModelScope.launch {
                runCatching { updateUnit(UpdateUnitParams(itemId, event.unit)) }
            }
            is FoodDetailEvent.Delete -> viewModelScope.launch {
                val result = runCatching { deleteFoodItem(DeleteFoodItemParams(itemId)) }
                if (result.isSuccess) {
                    effectChannel.send(FoodDetailEffect.Deleted)
                }
            }
            is FoodDetailEvent.AddToShoppingList -> viewModelScope.launch {
                val item = uiState.value.item ?: return@launch
                val result = runCatching {
                    addToShoppingList(
                        AddToShoppingListParams(
                            name = item.name,
                            category = item.category,
                            quantity = item.quantity.takeIf { it > 0.0 },
                            unit = item.unit.takeIf { item.quantity > 0.0 },
                        )
                    )
                }
                if (result.isSuccess) {
                    effectChannel.send(FoodDetailEffect.AddedToShoppingList)
                }
            }
        }
    }

    private fun mutateQuantity(transform: (quantity: Double, unit: QuantityUnit) -> Double) {
        val item = uiState.value.item ?: return
        val newQuantity = transform(item.quantity, item.unit).coerceAtLeast(0.0)
        viewModelScope.launch {
            val result = runCatching { updateQuantity(UpdateQuantityParams(itemId, newQuantity)) }
            if (result.isSuccess && newQuantity == 0.0) {
                effectChannel.send(FoodDetailEffect.QuantityDepleted)
            }
        }
    }
}

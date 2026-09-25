package com.foodario.inventory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.core.domain.usecase.UseCase
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.model.snapToStep
import com.foodario.inventory.domain.usecase.AddFoodItemParams
import com.foodario.inventory.domain.usecase.DeleteFoodItemParams
import com.foodario.inventory.domain.usecase.ObserveInventoryParams
import com.foodario.inventory.domain.usecase.UpdateQuantityParams
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface InventoryEvent {
    data class SearchQueryChanged(val query: String) : InventoryEvent
    data class CategorySelected(val category: FoodCategory?) : InventoryEvent
    data class QuickAdd(val name: String) : InventoryEvent
    data class QuickAddCategorySelected(val category: FoodCategory) : InventoryEvent
    data class IncreaseQuantity(
        val itemId: Long,
        val currentQuantity: Double,
        val unit: QuantityUnit,
    ) : InventoryEvent
    data class Delete(val itemId: Long) : InventoryEvent
}

sealed interface InventoryEffect {
    data class ItemAdded(
        val itemId: Long,
        val name: String,
        val category: FoodCategory,
    ) : InventoryEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModel(
    private val observeInventory: ObserveUseCase<ObserveInventoryParams, List<FoodItem>>,
    private val addFoodItem: UseCase<AddFoodItemParams, FoodItem>,
    private val updateQuantity: UseCase<UpdateQuantityParams, Unit>,
    private val deleteFoodItem: UseCase<DeleteFoodItemParams, Unit>,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<FoodCategory?>(null)
    private val addCategory = MutableStateFlow(FoodCategory.OTHER)

    private val effectChannel = Channel<InventoryEffect>(Channel.BUFFERED)

    val effects = effectChannel.receiveAsFlow()

    val quickAddCategory: StateFlow<FoodCategory> = addCategory.asStateFlow()

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
            is InventoryEvent.QuickAddCategorySelected -> addCategory.value = event.category
            is InventoryEvent.QuickAdd -> viewModelScope.launch {
                runCatching {
                    addFoodItem(AddFoodItemParams(name = event.name, category = addCategory.value))
                }.onSuccess { created ->
                    effectChannel.send(
                        InventoryEffect.ItemAdded(
                            itemId = created.id,
                            name = created.name,
                            category = created.category,
                        )
                    )
                }
            }
            is InventoryEvent.IncreaseQuantity -> viewModelScope.launch {
                runCatching {
                    updateQuantity(
                        UpdateQuantityParams(
                            itemId = event.itemId,
                            newQuantity = snapToStep(
                                event.currentQuantity + event.unit.step,
                                event.unit.step,
                            ),
                        )
                    )
                }
            }
            is InventoryEvent.Delete -> viewModelScope.launch {
                runCatching { deleteFoodItem(DeleteFoodItemParams(event.itemId)) }
            }
        }
    }
}

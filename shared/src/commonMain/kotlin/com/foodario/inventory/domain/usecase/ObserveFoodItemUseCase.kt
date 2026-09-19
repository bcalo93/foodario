package com.foodario.inventory.domain.usecase

import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow

data class ObserveFoodItemParams(
    val itemId: Long,
)

class ObserveFoodItemUseCase(
    private val repository: InventoryRepository,
) : ObserveUseCase<ObserveFoodItemParams, FoodItem?> {
    override fun invoke(params: ObserveFoodItemParams): Flow<FoodItem?> =
        repository.observeById(params.itemId)
}

package com.foodario.inventory.domain.usecase

import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow

data class ObserveInventoryParams(
    val query: String = "",
)

class ObserveInventoryUseCase(
    private val repository: InventoryRepository,
) : ObserveUseCase<ObserveInventoryParams, List<FoodItem>> {
    override fun invoke(params: ObserveInventoryParams): Flow<List<FoodItem>> =
        repository.observeInventory(params.query)
}

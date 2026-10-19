package com.foodario.inventory.domain.usecase

import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlinx.datetime.LocalDate

object ObserveExpiringItemsParams

class ObserveExpiringItemsUseCase(
    private val repository: InventoryRepository,
    private val todayProvider: () -> LocalDate = {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    },
) : ObserveUseCase<ObserveExpiringItemsParams, List<FoodItem>> {
    override fun invoke(params: ObserveExpiringItemsParams): Flow<List<FoodItem>> =
        repository.observeExpiringFrom(fromDate = todayProvider())
}

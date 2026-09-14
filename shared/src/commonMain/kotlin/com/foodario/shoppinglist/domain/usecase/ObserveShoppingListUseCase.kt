package com.foodario.shoppinglist.domain.usecase

import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.shoppinglist.domain.model.ShoppingItem
import com.foodario.shoppinglist.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.Flow

object ObserveShoppingListParams

class ObserveShoppingListUseCase(
    private val repository: ShoppingListRepository,
) : ObserveUseCase<ObserveShoppingListParams, List<ShoppingItem>> {
    override fun invoke(params: ObserveShoppingListParams): Flow<List<ShoppingItem>> =
        repository.observeShoppingList()
}

package com.foodario.shoppinglist.domain.repository

import com.foodario.shoppinglist.domain.model.ShoppingItem
import kotlinx.coroutines.flow.Flow

interface ShoppingListRepository {
    suspend fun add(item: ShoppingItem)
    suspend fun remove(id: Long)
    suspend fun getById(id: Long): ShoppingItem?
    fun observeShoppingList(): Flow<List<ShoppingItem>>
}

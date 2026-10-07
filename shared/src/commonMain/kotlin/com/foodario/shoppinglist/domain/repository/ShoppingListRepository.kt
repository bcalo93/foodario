package com.foodario.shoppinglist.domain.repository

import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.shoppinglist.domain.model.ShoppingItem
import kotlinx.coroutines.flow.Flow

interface ShoppingListRepository {
    suspend fun add(item: ShoppingItem)
    suspend fun remove(id: Long)
    suspend fun updateQuantity(id: Long, quantity: Double, unit: QuantityUnit)
    suspend fun getById(id: Long): ShoppingItem?
    fun observeShoppingList(): Flow<List<ShoppingItem>>
}

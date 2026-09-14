package com.foodario.inventory.domain.repository

import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    suspend fun add(item: FoodItem): FoodItem
    suspend fun updateQuantity(id: Long, quantity: Double)
    suspend fun setFrozen(id: Long, isFrozen: Boolean)
    suspend fun delete(id: Long)
    suspend fun getById(id: Long): FoodItem?
    fun observeInventory(query: String, category: FoodCategory?): Flow<List<FoodItem>>
}

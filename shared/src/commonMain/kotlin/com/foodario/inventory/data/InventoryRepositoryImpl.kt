package com.foodario.inventory.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.foodario.database.FoodItemQueries
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.repository.InventoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class InventoryRepositoryImpl(
    private val queries: FoodItemQueries,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : InventoryRepository {

    override suspend fun add(item: FoodItem): FoodItem = withContext(ioDispatcher) {
        val entity = item.toEntity()
        val id = queries.insertFoodItem(
            name = entity.name,
            category = entity.category,
            quantity = entity.quantity,
            unit = entity.unit,
            isFrozen = entity.is_frozen,
            expirationDate = entity.expiration_date,
            createdAt = entity.created_at,
            updatedAt = entity.updated_at,
        ).executeAsOne()
        item.copy(id = id)
    }

    override suspend fun updateQuantity(id: Long, quantity: Double) {
        withContext(ioDispatcher) {
            queries.updateQuantity(
                quantity = quantity,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
                id = id,
            )
        }
    }

    override suspend fun setFrozen(id: Long, isFrozen: Boolean) {
        withContext(ioDispatcher) {
            queries.updateFrozen(
                isFrozen = if (isFrozen) 1L else 0L,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
                id = id,
            )
        }
    }

    override suspend fun delete(id: Long) {
        withContext(ioDispatcher) {
            queries.delete(id = id)
        }
    }

    override suspend fun getById(id: Long): FoodItem? = withContext(ioDispatcher) {
        queries.selectById(id = id).executeAsOneOrNull()?.toDomain()
    }

    override fun observeInventory(query: String, category: FoodCategory?): Flow<List<FoodItem>> {
        val flow = if (query.isBlank()) {
            queries.selectAll().asFlow()
        } else {
            queries.search(query = query).asFlow()
        }
        return flow
            .mapToList(ioDispatcher)
            .map { entities -> entities.map { it.toDomain() }.filterByCategory(category) }
    }

    private fun List<FoodItem>.filterByCategory(category: FoodCategory?): List<FoodItem> =
        if (category == null) this else filter { it.category == category }
}

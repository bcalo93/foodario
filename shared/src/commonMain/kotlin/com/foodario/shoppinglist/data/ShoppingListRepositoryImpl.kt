package com.foodario.shoppinglist.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.foodario.database.ShoppingItemQueries
import com.foodario.shoppinglist.domain.model.ShoppingItem
import com.foodario.shoppinglist.domain.repository.ShoppingListRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ShoppingListRepositoryImpl(
    private val queries: ShoppingItemQueries,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ShoppingListRepository {

    override suspend fun add(item: ShoppingItem) {
        withContext(ioDispatcher) {
            val entity = item.toEntity()
            queries.insert(
                name = entity.name,
                category = entity.category,
                quantity = entity.quantity,
                unit = entity.unit,
                createdAt = entity.created_at,
            )
        }
    }

    override suspend fun remove(id: Long) {
        withContext(ioDispatcher) {
            queries.delete(id = id)
        }
    }

    override suspend fun getById(id: Long): ShoppingItem? = withContext(ioDispatcher) {
        queries.selectById(id = id).executeAsOneOrNull()?.toDomain()
    }

    override fun observeShoppingList(): Flow<List<ShoppingItem>> =
        queries.selectAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { entities -> entities.map { it.toDomain() } }
}

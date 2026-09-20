package com.foodario.shoppinglist.domain.usecase

import com.foodario.core.domain.usecase.UseCase
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.repository.InventoryRepository
import com.foodario.shoppinglist.domain.model.ShoppingItem
import com.foodario.shoppinglist.domain.repository.ShoppingListRepository
import kotlin.time.Clock

data class AddToShoppingListParams(
    val name: String,
    val category: FoodCategory? = null,
    val quantity: Double? = null,
    val unit: QuantityUnit? = null,
)

class AddToShoppingListUseCase(
    private val repository: ShoppingListRepository,
) : UseCase<AddToShoppingListParams, Unit> {
    override suspend fun invoke(params: AddToShoppingListParams) {
        require(params.name.isNotBlank()) { "El nombre no puede estar vacío" }
        repository.add(
            ShoppingItem(
                id = 0L,
                name = params.name,
                category = params.category,
                quantity = params.quantity,
                unit = params.unit,
                createdAt = Clock.System.now(),
            )
        )
    }
}

data class RemoveFromShoppingListParams(
    val itemId: Long,
)

class RemoveFromShoppingListUseCase(
    private val repository: ShoppingListRepository,
) : UseCase<RemoveFromShoppingListParams, Unit> {
    override suspend fun invoke(params: RemoveFromShoppingListParams) {
        repository.remove(params.itemId)
    }
}

data class MoveToInventoryParams(
    val shoppingItemId: Long,
)

class MoveToInventoryUseCase(
    private val shoppingListRepository: ShoppingListRepository,
    private val inventoryRepository: InventoryRepository,
) : UseCase<MoveToInventoryParams, FoodItem> {
    override suspend fun invoke(params: MoveToInventoryParams): FoodItem {
        val shoppingItem = requireNotNull(shoppingListRepository.getById(params.shoppingItemId)) {
            "El ítem de la lista de compras no existe"
        }
        val quantityToAdd = shoppingItem.quantity ?: 1.0
        val now = Clock.System.now()
        val existingItem = inventoryRepository.findByName(shoppingItem.name)
        val foodItem = if (existingItem == null) {
            inventoryRepository.add(
                FoodItem(
                    id = 0L,
                    name = shoppingItem.name,
                    category = shoppingItem.category ?: FoodCategory.OTHER,
                    quantity = quantityToAdd,
                    unit = shoppingItem.unit ?: QuantityUnit.UNIT,
                    isFrozen = false,
                    expirationDate = null,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        } else {
            val newQuantity = existingItem.quantity + quantityToAdd
            inventoryRepository.updateQuantity(existingItem.id, newQuantity)
            existingItem.copy(quantity = newQuantity, updatedAt = now)
        }
        shoppingListRepository.remove(params.shoppingItemId)
        return foodItem
    }
}

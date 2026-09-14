package com.foodario.inventory.domain.usecase

import com.foodario.core.domain.usecase.UseCase
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.repository.InventoryRepository
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

data class AddFoodItemParams(
    val name: String,
    val category: FoodCategory,
    val quantity: Double = 1.0,
    val unit: QuantityUnit = QuantityUnit.UNIT,
    val isFrozen: Boolean = false,
    val expirationDate: LocalDate? = null,
)

class AddFoodItemUseCase(
    private val repository: InventoryRepository,
) : UseCase<AddFoodItemParams, FoodItem> {
    override suspend fun invoke(params: AddFoodItemParams): FoodItem {
        require(params.name.isNotBlank()) { "El nombre no puede estar vacío" }
        require(params.quantity > 0) { "La cantidad debe ser positiva" }
        return repository.add(params.toFoodItem())
    }
}

data class UpdateQuantityParams(
    val itemId: Long,
    val newQuantity: Double,
)

class UpdateQuantityUseCase(
    private val repository: InventoryRepository,
) : UseCase<UpdateQuantityParams, Unit> {
    override suspend fun invoke(params: UpdateQuantityParams) {
        require(params.newQuantity >= 0) { "La cantidad no puede ser negativa" }
        repository.updateQuantity(params.itemId, params.newQuantity)
    }
}

data class ConsumeFoodItemParams(
    val itemId: Long,
    val amount: Double = 1.0,
)

class ConsumeFoodItemUseCase(
    private val repository: InventoryRepository,
) : UseCase<ConsumeFoodItemParams, Unit> {
    override suspend fun invoke(params: ConsumeFoodItemParams) {
        require(params.amount > 0) { "La cantidad a consumir debe ser positiva" }
        val item = requireNotNull(repository.getById(params.itemId)) { "El alimento no existe" }
        val newQuantity = item.quantity - params.amount
        require(newQuantity >= 0) { "No hay suficiente cantidad para consumir" }
        repository.updateQuantity(item.id, newQuantity)
    }
}

data class DeleteFoodItemParams(
    val itemId: Long,
)

class DeleteFoodItemUseCase(
    private val repository: InventoryRepository,
) : UseCase<DeleteFoodItemParams, Unit> {
    override suspend fun invoke(params: DeleteFoodItemParams) {
        repository.delete(params.itemId)
    }
}

data class ToggleFrozenParams(
    val itemId: Long,
)

class ToggleFrozenUseCase(
    private val repository: InventoryRepository,
) : UseCase<ToggleFrozenParams, Unit> {
    override suspend fun invoke(params: ToggleFrozenParams) {
        val item = requireNotNull(repository.getById(params.itemId)) { "El alimento no existe" }
        repository.setFrozen(item.id, !item.isFrozen)
    }
}

private fun AddFoodItemParams.toFoodItem(): FoodItem {
    val now = Clock.System.now()
    return FoodItem(
        id = 0L,
        name = name,
        category = category,
        quantity = quantity,
        unit = unit,
        isFrozen = isFrozen,
        expirationDate = expirationDate,
        createdAt = now,
        updatedAt = now,
    )
}

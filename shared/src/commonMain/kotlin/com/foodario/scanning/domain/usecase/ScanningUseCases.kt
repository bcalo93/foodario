package com.foodario.scanning.domain.usecase

import com.foodario.core.domain.matching.MatchConfidence
import com.foodario.core.domain.matching.ProductMatch
import com.foodario.core.domain.matching.ProductNameMatcher
import com.foodario.core.domain.usecase.UseCase
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.repository.InventoryRepository
import com.foodario.scanning.domain.model.ScannedReceiptItem
import com.foodario.scanning.domain.service.ReceiptParser
import kotlin.time.Clock

data class ParseReceiptParams(val rawText: String)

class ParseReceiptUseCase : UseCase<ParseReceiptParams, List<ScannedReceiptItem>> {
    override suspend fun invoke(params: ParseReceiptParams): List<ScannedReceiptItem> =
        ReceiptParser.parse(params.rawText)
}

data class MatchProductsParams(val names: List<String>)

class MatchProductsUseCase(
    private val repository: InventoryRepository,
) : UseCase<MatchProductsParams, List<ProductMatch?>> {
    override suspend fun invoke(params: MatchProductsParams): List<ProductMatch?> {
        val inventory = repository.getAll()
        return params.names.map { ProductNameMatcher.findBestMatch(it, inventory) }
    }
}

data class AddFoodItemsParams(val items: List<ImportedItem>) {
    data class ImportedItem(
        val name: String,
        val category: FoodCategory,
        val quantity: Double,
        val unit: QuantityUnit,
        val mergeIntoItemId: Long? = null,
    )
}

class AddFoodItemsUseCase(
    private val repository: InventoryRepository,
) : UseCase<AddFoodItemsParams, List<FoodItem>> {
    override suspend fun invoke(params: AddFoodItemsParams): List<FoodItem> {
        require(params.items.isNotEmpty()) { "La lista no puede estar vacía" }
        params.items.forEach {
            require(it.name.isNotBlank()) { "El nombre no puede estar vacío" }
            require(it.quantity > 0) { "La cantidad debe ser positiva" }
        }
        val snapshot = repository.getAll().toMutableList()
        val results = mutableListOf<FoodItem>()
        for (item in params.items) {
            val explicit = item.mergeIntoItemId
                ?.let { id -> snapshot.firstOrNull { it.id == id } }
                ?.takeIf { it.unit == item.unit }
            val auto = ProductNameMatcher.findBestMatch(item.name, snapshot)
                ?.takeIf { it.confidence == MatchConfidence.EXACT && it.item.unit == item.unit }
                ?.item
            val target = explicit ?: auto
            if (target != null) {
                val newQuantity = target.quantity + item.quantity
                repository.updateQuantity(target.id, newQuantity)
                val updated = target.copy(quantity = newQuantity, updatedAt = Clock.System.now())
                val index = snapshot.indexOfFirst { it.id == target.id }
                if (index >= 0) snapshot[index] = updated
                results += updated
            } else {
                val created = repository.add(
                    FoodItem(
                        id = 0L,
                        name = item.name.trim(),
                        category = item.category,
                        quantity = item.quantity,
                        unit = item.unit,
                        isFrozen = false,
                        expirationDate = null,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    ),
                )
                snapshot += created
                results += created
            }
        }
        return results
    }
}

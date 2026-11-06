package com.foodario.scanning.presentation

import com.foodario.core.domain.matching.ProductMatch
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.QuantityUnit

enum class ReceiptScanStep { Capturing, Reviewing }

data class ScannedReviewItem(
    val key: String,
    val name: String,
    val category: FoodCategory,
    val quantity: Double,
    val unit: QuantityUnit,
    val included: Boolean = true,
    val match: ProductMatch? = null,
    val mergeIntoItemId: Long? = null,
)

data class ReceiptScanUiState(
    val step: ReceiptScanStep = ReceiptScanStep.Capturing,
    val isProcessing: Boolean = false,
    val items: List<ScannedReviewItem> = emptyList(),
    val error: String? = null,
) {
    val includedCount: Int get() = items.count { it.included }
}

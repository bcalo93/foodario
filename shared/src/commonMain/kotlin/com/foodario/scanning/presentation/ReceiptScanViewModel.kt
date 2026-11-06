package com.foodario.scanning.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodario.core.domain.matching.MatchConfidence
import com.foodario.core.domain.matching.ProductMatch
import com.foodario.core.domain.usecase.UseCase
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.scanning.domain.model.ScannedReceiptItem
import com.foodario.scanning.domain.service.ReceiptTextRecognizer
import com.foodario.scanning.domain.usecase.AddFoodItemsParams
import com.foodario.scanning.domain.usecase.MatchProductsParams
import com.foodario.scanning.domain.usecase.ParseReceiptParams
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ReceiptScanEvent {
    data class PhotoCaptured(val imageBytes: ByteArray) : ReceiptScanEvent
    data object PhotoCaptureFailed : ReceiptScanEvent
    data object RetakePhoto : ReceiptScanEvent
    data class ToggleItemIncluded(val key: String) : ReceiptScanEvent
    data class EditItemName(val key: String, val name: String) : ReceiptScanEvent
    data class EditItemQuantity(val key: String, val quantity: Double) : ReceiptScanEvent
    data class EditItemUnit(val key: String, val unit: QuantityUnit) : ReceiptScanEvent
    data class EditItemCategory(val key: String, val category: FoodCategory) : ReceiptScanEvent
    data class ToggleMerge(val key: String) : ReceiptScanEvent
    data object ConfirmImport : ReceiptScanEvent
}

sealed interface ReceiptScanEffect {
    data class Imported(val count: Int) : ReceiptScanEffect
}

class ReceiptScanViewModel(
    private val receiptTextRecognizer: ReceiptTextRecognizer,
    private val parseReceipt: UseCase<ParseReceiptParams, List<ScannedReceiptItem>>,
    private val matchProducts: UseCase<MatchProductsParams, List<ProductMatch?>>,
    private val addFoodItems: UseCase<AddFoodItemsParams, List<FoodItem>>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptScanUiState())
    val uiState: StateFlow<ReceiptScanUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<ReceiptScanEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    private var nextKey = 0
    private fun newKey() = "scan-${nextKey++}"

    fun onEvent(event: ReceiptScanEvent) {
        when (event) {
            is ReceiptScanEvent.PhotoCaptured -> onPhotoCaptured(event.imageBytes)
            ReceiptScanEvent.PhotoCaptureFailed ->
                _uiState.update { it.copy(error = "No se pudo capturar la foto. Probá de nuevo.") }
            ReceiptScanEvent.RetakePhoto -> _uiState.value = ReceiptScanUiState()
            is ReceiptScanEvent.ToggleItemIncluded -> toggleIncluded(event.key)
            is ReceiptScanEvent.EditItemName -> editName(event.key, event.name)
            is ReceiptScanEvent.EditItemQuantity -> editQuantity(event.key, event.quantity)
            is ReceiptScanEvent.EditItemUnit -> editUnit(event.key, event.unit)
            is ReceiptScanEvent.EditItemCategory -> editCategory(event.key, event.category)
            is ReceiptScanEvent.ToggleMerge -> toggleMerge(event.key)
            ReceiptScanEvent.ConfirmImport -> confirmImport()
        }
    }

    private fun onPhotoCaptured(imageBytes: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            val parsed = runCatching {
                val text = receiptTextRecognizer.recognizeText(imageBytes)
                parseReceipt(ParseReceiptParams(text))
            }
            val items = parsed.getOrNull()
            if (items == null || items.isEmpty()) {
                _uiState.value = ReceiptScanUiState(
                    step = ReceiptScanStep.Capturing,
                    isProcessing = false,
                    error = "No detectamos productos. Probá con mejor luz y más cerca.",
                )
                return@launch
            }
            val matches = runCatching { matchProducts(MatchProductsParams(items.map { it.name })) }
                .getOrDefault(items.map { null })
            val reviewItems = items.mapIndexed { index, item ->
                val match = matches.getOrNull(index)
                ScannedReviewItem(
                    key = newKey(),
                    name = item.name,
                    category = item.category,
                    quantity = item.quantity,
                    unit = item.unit,
                    included = true,
                    match = match,
                    mergeIntoItemId = defaultMergeId(match, item.unit),
                )
            }
            _uiState.value = ReceiptScanUiState(
                step = ReceiptScanStep.Reviewing,
                isProcessing = false,
                items = reviewItems,
            )
        }
    }

    private fun toggleIncluded(key: String) = updateItem(key) { it.copy(included = !it.included) }

    private fun editName(key: String, name: String) {
        updateItem(key) { it.copy(name = name) }
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val match = runCatching { matchProducts(MatchProductsParams(listOf(trimmed))) }
                .getOrNull()?.firstOrNull()
            updateItem(key) { it.copy(match = match, mergeIntoItemId = defaultMergeId(match, it.unit)) }
        }
    }

    private fun editQuantity(key: String, quantity: Double) = updateItem(key) { it.copy(quantity = quantity) }

    private fun editUnit(key: String, unit: QuantityUnit) =
        updateItem(key) { it.copy(unit = unit, mergeIntoItemId = defaultMergeId(it.match, unit)) }

    private fun editCategory(key: String, category: FoodCategory) = updateItem(key) { it.copy(category = category) }

    private fun toggleMerge(key: String) = updateItem(key) { item ->
        val match = item.match ?: return@updateItem item
        if (match.item.unit != item.unit) return@updateItem item
        item.copy(mergeIntoItemId = if (item.mergeIntoItemId == null) match.item.id else null)
    }

    private fun confirmImport() {
        val included = _uiState.value.items.filter { it.included }
        if (included.isEmpty()) {
            _uiState.update { it.copy(error = "Seleccioná al menos un producto") }
            return
        }
        viewModelScope.launch {
            runCatching {
                addFoodItems(AddFoodItemsParams(included.map { it.toImportedItem() }))
            }.onSuccess { created ->
                effectChannel.send(ReceiptScanEffect.Imported(created.size))
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "No se pudieron agregar los productos") }
            }
        }
    }

    private fun updateItem(key: String, transform: (ScannedReviewItem) -> ScannedReviewItem) {
        _uiState.update { state ->
            state.copy(items = state.items.map { item -> if (item.key == key) transform(item) else item })
        }
    }

    private fun defaultMergeId(match: ProductMatch?, unit: QuantityUnit): Long? =
        match?.takeIf { it.confidence == MatchConfidence.EXACT && it.item.unit == unit }?.item?.id

    private fun ScannedReviewItem.toImportedItem() = AddFoodItemsParams.ImportedItem(
        name = name,
        category = category,
        quantity = quantity,
        unit = unit,
        mergeIntoItemId = mergeIntoItemId,
    )
}

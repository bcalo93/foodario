package com.foodario.inventory.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.core.domain.usecase.UseCase
import com.foodario.core.presentation.components.DoodleDivider
import com.foodario.core.presentation.components.FridgeIllustration
import com.foodario.core.presentation.components.NotebookListItem
import com.foodario.core.presentation.components.QuickAddBar
import com.foodario.core.presentation.components.emoji
import com.foodario.core.presentation.components.notebookMargin
import com.foodario.core.presentation.preview.previewUnitUseCase
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.core.presentation.theme.categoryColor
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.usecase.AddFoodItemParams
import com.foodario.inventory.domain.usecase.ObserveInventoryParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Instant
import kotlin.math.roundToInt
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InventoryScreen(
    onItemClick: (Long) -> Unit = {},
    viewModel: InventoryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val quickAddCategory by viewModel.quickAddCategory.collectAsStateWithLifecycle()
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
        ) {
            Text(
                text = "Mi heladera",
                style = typography.displayHand,
                color = colors.ink,
            )
            Spacer(Modifier.height(12.dp))
            SearchField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onEvent(InventoryEvent.SearchQueryChanged(it)) },
            )
            Spacer(Modifier.height(12.dp))
            CategoryFilters(
                selected = uiState.selectedCategory,
                onSelect = { category -> viewModel.onEvent(InventoryEvent.CategorySelected(category)) },
            )
            Spacer(Modifier.height(8.dp))
            DoodleDivider()
            Spacer(Modifier.height(8.dp))
            QuickAddBar(
                selectedCategory = quickAddCategory,
                onCategorySelected = { viewModel.onEvent(InventoryEvent.QuickAddCategorySelected(it)) },
                onAdd = { viewModel.onEvent(InventoryEvent.QuickAdd(it)) },
            )
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .notebookMargin(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = colors.penBlue)
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .notebookMargin()
                        .padding(end = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.error.orEmpty(),
                        style = typography.body,
                        color = colors.marginRed,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            uiState.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .notebookMargin()
                        .padding(end = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FridgeIllustration(
                            modifier = Modifier.size(96.dp),
                            contentDescription = "Ilustración de una heladera",
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Tu heladera está vacía… ¡empezá a anotar!",
                            style = typography.titleHand,
                            color = colors.inkSoft,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .notebookMargin(),
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        SwipeableInventoryItem(
                            item = item,
                            onClick = { onItemClick(item.id) },
                            onIncreaseQuantity = {
                                viewModel.onEvent(
                                    InventoryEvent.IncreaseQuantity(
                                        itemId = item.id,
                                        currentQuantity = item.quantity,
                                    )
                                )
                            },
                            onDelete = { viewModel.onEvent(InventoryEvent.Delete(item.id)) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun SwipeableInventoryItem(
    item: FoodItem,
    onClick: () -> Unit,
    onIncreaseQuantity: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography
    val density = LocalDensity.current
    val actionWidth = with(density) { 96.dp.toPx() }
    val swipeThreshold = with(density) { 64.dp.toPx() }
    var offsetX by remember(item.id) { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.paper),
    ) {
        if (offsetX != 0f) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        if (offsetX < 0f) {
                            colors.marginRed.copy(alpha = 0.12f)
                        } else {
                            colors.penBlue.copy(alpha = 0.12f)
                        }
                    )
                    .clickable {
                        val isDeleteAction = offsetX < 0f
                        offsetX = 0f
                        if (isDeleteAction) onDelete() else onIncreaseQuantity()
                    }
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (offsetX < 0f) {
                    Arrangement.End
                } else {
                    Arrangement.Start
                },
            ) {
                Text(
                    text = if (offsetX < 0f) "Eliminar" else "+1",
                    style = typography.labelHand,
                    color = if (offsetX < 0f) colors.marginRed else colors.penBlue,
                )
            }
        }

        NotebookListItem(
            name = item.name,
            category = item.category,
            quantity = item.quantity,
            unit = item.unit,
            isFrozen = item.isFrozen,
            onClick = { if (offsetX == 0f) onClick() else offsetX = 0f },
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .background(colors.paper)
                .draggable(
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(-actionWidth, actionWidth)
                    },
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        val swipe = offsetX
                        if (swipe >= actionWidth) {
                            offsetX = 0f
                            onIncreaseQuantity()
                        } else {
                            offsetX = when {
                                swipe <= -swipeThreshold -> -actionWidth
                                swipe >= swipeThreshold -> actionWidth
                                else -> 0f
                            }
                        }
                    },
                ),
        )
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .drawBehind {
                val y = size.height - 2.dp.toPx()
                drawLine(
                    color = colors.pencilGray,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(5.dp.toPx(), 5.dp.toPx()),
                        0f,
                    ),
                )
            }
            .padding(vertical = 12.dp),
        textStyle = typography.body.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.penBlue),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = "Buscar alimento…",
                        style = typography.body,
                        color = colors.inkSoft,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun CategoryFilters(
    selected: FoodCategory?,
    onSelect: (FoodCategory?) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FoodCategory.entries.forEach { category ->
            val isSelected = category == selected
            FilterChip(
                category = category,
                selected = isSelected,
                onClick = { onSelect(if (isSelected) null else category) },
            )
        }
    }
}

@Composable
private fun FilterChip(
    category: FoodCategory,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography
    val borderColor = if (selected) colors.penBlue else colors.pencilGray
    val shape = RoundedCornerShape(50)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(shape)
            .background(categoryColor(category))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text = category.emoji, fontSize = 16.sp)
        Text(
            text = category.displayName,
            style = typography.labelHand,
            color = colors.ink,
        )
    }
}

private fun fakeObserveInventoryUseCase(): ObserveUseCase<ObserveInventoryParams, List<FoodItem>> =
    object : ObserveUseCase<ObserveInventoryParams, List<FoodItem>> {
        override fun invoke(params: ObserveInventoryParams): Flow<List<FoodItem>> =
            flowOf(
                listOf(
                    sampleItem(1L, "Leche", FoodCategory.DAIRY, 2.0, QuantityUnit.UNIT, isFrozen = false),
                    sampleItem(2L, "Pechuga", FoodCategory.MEAT, 300.0, QuantityUnit.GRAMS, isFrozen = true),
                    sampleItem(3L, "Manzanas", FoodCategory.FRUITS, 1.0, QuantityUnit.KILOGRAMS, isFrozen = false),
                )
            )
    }

private fun sampleItem(
    id: Long,
    name: String,
    category: FoodCategory,
    quantity: Double,
    unit: QuantityUnit,
    isFrozen: Boolean,
) = FoodItem(
    id = id,
    name = name,
    category = category,
    quantity = quantity,
    unit = unit,
    isFrozen = isFrozen,
    expirationDate = null,
    createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
)

private fun fakeAddFoodItemUseCase(): UseCase<AddFoodItemParams, FoodItem> =
    object : UseCase<AddFoodItemParams, FoodItem> {
        override suspend fun invoke(params: AddFoodItemParams): FoodItem =
            sampleItem(
                id = 0L,
                name = params.name,
                category = params.category,
                quantity = params.quantity,
                unit = params.unit,
                isFrozen = params.isFrozen,
            )
    }

@Preview
@Composable
private fun InventoryScreenLightPreview() {
    FoodarioTheme(darkTheme = false) {
        InventoryScreen(
            viewModel = InventoryViewModel(
                observeInventory = fakeObserveInventoryUseCase(),
                addFoodItem = fakeAddFoodItemUseCase(),
                updateQuantity = previewUnitUseCase(),
                deleteFoodItem = previewUnitUseCase(),
            )
        )
    }
}

@Preview
@Composable
private fun InventoryScreenDarkPreview() {
    FoodarioTheme(darkTheme = true) {
        InventoryScreen(
            viewModel = InventoryViewModel(
                observeInventory = fakeObserveInventoryUseCase(),
                addFoodItem = fakeAddFoodItemUseCase(),
                updateQuantity = previewUnitUseCase(),
                deleteFoodItem = previewUnitUseCase(),
            )
        )
    }
}

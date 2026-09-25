package com.foodario.shoppinglist.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foodario.core.domain.usecase.UseCase
import com.foodario.core.presentation.components.DoodleDivider
import com.foodario.core.presentation.components.FridgeIllustration
import com.foodario.core.presentation.components.HandDrawnCheckbox
import com.foodario.core.presentation.components.QuickAddBar
import com.foodario.core.presentation.components.emoji
import com.foodario.core.presentation.components.formatQuantity
import com.foodario.core.presentation.components.notebookMargin
import com.foodario.core.presentation.preview.previewObserveUseCase
import com.foodario.core.presentation.preview.previewUnitUseCase
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.shoppinglist.domain.model.ShoppingItem
import com.foodario.shoppinglist.domain.usecase.AddToShoppingListParams
import com.foodario.shoppinglist.domain.usecase.MoveToInventoryParams
import kotlin.math.roundToInt
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ShoppingListScreen(
    viewModel: ShoppingListViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val quickAddCategory by viewModel.quickAddCategory.collectAsStateWithLifecycle()
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .imePadding(),
    ) {
        Text(
            text = "Compras",
            style = typography.displayHand,
            color = colors.ink,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FoodarioTheme.dimensions.lg)
                .padding(top = FoodarioTheme.dimensions.lg),
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .notebookMargin(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = colors.penBlue)
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .notebookMargin()
                        .padding(end = FoodarioTheme.dimensions.lg),
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
                        .weight(1f)
                        .fillMaxWidth()
                        .notebookMargin()
                        .padding(end = FoodarioTheme.dimensions.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🛒", fontSize = 48.sp)
                        Spacer(Modifier.height(FoodarioTheme.dimensions.md))
                        Text(
                            text = "Nada para comprar. Tachá todo ✓",
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
                        .weight(1f)
                        .fillMaxWidth()
                        .notebookMargin(),
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        ShoppingListRow(
                            item = item,
                            checked = item.id in uiState.checkedIds,
                            onEvent = viewModel::onEvent,
                        )
                    }
                }
            }
        }

        DoodleDivider()
        QuickAddBar(
            selectedCategory = quickAddCategory,
            onCategorySelected = { viewModel.onEvent(ShoppingListEvent.QuickAddCategorySelected(it)) },
            onAdd = { viewModel.onEvent(ShoppingListEvent.QuickAdd(it)) },
            placeholder = "Escribí para comprar…",
            addContentDescription = "Agregar a compras",
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.paper)
                .padding(horizontal = FoodarioTheme.dimensions.lg)
                .padding(bottom = FoodarioTheme.dimensions.sm),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShoppingListRow(
    item: ShoppingItem,
    checked: Boolean,
    onEvent: (ShoppingListEvent) -> Unit,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography
    val density = LocalDensity.current
    val actionWidth = with(density) { 96.dp.toPx() }
    val swipeThreshold = with(density) { 64.dp.toPx() }
    var offsetX by remember(item.id) { mutableStateOf(0f) }
    val visibilityState = remember(item.id) { MutableTransitionState(true) }

    LaunchedEffect(visibilityState) {
        snapshotFlow { visibilityState.isIdle && !visibilityState.currentState }
            .first { it }
        onEvent(ShoppingListEvent.RemoveFromShoppingList(item.id))
    }

    val requestRemoveItem = {
        if (visibilityState.targetState) {
            offsetX = 0f
            visibilityState.targetState = false
        }
    }

    AnimatedVisibility(
        visibleState = visibilityState,
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = 280),
            shrinkTowards = Alignment.Top,
        ) + fadeOut(animationSpec = tween(durationMillis = 180)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.paper),
        ) {
            if (offsetX != 0f) {
                Row(
                    modifier = Modifier
                        .matchParentSize()
                        .background(colors.marginRed.copy(alpha = 0.12f))
                        .clickable(role = Role.Button, onClick = requestRemoveItem)
                        .padding(horizontal = FoodarioTheme.dimensions.xl),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = "Eliminar",
                        style = typography.labelHand,
                        color = colors.marginRed,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .background(colors.paper)
                    .semantics {
                        customActions = listOf(
                            CustomAccessibilityAction(label = "Eliminar ${item.name}") {
                                requestRemoveItem()
                                true
                            },
                        )
                    }
                    .draggable(
                        state = rememberDraggableState { delta ->
                            offsetX = (offsetX + delta).coerceIn(-actionWidth, 0f)
                        },
                        orientation = Orientation.Horizontal,
                        onDragStopped = {
                            offsetX = if (offsetX <= -swipeThreshold) -actionWidth else 0f
                        },
                    )
                    .padding(
                        end = FoodarioTheme.dimensions.lg,
                        top = FoodarioTheme.dimensions.sm,
                        bottom = FoodarioTheme.dimensions.sm,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HandDrawnCheckbox(
                    checked = checked,
                    onCheckedChange = { onEvent(ShoppingListEvent.ToggleChecked(item.id)) },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = typography.titleHand,
                        color = if (checked) colors.inkSoft else colors.ink,
                        modifier = Modifier.markerStrikeThrough(checked),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        item.category?.let { category ->
                            Text(text = category.emoji, fontSize = 16.sp)
                            Spacer(Modifier.width(FoodarioTheme.dimensions.xs))
                            Text(
                                text = category.displayName,
                                style = typography.label,
                                color = colors.inkSoft,
                            )
                        }
                        if (item.quantity != null) {
                            Spacer(Modifier.width(FoodarioTheme.dimensions.sm))
                            Text(
                                text = formatQuantity(item.quantity, item.unit ?: QuantityUnit.UNIT),
                                style = typography.caption,
                                color = colors.inkSoft,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(FoodarioTheme.dimensions.sm))
                MoveToInventoryButton(
                    itemName = item.name,
                    onClick = { onEvent(ShoppingListEvent.MoveToInventory(item.id)) },
                )
            }
        }
    }
}

@Composable
private fun MoveToInventoryButton(
    itemName: String,
    onClick: () -> Unit,
) {
    val colors = FoodarioTheme.colors
    val shape = RoundedCornerShape(8.dp)
    val description = "Agregar $itemName a mi heladera"
    Row(
        modifier = Modifier
            .clip(shape)
            .background(colors.paperElevated)
            .border(1.dp, colors.penBlue.copy(alpha = 0.5f), shape)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(
                role = Role.Button,
                onClickLabel = description,
                onClick = onClick,
            )
            .semantics { contentDescription = description }
            .padding(horizontal = FoodarioTheme.dimensions.sm, vertical = FoodarioTheme.dimensions.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        FridgeIllustration(Modifier.size(24.dp))
    }
}

@Composable
fun Modifier.markerStrikeThrough(visible: Boolean): Modifier {
    if (!visible) return this
    val color = FoodarioTheme.colors.inkSoft
    return this.drawBehind {
        val y = size.height / 2f
        val path = Path().apply {
            moveTo(0f, y)
            cubicTo(
                size.width * 0.2f, y - 2.dp.toPx(),
                size.width * 0.4f, y + 2.dp.toPx(),
                size.width * 0.6f, y,
            )
            cubicTo(
                size.width * 0.8f, y - 2.dp.toPx(),
                size.width * 0.9f, y + 2.dp.toPx(),
                size.width, y,
            )
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

private fun sampleShoppingList(): List<ShoppingItem> =
    listOf(
        sampleShoppingItem(1L, "Leche", FoodCategory.DAIRY, 2.0, QuantityUnit.UNIT),
        sampleShoppingItem(2L, "Pechuga", FoodCategory.MEAT, 300.0, QuantityUnit.GRAMS),
        sampleShoppingItem(3L, "Manzanas", FoodCategory.FRUITS, 1.0, QuantityUnit.KILOGRAMS),
    )

private fun sampleShoppingItem(
    id: Long,
    name: String,
    category: FoodCategory,
    quantity: Double,
    unit: QuantityUnit,
) = ShoppingItem(
    id = id,
    name = name,
    category = category,
    quantity = quantity,
    unit = unit,
    createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
)

private fun fakeMoveToInventoryUseCase(): UseCase<MoveToInventoryParams, FoodItem> =
    object : UseCase<MoveToInventoryParams, FoodItem> {
        override suspend fun invoke(params: MoveToInventoryParams): FoodItem =
            FoodItem(
                id = 0L,
                name = "",
                category = FoodCategory.OTHER,
                quantity = 1.0,
                unit = QuantityUnit.UNIT,
                isFrozen = false,
                expirationDate = null,
                createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
                updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
            )
    }

private fun fakeAddToShoppingListUseCase(): UseCase<AddToShoppingListParams, Unit> =
    object : UseCase<AddToShoppingListParams, Unit> {
        override suspend fun invoke(params: AddToShoppingListParams) = Unit
    }

@Preview
@Composable
private fun ShoppingListScreenLightPreview() {
    FoodarioTheme(darkTheme = false) {
        ShoppingListScreen(
            viewModel = ShoppingListViewModel(
                observeShoppingList = previewObserveUseCase(sampleShoppingList()),
                moveToInventory = fakeMoveToInventoryUseCase(),
                addToShoppingList = fakeAddToShoppingListUseCase(),
                removeFromShoppingList = previewUnitUseCase(),
            ),
        )
    }
}

@Preview
@Composable
private fun ShoppingListScreenDarkPreview() {
    FoodarioTheme(darkTheme = true) {
        ShoppingListScreen(
            viewModel = ShoppingListViewModel(
                observeShoppingList = previewObserveUseCase(sampleShoppingList()),
                moveToInventory = fakeMoveToInventoryUseCase(),
                addToShoppingList = fakeAddToShoppingListUseCase(),
                removeFromShoppingList = previewUnitUseCase(),
            ),
        )
    }
}

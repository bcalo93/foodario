package com.foodario.shoppinglist.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.core.domain.usecase.UseCase
import com.foodario.core.presentation.components.HandDrawnCheckbox
import com.foodario.core.presentation.components.emoji
import com.foodario.core.presentation.components.formatQuantity
import com.foodario.core.presentation.components.notebookMargin
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.shoppinglist.domain.model.ShoppingItem
import com.foodario.shoppinglist.domain.usecase.MoveToInventoryParams
import com.foodario.shoppinglist.domain.usecase.ObserveShoppingListParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Instant
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ShoppingListScreen(
    viewModel: ShoppingListViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper),
    ) {
        Text(
            text = "Compras",
            style = typography.displayHand,
            color = colors.ink,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
        )

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
                        Text(text = "🛒", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
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
                        .fillMaxSize()
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
    }
}

@Composable
private fun ShoppingListRow(
    item: ShoppingItem,
    checked: Boolean,
    onEvent: (ShoppingListEvent) -> Unit,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 16.dp, top = 8.dp, bottom = 8.dp),
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
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = category.name,
                        style = typography.label,
                        color = colors.inkSoft,
                    )
                }
                if (item.quantity != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatQuantity(item.quantity, item.unit ?: QuantityUnit.UNIT),
                        style = typography.caption,
                        color = colors.inkSoft,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        MoveToInventoryButton(onClick = { onEvent(ShoppingListEvent.MoveToInventory(item.id)) })
    }
}

@Composable
private fun MoveToInventoryButton(onClick: () -> Unit) {
    val colors = FoodarioTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Text(
        text = "→ heladera",
        style = FoodarioTheme.typography.label,
        color = colors.penBlue,
        modifier = Modifier
            .clip(shape)
            .background(colors.paperElevated)
            .border(1.dp, colors.penBlue.copy(alpha = 0.5f), shape)
            .defaultMinSize(minHeight = 40.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
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

private fun fakeObserveShoppingListUseCase(): ObserveUseCase<ObserveShoppingListParams, List<ShoppingItem>> =
    object : ObserveUseCase<ObserveShoppingListParams, List<ShoppingItem>> {
        override fun invoke(params: ObserveShoppingListParams): Flow<List<ShoppingItem>> =
            flowOf(
                listOf(
                    sampleShoppingItem(1L, "Leche", FoodCategory.DAIRY, 2.0, QuantityUnit.UNIT),
                    sampleShoppingItem(2L, "Pechuga", FoodCategory.MEAT, 300.0, QuantityUnit.GRAMS),
                    sampleShoppingItem(3L, "Manzanas", FoodCategory.FRUITS, 1.0, QuantityUnit.KILOGRAMS),
                )
            )
    }

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

@Preview
@Composable
private fun ShoppingListScreenLightPreview() {
    FoodarioTheme(darkTheme = false) {
        ShoppingListScreen(
            viewModel = ShoppingListViewModel(
                observeShoppingList = fakeObserveShoppingListUseCase(),
                moveToInventory = fakeMoveToInventoryUseCase(),
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
                observeShoppingList = fakeObserveShoppingListUseCase(),
                moveToInventory = fakeMoveToInventoryUseCase(),
            ),
        )
    }
}

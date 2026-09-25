package com.foodario.inventory.presentation.uicomponents

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foodario.core.presentation.components.CategoryGrid
import com.foodario.core.presentation.components.DoodleDivider
import com.foodario.core.presentation.components.FridgeIllustration
import com.foodario.core.presentation.components.QuickAddBar
import com.foodario.core.presentation.components.notebookMargin
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.presentation.InventoryEffect
import com.foodario.inventory.presentation.InventoryEvent
import com.foodario.inventory.presentation.InventoryUiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun InventoryContent(
    uiState: InventoryUiState,
    quickAddCategory: FoodCategory,
    flight: InventoryEffect.ItemAdded?,
    onEvent: (InventoryEvent) -> Unit,
    onItemClick: (Long) -> Unit,
    onFlightFinished: () -> Unit,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography

    val listState = rememberLazyListState()
    var parentCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var listCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var barCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { parentCoordinates = it },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.paper)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FoodarioTheme.dimensions.lg)
                    .padding(top = FoodarioTheme.dimensions.lg),
            ) {
                Text(
                    text = "Mi heladera",
                    style = typography.displayHand,
                    color = colors.ink,
                )
                Spacer(Modifier.height(FoodarioTheme.dimensions.md))
                InventorySearchField(
                    value = uiState.searchQuery,
                    onValueChange = { onEvent(InventoryEvent.SearchQueryChanged(it)) },
                )
                Spacer(Modifier.height(FoodarioTheme.dimensions.md))
                CategoryGrid(
                    selectedCategory = uiState.selectedCategory,
                    onCategoryClick = { category ->
                        val next = if (category == uiState.selectedCategory) null else category
                        onEvent(InventoryEvent.CategorySelected(next))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(FoodarioTheme.dimensions.sm))
                DoodleDivider()
            }

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
                            FridgeIllustration(
                                modifier = Modifier.size(96.dp),
                                contentDescription = "Ilustración de una heladera",
                            )
                            Spacer(Modifier.height(FoodarioTheme.dimensions.md))
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
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .notebookMargin()
                            .onGloballyPositioned { listCoordinates = it },
                    ) {
                        items(uiState.items, key = { it.id }) { item ->
                            InventoryItemRow(
                                item = item,
                                onClick = { onItemClick(item.id) },
                                onIncreaseQuantity = {
                                    onEvent(
                                        InventoryEvent.IncreaseQuantity(
                                            itemId = item.id,
                                            currentQuantity = item.quantity,
                                            unit = item.unit,
                                        )
                                    )
                                },
                                onDelete = { onEvent(InventoryEvent.Delete(item.id)) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }

            DoodleDivider()
            QuickAddBar(
                selectedCategory = quickAddCategory,
                onCategorySelected = { onEvent(InventoryEvent.QuickAddCategorySelected(it)) },
                onAdd = { onEvent(InventoryEvent.QuickAdd(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.paper)
                    .padding(horizontal = FoodarioTheme.dimensions.lg)
                    .padding(bottom = FoodarioTheme.dimensions.sm)
                    .onGloballyPositioned { barCoordinates = it },
            )
        }

        AddItemFlightOverlay(
            flight = flight,
            items = uiState.items,
            listState = listState,
            parentCoordinates = parentCoordinates,
            listCoordinates = listCoordinates,
            barCoordinates = barCoordinates,
            onFinished = onFlightFinished,
        )
    }
}

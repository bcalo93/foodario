package com.foodario.shoppinglist.presentation.uicomponents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.foodario.core.presentation.components.DoodleDivider
import com.foodario.core.presentation.components.QuickAddBar
import com.foodario.core.presentation.components.notebookMargin
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.shoppinglist.presentation.ShoppingListEvent
import com.foodario.shoppinglist.presentation.ShoppingListUiState

@Composable
internal fun ShoppingListContent(
    uiState: ShoppingListUiState,
    quickAddCategory: FoodCategory,
    onEvent: (ShoppingListEvent) -> Unit,
) {
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
                val unchecked = uiState.items.filter { it.id !in uiState.checkedIds }
                val checked = uiState.items.filter { it.id in uiState.checkedIds }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .notebookMargin(),
                ) {
                    if (unchecked.isEmpty()) {
                        item(key = "empty-unchecked") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = FoodarioTheme.dimensions.lg,
                                        vertical = FoodarioTheme.dimensions.lg,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Todo tachado 🎉",
                                    style = typography.titleHand,
                                    color = colors.inkSoft,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    } else {
                        items(unchecked, key = { it.id }) { item ->
                            ShoppingListItemRow(
                                item = item,
                                checked = item.id in uiState.pendingIds,
                                pending = item.id in uiState.pendingIds,
                                onEvent = onEvent,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    if (checked.isNotEmpty()) {
                        item(key = "checked-header") {
                            CheckedSectionHeader(
                                count = checked.size,
                                expanded = uiState.showCheckedSection,
                                onToggle = { onEvent(ShoppingListEvent.ToggleCheckedSection) },
                                modifier = Modifier.animateItem(),
                            )
                        }

                        item(key = "checked-section-body") {
                            AnimatedVisibility(
                                visible = uiState.showCheckedSection,
                                enter = expandVertically(
                                    animationSpec = tween(200),
                                    expandFrom = Alignment.Top,
                                ) + fadeIn(tween(200)),
                                exit = shrinkVertically(
                                    animationSpec = tween(200),
                                    shrinkTowards = Alignment.Top,
                                ) + fadeOut(tween(150)),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    checked.forEach { item ->
                                        ShoppingListItemRow(
                                            item = item,
                                            checked = true,
                                            pending = false,
                                            onEvent = onEvent,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        DoodleDivider()
        QuickAddBar(
            selectedCategory = quickAddCategory,
            onCategorySelected = { onEvent(ShoppingListEvent.QuickAddCategorySelected(it)) },
            onAdd = { onEvent(ShoppingListEvent.QuickAdd(it)) },
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

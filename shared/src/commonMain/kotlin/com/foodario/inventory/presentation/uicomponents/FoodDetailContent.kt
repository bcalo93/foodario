package com.foodario.inventory.presentation.uicomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodario.core.presentation.components.DoodleDivider
import com.foodario.core.presentation.components.HandDrawnCheckbox
import com.foodario.core.presentation.components.QuantityStepper
import com.foodario.core.presentation.components.emoji
import com.foodario.core.presentation.components.notebookMargin
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.core.presentation.theme.categoryColor
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.presentation.FoodDetailEvent
import com.foodario.inventory.presentation.formatDate

@Composable
internal fun FoodDetailContent(
    item: FoodItem,
    onEvent: (FoodDetailEvent) -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showExpirationDialog by remember { mutableStateOf(false) }
    var showUnitDialog by remember { mutableStateOf(false) }
    var showQuantityDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .notebookMargin()
            .verticalScroll(rememberScrollState())
            .padding(end = FoodarioTheme.dimensions.lg),
    ) {
        Spacer(Modifier.height(FoodarioTheme.dimensions.lg))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { showCategoryDialog = true }
                .heightIn(min = FoodarioTheme.dimensions.touchTarget)
                .semantics {
                    role = Role.Button
                    contentDescription = "Cambiar categoría"
                },
        ) {
            Text(text = item.category.emoji, fontSize = 32.sp)
            Spacer(Modifier.width(FoodarioTheme.dimensions.sm))
            Text(
                text = item.category.displayName,
                style = typography.labelHand,
                color = colors.ink,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(categoryColor(item.category))
                    .padding(horizontal = FoodarioTheme.dimensions.xs, vertical = FoodarioTheme.dimensions.xxs),
            )
        }
        Spacer(Modifier.height(FoodarioTheme.dimensions.sm))
        Text(
            text = item.name,
            style = typography.displayHand,
            color = colors.ink,
        )
        Spacer(Modifier.height(FoodarioTheme.dimensions.lg))
        DoodleDivider()
        Spacer(Modifier.height(FoodarioTheme.dimensions.lg))

        QuantityStepper(
            quantity = item.quantity,
            unit = item.unit,
            onIncrement = { onEvent(FoodDetailEvent.IncrementQuantity) },
            onDecrement = { onEvent(FoodDetailEvent.DecrementQuantity) },
            onQuantityClick = { showQuantityDialog = true },
            onUnitClick = { showUnitDialog = true },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FoodarioTheme.dimensions.md))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            HandDrawnCheckbox(
                checked = item.isFrozen,
                onCheckedChange = { onEvent(FoodDetailEvent.ToggleFrozen) },
            )
            Spacer(Modifier.width(FoodarioTheme.dimensions.sm))
            Text(
                text = "Congelado",
                style = typography.labelHand,
                color = colors.ink,
            )
        }
        Spacer(Modifier.height(FoodarioTheme.dimensions.lg))
        DoodleDivider()
        Spacer(Modifier.height(FoodarioTheme.dimensions.lg))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Vencimiento",
                    style = typography.label,
                    color = colors.inkSoft,
                )
                Text(
                    text = item.expirationDate?.let { "Vence: ${formatDate(it)}" } ?: "Sin vencimiento",
                    style = typography.body,
                    color = colors.ink,
                )
            }
            TextButton(onClick = { showExpirationDialog = true }) {
                Text(text = "Editar", color = colors.penBlue)
            }
        }
        Spacer(Modifier.height(FoodarioTheme.dimensions.lg))

        DetailActionButton(
            text = "Agregar a la lista de compras",
            color = colors.penBlue,
            onClick = { onEvent(FoodDetailEvent.AddToShoppingList) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FoodarioTheme.dimensions.md))

        DetailActionButton(
            text = "Eliminar",
            color = colors.marginRed,
            onClick = onDeleteRequest,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FoodarioTheme.dimensions.xl))
    }

    if (showQuantityDialog) {
        QuantityEditDialog(
            quantity = item.quantity,
            unit = item.unit,
            onDismiss = { showQuantityDialog = false },
            onConfirm = { value ->
                showQuantityDialog = false
                onEvent(FoodDetailEvent.QuantitySet(value))
            },
        )
    }

    if (showExpirationDialog) {
        ExpirationDialog(
            currentDate = item.expirationDate,
            onDismiss = { showExpirationDialog = false },
            onSelect = { date ->
                showExpirationDialog = false
                onEvent(FoodDetailEvent.ExpirationDateChanged(date))
            },
        )
    }

    if (showCategoryDialog) {
        CategorySelectionDialog(
            currentCategory = item.category,
            onDismiss = { showCategoryDialog = false },
            onSelect = { category ->
                showCategoryDialog = false
                onEvent(FoodDetailEvent.CategoryChanged(category))
            },
        )
    }

    if (showUnitDialog) {
        UnitSelectionDialog(
            currentUnit = item.unit,
            onDismiss = { showUnitDialog = false },
            onSelect = { unit ->
                showUnitDialog = false
                onEvent(FoodDetailEvent.UnitChanged(unit))
            },
        )
    }
}

@Composable
private fun DetailActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Text(
        text = text,
        style = FoodarioTheme.typography.titleHand,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(shape)
            .border(1.5.dp, color, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = FoodarioTheme.dimensions.xl, vertical = FoodarioTheme.dimensions.md),
    )
}

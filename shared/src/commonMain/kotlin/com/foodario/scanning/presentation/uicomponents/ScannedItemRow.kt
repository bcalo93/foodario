package com.foodario.scanning.presentation.uicomponents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodario.core.domain.matching.MatchConfidence
import com.foodario.core.presentation.components.HandDrawnCheckbox
import com.foodario.core.presentation.components.emoji
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.model.formatQuantityNumber
import com.foodario.inventory.domain.model.snapToStep
import com.foodario.scanning.presentation.ReceiptScanEvent
import com.foodario.scanning.presentation.ScannedReviewItem

@Composable
fun ScannedItemRow(
    item: ScannedReviewItem,
    onEvent: (ReceiptScanEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography
    var showCategoryDialog by remember(item.key) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = colors.pencilGray.copy(alpha = 0.4f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = FoodarioTheme.dimensions.lg, vertical = FoodarioTheme.dimensions.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HandDrawnCheckbox(
                checked = item.included,
                onCheckedChange = { onEvent(ReceiptScanEvent.ToggleItemIncluded(item.key)) },
            )
            Spacer(Modifier.width(FoodarioTheme.dimensions.sm))
            Box(
                modifier = Modifier
                    .size(FoodarioTheme.dimensions.touchTarget)
                    .clickable(onClick = { showCategoryDialog = true }),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = item.category.emoji, fontSize = 24.sp)
            }
            Spacer(Modifier.width(FoodarioTheme.dimensions.sm))
            BasicTextField(
                value = item.name,
                onValueChange = { onEvent(ReceiptScanEvent.EditItemName(item.key, it)) },
                enabled = item.included,
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = FoodarioTheme.dimensions.touchTarget)
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
                    .then(if (item.included) Modifier else Modifier.strikeThrough()),
                textStyle = typography.titleHand.copy(
                    color = if (item.included) colors.ink else colors.inkSoft,
                ),
                cursorBrush = SolidColor(colors.penBlue),
            )
        }

        if (item.match != null && item.included) {
            Spacer(Modifier.width(FoodarioTheme.dimensions.sm))
            DuplicateBadge(item = item, onEvent = onEvent)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            CompactQuantity(item = item, onEvent = onEvent)
            Spacer(Modifier.width(FoodarioTheme.dimensions.sm))
            Text(
                text = item.unit.displayName,
                style = typography.labelHand,
                color = colors.ink,
                modifier = Modifier
                    .defaultMinSize(minHeight = FoodarioTheme.dimensions.touchTarget)
                    .clickable {
                        onEvent(ReceiptScanEvent.EditItemUnit(item.key, nextUnit(item.unit)))
                    }
                    .padding(horizontal = FoodarioTheme.dimensions.xs),
            )
        }
    }

    if (showCategoryDialog) {
        CategorySelectionDialog(
            currentCategory = item.category,
            onDismiss = { showCategoryDialog = false },
            onSelect = { category ->
                showCategoryDialog = false
                onEvent(ReceiptScanEvent.EditItemCategory(item.key, category))
            },
        )
    }
}

@Composable
private fun DuplicateBadge(
    item: ScannedReviewItem,
    onEvent: (ReceiptScanEvent) -> Unit,
) {
    val match = item.match ?: return
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography
    val isPartial = match.confidence == MatchConfidence.PARTIAL
    val merged = item.mergeIntoItemId != null
    val text = if (match.confidence == MatchConfidence.EXACT || merged) {
        "↪ se suma a «${match.item.name}» (tenés ${formatQuantityNumber(match.item.quantity)} ${match.item.unit.displayName})"
    } else {
        "¿Es lo mismo que «${match.item.name}»? Tocá para fusionar"
    }
    Text(
        text = text,
        style = typography.labelHand,
        color = colors.penBlue,
        modifier = Modifier
            .then(
                if (isPartial) {
                    Modifier.clickable { onEvent(ReceiptScanEvent.ToggleMerge(item.key)) }
                } else {
                    Modifier
                }
            )
            .padding(vertical = FoodarioTheme.dimensions.xxs),
    )
}

@Composable
private fun CompactQuantity(
    item: ScannedReviewItem,
    onEvent: (ReceiptScanEvent) -> Unit,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton(
            symbol = "−",
            contentDescription = "Quitar",
            onClick = {
                val newQuantity = snapToStep(
                    (item.quantity - item.unit.step).coerceAtLeast(item.unit.step),
                    item.unit.step,
                )
                onEvent(ReceiptScanEvent.EditItemQuantity(item.key, newQuantity))
            },
        )
        Text(
            text = formatQuantityNumber(item.quantity),
            style = typography.quantityHand,
            color = colors.ink,
        )
        StepperButton(
            symbol = "+",
            contentDescription = "Agregar",
            onClick = {
                val newQuantity = snapToStep(item.quantity + item.unit.step, item.unit.step)
                onEvent(ReceiptScanEvent.EditItemQuantity(item.key, newQuantity))
            },
        )
    }
}

@Composable
private fun StepperButton(
    symbol: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val colors = FoodarioTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(FoodarioTheme.dimensions.touchTarget)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = symbol,
            style = FoodarioTheme.typography.quantityHand.copy(fontSize = 24.sp),
            color = colors.penBlue,
        )
    }
}

@Composable
private fun CategorySelectionDialog(
    currentCategory: FoodCategory,
    onDismiss: () -> Unit,
    onSelect: (FoodCategory) -> Unit,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.paperElevated,
        title = {
            Text(
                text = "Tipo de alimento",
                style = typography.titleHand,
                color = colors.ink,
            )
        },
        text = {
            Column {
                FoodCategory.entries.forEach { category ->
                    TextButton(
                        onClick = { onSelect(category) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = category.emoji, fontSize = 24.sp)
                            Spacer(Modifier.width(FoodarioTheme.dimensions.sm))
                            Text(
                                text = category.displayName,
                                style = typography.body,
                                color = if (category == currentCategory) colors.penBlue else colors.ink,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
    )
}

private fun nextUnit(unit: QuantityUnit): QuantityUnit = when (unit) {
    QuantityUnit.UNIT -> QuantityUnit.GRAMS
    QuantityUnit.GRAMS -> QuantityUnit.KILOGRAMS
    QuantityUnit.KILOGRAMS -> QuantityUnit.MILLILITERS
    QuantityUnit.MILLILITERS -> QuantityUnit.LITERS
    QuantityUnit.LITERS -> QuantityUnit.UNIT
}

@Composable
private fun Modifier.strikeThrough(): Modifier {
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

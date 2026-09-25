package com.foodario.inventory.presentation.uicomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.foodario.core.presentation.components.NotebookListItem
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.inventory.domain.model.FoodItem
import kotlin.math.roundToInt

@Composable
internal fun InventoryItemRow(
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
                    .padding(horizontal = FoodarioTheme.dimensions.xl),
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

package com.foodario.inventory.presentation.uicomponents

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodario.core.presentation.components.emoji
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.presentation.InventoryEffect
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun AddItemFlightOverlay(
    flight: InventoryEffect.ItemAdded?,
    items: List<FoodItem>,
    listState: LazyListState,
    parentCoordinates: LayoutCoordinates?,
    listCoordinates: LayoutCoordinates?,
    barCoordinates: LayoutCoordinates?,
    onFinished: () -> Unit,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val currentItems by rememberUpdatedState(items)
    val currentListState by rememberUpdatedState(listState)
    val currentParent by rememberUpdatedState(parentCoordinates)
    val currentList by rememberUpdatedState(listCoordinates)
    val currentBar by rememberUpdatedState(barCoordinates)

    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val progress = remember { Animatable(0f, Float.VectorConverter) }
    var visible by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("") }

    LaunchedEffect(flight?.itemId) {
        visible = false
        val current = flight ?: return@LaunchedEffect
        val id = current.itemId

        val index = withTimeoutOrNull(800.milliseconds) {
            snapshotFlow { currentItems.indexOfFirst { it.id == id } }
                .first { it >= 0 }
        }
        if (index == null) {
            onFinished()
            return@LaunchedEffect
        }

        val parent = currentParent
        val bar = currentBar
        if (parent == null || bar == null) {
            if (currentListState.layoutInfo.visibleItemsInfo.none { it.index == index }) {
                scope.launch { currentListState.animateScrollToItem(index) }
            }
            onFinished()
            return@LaunchedEffect
        }

        val start = parent.localPositionOf(
            bar,
            Offset(bar.size.width * 0.25f, bar.size.height * 0.7f),
        )

        val list = currentList
        val info = currentListState.layoutInfo.visibleItemsInfo.find { it.index == index }
        val end: Offset
        var scrollToItem = false
        when {
            info != null && list != null -> {
                end = parent.localPositionOf(
                    list,
                    with(density) { Offset(48.dp.toPx(), info.offset.toFloat() + info.size / 2f) },
                )
            }
            list != null -> {
                val topLeft = parent.localPositionOf(list, Offset.Zero)
                val goDown = index > (currentListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1)
                val y = if (goDown) {
                    topLeft.y + list.size.height.toFloat() - with(density) { 28.dp.toPx() }
                } else {
                    topLeft.y + with(density) { 28.dp.toPx() }
                }
                end = with(density) { Offset(topLeft.x + 48.dp.toPx(), y) }
                scrollToItem = true
            }
            else -> end = start
        }

        label = current.name
        emoji = current.category.emoji

        offset.snapTo(start)
        progress.snapTo(0f)

        visible = true

        if (scrollToItem) {
            scope.launch { currentListState.animateScrollToItem(index) }
        }

        coroutineScope {
            launch { offset.animateTo(end, tween(250, easing = FastOutSlowInEasing)) }
            launch { progress.animateTo(1f, tween(250, easing = FastOutSlowInEasing)) }
        }

        onFinished()
    }

    if (visible) {
        val alpha = if (progress.value < 0.8f) {
            1f
        } else {
            (1f - (progress.value - 0.8f) / 0.2f).coerceIn(0f, 1f)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .offset { IntOffset(offset.value.x.roundToInt(), offset.value.y.roundToInt()) }
                .clearAndSetSemantics { }
                .background(colors.paper)
                .alpha(alpha),
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(Modifier.width(FoodarioTheme.dimensions.xs))
            Text(
                text = label,
                style = typography.titleHand,
                color = colors.ink,
                maxLines = 1,
            )
        }
    }
}

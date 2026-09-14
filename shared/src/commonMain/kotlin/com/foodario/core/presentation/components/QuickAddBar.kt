package com.foodario.core.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.core.presentation.theme.categoryColor
import com.foodario.inventory.domain.model.FoodCategory

@Composable
fun QuickAddBar(
    selectedCategory: FoodCategory,
    onCategorySelected: (FoodCategory) -> Unit,
    onAdd: (name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography
    var text by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .weight(1f)
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
                        if (text.isEmpty()) {
                            Text(
                                text = "Escribí un alimento…",
                                style = typography.body,
                                color = colors.inkSoft,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            AddButton(
                onClick = {
                    val value = text.trim()
                    if (value.isNotEmpty()) {
                        onAdd(value)
                        text = ""
                    }
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FoodCategory.entries.forEach { category ->
                CategoryChip(
                    category = category,
                    selected = category == selectedCategory,
                    onClick = { onCategorySelected(category) },
                )
            }
        }
    }
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    val penBlue = FoodarioTheme.colors.penBlue
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Agregar alimento" },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = penBlue,
                radius = size.minDimension / 2f - 2.dp.toPx(),
                style = Stroke(width = 1.5.dp.toPx()),
            )
            val cx = size.width / 2f
            val cy = size.height / 2f
            val arm = size.width * 0.22f
            drawLine(
                color = penBlue,
                start = Offset(cx - arm, cy),
                end = Offset(cx + arm, cy),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = penBlue,
                start = Offset(cx, cy - arm),
                end = Offset(cx, cy + arm),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: FoodCategory,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = FoodarioTheme.colors
    val borderColor = if (selected) colors.penBlue else colors.pencilGray
    val shape = RoundedCornerShape(50)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(shape)
            .background(categoryColor(category))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = category.name },
    ) {
        Text(text = category.emoji, fontSize = 20.sp)
    }
}

@Preview
@Composable
private fun QuickAddBarLightPreview() {
    FoodarioTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper)
                .padding(16.dp),
        ) {
            QuickAddBar(
                selectedCategory = FoodCategory.FRUITS,
                onCategorySelected = {},
                onAdd = {},
            )
        }
    }
}

@Preview
@Composable
private fun QuickAddBarDarkPreview() {
    FoodarioTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper)
                .padding(16.dp),
        ) {
            QuickAddBar(
                selectedCategory = FoodCategory.FRUITS,
                onCategorySelected = {},
                onAdd = {},
            )
        }
    }
}

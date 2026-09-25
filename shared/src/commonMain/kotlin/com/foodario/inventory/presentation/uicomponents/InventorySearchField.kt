package com.foodario.inventory.presentation.uicomponents

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.foodario.core.presentation.theme.FoodarioTheme

@Composable
internal fun InventorySearchField(
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
            .padding(vertical = FoodarioTheme.dimensions.md),
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

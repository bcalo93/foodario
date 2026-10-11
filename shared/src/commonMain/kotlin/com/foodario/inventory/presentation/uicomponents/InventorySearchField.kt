package com.foodario.inventory.presentation.uicomponents

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = "Buscar alimento…",
                            style = typography.body,
                            color = colors.inkSoft,
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    ClearSearchButton(onClick = { onValueChange("") })
                }
            }
        },
    )
}

@Composable
private fun ClearSearchButton(onClick: () -> Unit) {
    val color = FoodarioTheme.colors.inkSoft

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(FoodarioTheme.dimensions.touchTarget)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = "Borrar búsqueda"
            },
    ) {
        Canvas(Modifier.size(24.dp)) {
            val firstStroke = Path().apply {
                moveTo(size.width * 0.28f, size.height * 0.27f)
                quadraticTo(
                    size.width * 0.47f,
                    size.height * 0.48f,
                    size.width * 0.73f,
                    size.height * 0.72f,
                )
            }
            val secondStroke = Path().apply {
                moveTo(size.width * 0.72f, size.height * 0.28f)
                quadraticTo(
                    size.width * 0.52f,
                    size.height * 0.5f,
                    size.width * 0.27f,
                    size.height * 0.73f,
                )
            }
            val stroke = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
            drawPath(path = firstStroke, color = color, style = stroke)
            drawPath(path = secondStroke, color = color, style = stroke)
        }
    }
}

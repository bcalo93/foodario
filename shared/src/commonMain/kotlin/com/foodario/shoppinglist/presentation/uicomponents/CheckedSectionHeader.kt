package com.foodario.shoppinglist.presentation.uicomponents

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.foodario.core.presentation.theme.FoodarioTheme

@Composable
internal fun CheckedSectionHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography
    val description = if (expanded) "Ocultar tachados" else "Mostrar tachados"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.paper)
            .border(
                width = 1.dp,
                color = colors.pencilGray.copy(alpha = 0.4f),
            )
            .clickable(role = Role.Button, onClickLabel = description, onClick = onToggle)
            .semantics { contentDescription = description }
            .padding(
                horizontal = FoodarioTheme.dimensions.lg,
                vertical = FoodarioTheme.dimensions.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Tachados ($count)",
            style = typography.labelHand,
            color = colors.inkSoft,
        )
        Canvas(modifier = Modifier.size(20.dp)) {
            val stroke = 2.dp.toPx()
            val inset = size.width * 0.3f
            if (expanded) {
                val apexY = size.height * 0.35f
                val baseY = size.height * 0.65f
                drawLine(
                    color = colors.penBlue,
                    start = Offset(inset, baseY),
                    end = Offset(size.width / 2f, apexY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = colors.penBlue,
                    start = Offset(size.width - inset, baseY),
                    end = Offset(size.width / 2f, apexY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            } else {
                val apexY = size.height * 0.65f
                val baseY = size.height * 0.35f
                drawLine(
                    color = colors.penBlue,
                    start = Offset(inset, baseY),
                    end = Offset(size.width / 2f, apexY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = colors.penBlue,
                    start = Offset(size.width - inset, baseY),
                    end = Offset(size.width / 2f, apexY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

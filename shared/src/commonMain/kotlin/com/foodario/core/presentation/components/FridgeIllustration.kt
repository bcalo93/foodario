package com.foodario.core.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.foodario.core.presentation.theme.FoodarioTheme

@Composable
fun FridgeIllustration(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val colors = FoodarioTheme.colors
    val semanticModifier = if (contentDescription == null) {
        modifier
    } else {
        modifier.semantics { this.contentDescription = contentDescription }
    }

    Canvas(semanticModifier) {
        val outlineWidth = (size.minDimension * 0.06f).coerceAtLeast(1.5f)
        val outline = colors.penBlue
        val body = colors.paperElevated
        val dividerY = size.height * 0.42f
        val handleX = size.width * 0.82f

        drawRoundRect(
            color = body,
            topLeft = Offset(outlineWidth, outlineWidth),
            size = Size(
                width = size.width - outlineWidth * 2,
                height = size.height - outlineWidth * 2,
            ),
            cornerRadius = CornerRadius(size.minDimension * 0.1f),
        )
        drawRoundRect(
            color = outline,
            topLeft = Offset(outlineWidth, outlineWidth),
            size = Size(
                width = size.width - outlineWidth * 2,
                height = size.height - outlineWidth * 2,
            ),
            cornerRadius = CornerRadius(size.minDimension * 0.1f),
            style = Stroke(width = outlineWidth),
        )
        drawLine(
            color = outline,
            start = Offset(outlineWidth, dividerY),
            end = Offset(size.width - outlineWidth, dividerY),
            strokeWidth = outlineWidth,
        )
        drawLine(
            color = outline,
            start = Offset(handleX, size.height * 0.16f),
            end = Offset(handleX, size.height * 0.32f),
            strokeWidth = outlineWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = outline,
            start = Offset(handleX, size.height * 0.56f),
            end = Offset(handleX, size.height * 0.72f),
            strokeWidth = outlineWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Preview
@Composable
private fun FridgeIllustrationLightPreview() {
    FoodarioTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper)
                .padding(32.dp),
        ) {
            FridgeIllustration(Modifier.size(120.dp))
        }
    }
}

@Preview
@Composable
private fun FridgeIllustrationDarkPreview() {
    FoodarioTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper)
                .padding(32.dp),
        ) {
            FridgeIllustration(Modifier.size(120.dp))
        }
    }
}

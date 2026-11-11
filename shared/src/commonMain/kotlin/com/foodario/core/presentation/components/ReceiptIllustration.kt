package com.foodario.core.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.foodario.core.presentation.theme.FoodarioTheme

/**
 * Hand-drawn supermarket receipt: a narrow piece of paper with scribbled product lines and a
 * torn bottom edge. Represents the receipt-scanning entry point (scan a ticket → add to the fridge).
 */
@Composable
fun ReceiptIllustration(
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
        val strokeWidth = (size.minDimension * 0.07f).coerceAtLeast(1.dp.toPx())
        val outline = colors.penBlue

        val left = size.width * 0.28f
        val right = size.width * 0.72f
        val top = size.height * 0.08f
        val bottom = size.height * 0.80f

        val body = Path().apply {
            moveTo(left, top)
            cubicTo(
                size.width * 0.38f, top - strokeWidth,
                size.width * 0.62f, top + strokeWidth,
                right, top,
            )
            cubicTo(
                right + strokeWidth, size.height * 0.40f,
                right - strokeWidth, size.height * 0.60f,
                right, bottom,
            )
            val teeth = 3
            val stepX = (right - left) / teeth
            for (i in 0 until teeth) {
                val x1 = right - stepX * i
                val x2 = right - stepX * (i + 1)
                lineTo((x1 + x2) / 2f, bottom + size.height * 0.05f)
                lineTo(x2, bottom)
            }
            cubicTo(
                left - strokeWidth, size.height * 0.60f,
                left + strokeWidth, size.height * 0.40f,
                left, top,
            )
            close()
        }
        drawPath(
            path = body,
            color = outline,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        val lineStart = size.width * 0.35f
        val lineEnd = size.width * 0.65f
        drawWobblyLine(lineStart, size.height * 0.34f, lineEnd, size.height * 0.34f, outline, strokeWidth)
        drawWobblyLine(lineStart, size.height * 0.52f, lineEnd, size.height * 0.52f, outline, strokeWidth)
    }
}

private fun DrawScope.drawWobblyLine(
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    color: Color,
    strokeWidth: Float,
) {
    val midX = (startX + endX) / 2f
    val wobble = strokeWidth * 1.6f
    val path = Path().apply {
        moveTo(startX, startY)
        cubicTo(
            midX, startY - wobble,
            midX, startY + wobble,
            endX, endY,
        )
    }
    drawPath(path = path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
}

@Preview
@Composable
private fun ReceiptIllustrationLightPreview() {
    FoodarioTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .background(FoodarioTheme.colors.paper)
                .padding(FoodarioTheme.dimensions.xxl),
        ) {
            ReceiptIllustration(Modifier.size(48.dp))
        }
    }
}

@Preview
@Composable
private fun ReceiptIllustrationDarkPreview() {
    FoodarioTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(FoodarioTheme.colors.paper)
                .padding(FoodarioTheme.dimensions.xxl),
        ) {
            ReceiptIllustration(Modifier.size(48.dp))
        }
    }
}

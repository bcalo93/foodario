package com.foodario.core.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.foodario.core.presentation.theme.FoodarioTheme

@Composable
fun BellIllustration(
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
        val width = size.width
        val height = size.height
        val strokeWidth = (size.minDimension * 0.075f).coerceAtLeast(1.dp.toPx())
        val outline = colors.penBlue

        val body = Path().apply {
            moveTo(width * 0.50f, height * 0.19f)
            cubicTo(
                width * 0.39f, height * 0.18f,
                width * 0.32f, height * 0.27f,
                width * 0.31f, height * 0.40f,
            )
            cubicTo(
                width * 0.30f, height * 0.56f,
                width * 0.28f, height * 0.68f,
                width * 0.18f, height * 0.76f,
            )
            cubicTo(
                width * 0.13f, height * 0.80f,
                width * 0.16f, height * 0.85f,
                width * 0.24f, height * 0.84f,
            )
            cubicTo(
                width * 0.40f, height * 0.87f,
                width * 0.60f, height * 0.84f,
                width * 0.77f, height * 0.85f,
            )
            cubicTo(
                width * 0.84f, height * 0.85f,
                width * 0.87f, height * 0.81f,
                width * 0.82f, height * 0.76f,
            )
            cubicTo(
                width * 0.72f, height * 0.68f,
                width * 0.70f, height * 0.56f,
                width * 0.69f, height * 0.40f,
            )
            cubicTo(
                width * 0.68f, height * 0.27f,
                width * 0.61f, height * 0.18f,
                width * 0.50f, height * 0.19f,
            )
        }
        drawPath(
            path = body,
            color = outline,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        val crown = Path().apply {
            moveTo(width * 0.44f, height * 0.19f)
            cubicTo(
                width * 0.43f, height * 0.13f,
                width * 0.47f, height * 0.10f,
                width * 0.50f, height * 0.10f,
            )
            cubicTo(
                width * 0.54f, height * 0.10f,
                width * 0.57f, height * 0.14f,
                width * 0.56f, height * 0.19f,
            )
        }
        drawPath(
            path = crown,
            color = outline,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        val clapper = Path().apply {
            moveTo(width * 0.50f, height * 0.83f)
            cubicTo(
                width * 0.50f, height * 0.89f,
                width * 0.45f, height * 0.90f,
                width * 0.42f, height * 0.93f,
            )
            cubicTo(
                width * 0.46f, height * 0.98f,
                width * 0.54f, height * 0.98f,
                width * 0.58f, height * 0.93f,
            )
            cubicTo(
                width * 0.55f, height * 0.90f,
                width * 0.50f, height * 0.89f,
                width * 0.50f, height * 0.83f,
            )
        }
        drawPath(
            path = clapper,
            color = outline,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Preview
@Composable
private fun BellIllustrationLightPreview() {
    FoodarioTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .background(FoodarioTheme.colors.paper)
                .padding(FoodarioTheme.dimensions.xxl),
        ) {
            BellIllustration(Modifier.size(48.dp))
        }
    }
}

@Preview
@Composable
private fun BellIllustrationDarkPreview() {
    FoodarioTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(FoodarioTheme.colors.paper)
                .padding(FoodarioTheme.dimensions.xxl),
        ) {
            BellIllustration(Modifier.size(48.dp))
        }
    }
}

package com.foodario.core.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.foodario.core.presentation.theme.FoodarioTheme

@Composable
fun DoodleDivider(modifier: Modifier = Modifier) {
    val pencilGray = FoodarioTheme.colors.pencilGray
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp),
    ) {
        val strokeWidth = 1.5.dp.toPx()
        val y = size.height / 2f
        val startX = size.width * 0.08f
        val endX = size.width * 0.92f
        val path = Path().apply {
            moveTo(startX, y)
            cubicTo(
                size.width * 0.20f, y - 3.dp.toPx(),
                size.width * 0.28f, y + 3.dp.toPx(),
                size.width * 0.40f, y,
            )
            cubicTo(
                size.width * 0.52f, y - 2.dp.toPx(),
                size.width * 0.60f, y + 2.dp.toPx(),
                size.width * 0.70f, y,
            )
            cubicTo(
                size.width * 0.80f, y - 3.dp.toPx(),
                size.width * 0.86f, y + 3.dp.toPx(),
                endX, y,
            )
        }
        drawPath(path = path, color = pencilGray, style = Stroke(width = strokeWidth))
    }
}

@Preview
@Composable
private fun DoodleDividerLightPreview() {
    FoodarioTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper)
                .padding(FoodarioTheme.dimensions.lg),
        ) {
            DoodleDivider()
        }
    }
}

@Preview
@Composable
private fun DoodleDividerDarkPreview() {
    FoodarioTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper)
                .padding(FoodarioTheme.dimensions.lg),
        ) {
            DoodleDivider()
        }
    }
}

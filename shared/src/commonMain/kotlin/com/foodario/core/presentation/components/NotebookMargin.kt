package com.foodario.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.foodario.core.presentation.theme.FoodarioTheme

@Composable
fun Modifier.notebookMargin(): Modifier {
    val marginRed = FoodarioTheme.colors.marginRed
    return this
        .drawBehind {
            val x = 40.dp.toPx()
            drawLine(
                color = marginRed.copy(alpha = 0.6f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1.dp.toPx(),
            )
        }
        .padding(start = FoodarioTheme.dimensions.xxl)
}

@Preview
@Composable
private fun NotebookMarginLightPreview() {
    FoodarioTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper)
                .notebookMargin()
                .padding(end = FoodarioTheme.dimensions.lg),
        ) {
            Text(
                text = "Nota de ejemplo al margen",
                style = FoodarioTheme.typography.body,
                color = FoodarioTheme.colors.ink,
            )
        }
    }
}

@Preview
@Composable
private fun NotebookMarginDarkPreview() {
    FoodarioTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper)
                .notebookMargin()
                .padding(end = FoodarioTheme.dimensions.lg),
        ) {
            Text(
                text = "Nota de ejemplo al margen",
                style = FoodarioTheme.typography.body,
                color = FoodarioTheme.colors.ink,
            )
        }
    }
}

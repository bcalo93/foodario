package com.foodario.core.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.foodario.inventory.domain.model.FoodCategory

@Composable
private fun ThemeSample() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FoodarioTheme.colors.paper)
            .padding(FoodarioTheme.dimensions.lg),
        verticalArrangement = Arrangement.spacedBy(FoodarioTheme.dimensions.sm),
    ) {
        Text("Display Hand", style = FoodarioTheme.typography.displayHand, color = FoodarioTheme.colors.ink)
        Text("Title Hand", style = FoodarioTheme.typography.titleHand, color = FoodarioTheme.colors.penBlue)
        Text("Body text", style = FoodarioTheme.typography.body, color = FoodarioTheme.colors.inkSoft)
        Text("Caption text", style = FoodarioTheme.typography.caption, color = FoodarioTheme.colors.pencilGray)

        Row(horizontalArrangement = Arrangement.spacedBy(FoodarioTheme.dimensions.sm)) {
            FoodCategory.entries.forEach { category ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(categoryColor(category)),
                )
            }
        }
    }
}

@Preview
@Composable
private fun FoodarioThemeLightPreview() {
    FoodarioTheme(darkTheme = false) {
        ThemeSample()
    }
}

@Preview
@Composable
private fun FoodarioThemeDarkPreview() {
    FoodarioTheme(darkTheme = true) {
        ThemeSample()
    }
}

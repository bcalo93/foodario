package com.foodario.core.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class FoodarioColors(
    val paper: Color,
    val paperElevated: Color,
    val ink: Color,
    val inkSoft: Color,
    val penBlue: Color,
    val marginRed: Color,
    val pencilGray: Color,
)

val lightFoodarioColors = FoodarioColors(
    paper = paperLight,
    paperElevated = paperElevatedLight,
    ink = inkLight,
    inkSoft = inkSoftLight,
    penBlue = penBlueLight,
    marginRed = marginRedLight,
    pencilGray = pencilGrayLight,
)

val darkFoodarioColors = FoodarioColors(
    paper = paperDark,
    paperElevated = paperElevatedDark,
    ink = inkDark,
    inkSoft = inkSoftDark,
    penBlue = penBlueDark,
    marginRed = marginRedDark,
    pencilGray = pencilGrayDark,
)

val LocalFoodarioColors = staticCompositionLocalOf { lightFoodarioColors }

val LocalFoodarioTypography = staticCompositionLocalOf { FoodarioTypography() }

@Composable
fun FoodarioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) darkFoodarioColors else lightFoodarioColors
    val typography = foodarioTypography()

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.penBlue,
            onPrimary = colors.ink,
            error = colors.marginRed,
            background = colors.paper,
            onBackground = colors.ink,
            surface = colors.paperElevated,
            onSurface = colors.ink,
        )
    } else {
        lightColorScheme(
            primary = colors.penBlue,
            onPrimary = Color.White,
            error = colors.marginRed,
            background = colors.paper,
            onBackground = colors.ink,
            surface = colors.paperElevated,
            onSurface = colors.ink,
        )
    }

    CompositionLocalProvider(
        LocalFoodarioColors provides colors,
        LocalFoodarioTypography provides typography,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography.toMaterialTypography(),
            content = content,
        )
    }
}

object FoodarioTheme {
    val dimensions = FoodarioDimensions()

    val isDark: Boolean
        @Composable get() = LocalFoodarioColors.current == darkFoodarioColors

    val colors: FoodarioColors
        @Composable get() = LocalFoodarioColors.current

    val typography: FoodarioTypography
        @Composable get() = LocalFoodarioTypography.current
}

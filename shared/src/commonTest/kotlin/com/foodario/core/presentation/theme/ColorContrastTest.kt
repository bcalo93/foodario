package com.foodario.core.presentation.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

class ColorContrastTest {

    @Test
    fun lightModeTextColorsMeetAA() {
        assertTrue(contrastRatio(inkLight, paperLight) >= 4.5, "ink sobre paper (light)")
        assertTrue(contrastRatio(inkSoftLight, paperLight) >= 4.5, "inkSoft sobre paper (light)")
        assertTrue(contrastRatio(penBlueLight, paperLight) >= 4.5, "penBlue sobre paper (light)")
    }

    @Test
    fun darkModeTextColorsMeetAA() {
        assertTrue(contrastRatio(inkDark, paperDark) >= 4.5, "ink sobre paper (dark)")
        assertTrue(contrastRatio(inkSoftDark, paperDark) >= 4.5, "inkSoft sobre paper (dark)")
        assertTrue(contrastRatio(penBlueDark, paperDark) >= 4.5, "penBlue sobre paper (dark)")
    }

    @Test
    fun marginRedMeetsLargeTextAA() {
        assertTrue(contrastRatio(marginRedLight, paperLight) >= 3.0, "marginRed sobre paper (light)")
        assertTrue(contrastRatio(marginRedDark, paperDark) >= 3.0, "marginRed sobre paper (dark)")
    }
}

private fun contrastRatio(a: Color, b: Color): Double {
    val l1 = relativeLuminance(a)
    val l2 = relativeLuminance(b)
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
}

private fun relativeLuminance(color: Color): Double {
    val r = linearize(color.red)
    val g = linearize(color.green)
    val b = linearize(color.blue)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

private fun linearize(channel: Float): Double {
    val c = channel.toDouble()
    return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
}

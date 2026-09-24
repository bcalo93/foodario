package com.foodario.inventory.domain.model

import kotlin.math.roundToInt

enum class QuantityUnit(
    val displayName: String,
    val step: Double,
) {
    UNIT("Unidad", 1.0),
    GRAMS("Gramos", 50.0),
    KILOGRAMS("Kilogramos", 0.1),
    MILLILITERS("Mililitros", 50.0),
    LITERS("Litros", 0.1),
}

internal fun snapToStep(quantity: Double, step: Double): Double {
    if (step <= 0.0) return quantity.coerceAtLeast(0.0)
    val snapped = (quantity / step).roundToInt() * step
    return ((snapped * 1000.0).roundToInt() / 1000.0).coerceAtLeast(0.0)
}

internal fun formatQuantityNumber(quantity: Double): String {
    val normalized = (quantity * 1000.0).roundToInt() / 1000.0
    return if (normalized % 1.0 == 0.0) {
        normalized.toLong().toString()
    } else {
        normalized.toString().trimEnd('0').trimEnd('.')
    }
}

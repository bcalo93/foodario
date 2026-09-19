package com.foodario.inventory.domain.model

enum class QuantityUnit(
    val displayName: String,
) {
    UNIT("Unidad"),
    GRAMS("Gramos"),
    KILOGRAMS("Kilogramos"),
    MILLILITERS("Mililitros"),
    LITERS("Litros"),
}

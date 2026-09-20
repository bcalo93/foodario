package com.foodario.inventory.domain.model

enum class FoodCategory(
    val displayName: String,
) {
    DAIRY("Lácteos"),
    MEAT("Carnes"),
    VEGETABLES("Verduras"),
    FRUITS("Frutas"),
    PANTRY("Almacén"),
    FROZEN("Congelados"),
    BEVERAGES("Bebidas"),
    COOKED("Cocinados"),
    OTHER("Otros"),
}

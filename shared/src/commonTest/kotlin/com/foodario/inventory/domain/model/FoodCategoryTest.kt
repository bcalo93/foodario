package com.foodario.inventory.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class FoodCategoryTest {

    @Test
    fun `display names are localized`() {
        assertEquals(
            listOf(
                "Lácteos",
                "Carnes",
                "Verduras",
                "Frutas",
                "Almacén",
                "Congelados",
                "Bebidas",
                "Cocinados",
                "Otros",
            ),
            FoodCategory.entries.map { it.displayName },
        )
    }
}

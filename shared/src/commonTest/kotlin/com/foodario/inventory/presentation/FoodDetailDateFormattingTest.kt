package com.foodario.inventory.presentation

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class FoodDetailDateFormattingTest {

    @Test
    fun `formats expiration date with numeric day month and year`() {
        assertEquals("23/12/2026", formatDate(LocalDate(2026, 12, 23)))
    }
}

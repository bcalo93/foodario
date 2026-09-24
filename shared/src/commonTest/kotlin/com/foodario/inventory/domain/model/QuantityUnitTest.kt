package com.foodario.inventory.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class QuantityUnitTest {

    @Test
    fun `quantity unit steps match expected values`() {
        assertEquals(1.0, QuantityUnit.UNIT.step)
        assertEquals(50.0, QuantityUnit.GRAMS.step)
        assertEquals(0.1, QuantityUnit.KILOGRAMS.step)
        assertEquals(50.0, QuantityUnit.MILLILITERS.step)
        assertEquals(0.1, QuantityUnit.LITERS.step)
    }

    @Test
    fun `snapToStep rounds to nearest step`() {
        assertEquals(300.0, snapToStep(250.0 + 50.0, 50.0))
    }

    @Test
    fun `snapToStep clamps to zero when decrement goes below step`() {
        assertEquals(0.0, snapToStep((30.0 - 50.0).coerceAtLeast(0.0), 50.0))
    }

    @Test
    fun `snapToStep avoids floating point garbage`() {
        assertEquals(0.3, snapToStep(0.1 + 0.1 + 0.1, 0.1), 1e-9)
    }

    @Test
    fun `formatQuantityNumber drops trailing zeros for whole numbers`() {
        assertEquals("2", formatQuantityNumber(2.0))
        assertEquals("50", formatQuantityNumber(50.0))
    }

    @Test
    fun `formatQuantityNumber keeps decimals when needed`() {
        assertEquals("0.3", formatQuantityNumber(0.3))
    }
}

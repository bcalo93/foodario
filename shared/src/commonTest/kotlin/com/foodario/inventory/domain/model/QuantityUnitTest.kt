package com.foodario.inventory.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun `stepDecreaseQuantity subtracts one step`() {
        assertEquals(1.0, stepDecreaseQuantity(2.0, QuantityUnit.UNIT))
        assertEquals(250.0, stepDecreaseQuantity(300.0, QuantityUnit.GRAMS))
        assertEquals(0.9, stepDecreaseQuantity(1.0, QuantityUnit.KILOGRAMS), 1e-9)
    }

    @Test
    fun `stepDecreaseQuantity snaps non aligned quantities to the step`() {
        assertEquals(50.0, stepDecreaseQuantity(120.0, QuantityUnit.GRAMS))
    }

    @Test
    fun `stepDecreaseQuantity clamps to zero when quantity is not above step`() {
        assertEquals(0.0, stepDecreaseQuantity(1.0, QuantityUnit.UNIT))
        assertEquals(0.0, stepDecreaseQuantity(50.0, QuantityUnit.GRAMS))
        assertEquals(0.0, stepDecreaseQuantity(30.0, QuantityUnit.MILLILITERS))
    }

    @Test
    fun `canDecreaseStep is true only when result stays positive`() {
        assertTrue(canDecreaseStep(2.0, QuantityUnit.UNIT))
        assertTrue(canDecreaseStep(300.0, QuantityUnit.GRAMS))
        assertFalse(canDecreaseStep(1.0, QuantityUnit.UNIT))
        assertFalse(canDecreaseStep(50.0, QuantityUnit.GRAMS))
        assertFalse(canDecreaseStep(0.05, QuantityUnit.KILOGRAMS))
        assertFalse(canDecreaseStep(0.0, QuantityUnit.UNIT))
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

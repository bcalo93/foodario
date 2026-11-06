package com.foodario.scanning.domain

import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.scanning.domain.service.ReceiptParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReceiptParserTest {

    private val fixture = """
        SUPERMERCADO DIA S.A.
        CUIT 30-12345678-9
        Av. Siempre Viva 123
        10/01/2026 18:42        CAJA 3
        TICKET 0001-00012345
        LECHE ENTERA X 1LT        2 X   1.250,00
        0.485 KG PAN LACTAL             980,50
        YOGUR BEBIBLE FRUTILLA          1.100,00
        ARROZ LARGO FINO 500G           1.400,00
        COCA COLA 2.25LT          1 X   2.300,00
        SUBTOTAL                        7.030,50
        IVA 21%                           350,00
        TOTAL                           7.380,50
        EFECTIVO                       10.000,00
        VUELTO                          2.619,50
    """.trimIndent()

    @Test
    fun `parses item lines and drops noise`() {
        val items = ReceiptParser.parse(fixture)

        assertEquals(5, items.size)
        assertTrue(items.none { "TOTAL" in it.name || "IVA" in it.name || "SUBTOTAL" in it.name })
        assertTrue(items.none { "TOTAL" in it.rawLine || "IVA" in it.rawLine || "SUBTOTAL" in it.rawLine })
    }

    @Test
    fun `leading N x sets quantity`() {
        val items = ReceiptParser.parse(fixture)

        val leche = items.first { it.name.contains("Leche") }
        assertEquals(2.0, leche.quantity)
        assertEquals(QuantityUnit.UNIT, leche.unit)

        val coca = items.first { it.name.contains("Coca") }
        assertEquals(1.0, coca.quantity)
    }

    @Test
    fun `weight pattern sets quantity and unit`() {
        val items = ReceiptParser.parse(fixture)

        val pan = items.first { it.name.contains("Pan") }
        assertEquals(0.485, pan.quantity)
        assertEquals(QuantityUnit.KILOGRAMS, pan.unit)
        assertEquals("Pan Lactal", pan.name)
    }

    @Test
    fun `prices are stripped from names`() {
        val items = ReceiptParser.parse(fixture)

        assertTrue(items.isNotEmpty())
        assertTrue(items.none { it.name.any(Char::isDigit) })
    }

    @Test
    fun `category guessing`() {
        val items = ReceiptParser.parse(fixture)

        assertEquals(FoodCategory.DAIRY, items.first { it.name.contains("Leche") }.category)
        assertEquals(FoodCategory.PANTRY, items.first { it.name.contains("Arroz") }.category)
        assertEquals(FoodCategory.BEVERAGES, items.first { it.name.contains("Coca") }.category)
        assertEquals(FoodCategory.OTHER, items.first { it.name.contains("Pan") }.category)
    }

    @Test
    fun `garbage input returns empty list`() {
        assertEquals(emptyList(), ReceiptParser.parse(""))
        assertEquals(emptyList(), ReceiptParser.parse("12345 67890"))
        assertEquals(emptyList(), ReceiptParser.parse("TOTAL 100,00\nIVA 21%"))
    }

    @Test
    fun `lines with too few letters are dropped`() {
        assertTrue(ReceiptParser.parse("12 34 56").isEmpty())
    }
}

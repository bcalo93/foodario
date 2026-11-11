package com.foodario.scanning.domain.service

import com.foodario.scanning.domain.model.ScannedReceiptItem
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.core.domain.matching.ProductNameMatcher

object ReceiptParser {

    fun parse(rawText: String): List<ScannedReceiptItem> =
        rawText.lines()
            .map { it.trim() }
            .filter { it.isItemLine() }
            .mapNotNull { it.toScannedItem() }

    private val NOISE_KEYWORDS = listOf(
        "TOTAL", "SUBTOTAL", "IVA", "CUIT", "C.U.I.T", "EFECTIVO", "TARJETA", "DEBITO", "CREDITO",
        "CAMBIO", "VUELTO", "TICKET", "FACTURA", "ORIGINAL", "DUPLICADO", "CAJA", "CAJERO",
        "SUCURSAL", "DOMICILIO", "TEL", "CLIENTE", "FECHA", "HORA", "REMITO", "LOTE",
        "SUPERMERCADO", "AVENIDA", "AV.", "DIRECCION",
    )
    private val DATE_REGEX = Regex("\\d{2}[/\\-.]\\d{2}[/\\-.]\\d{2,4}")
    private val TIME_REGEX = Regex("\\d{2}:\\d{2}(:\\d{2})?")

    internal fun String.isItemLine(): Boolean {
        if (length < 4) return false
        val upper = uppercase()
        if (NOISE_KEYWORDS.any { upper.contains(it) }) return false
        if (DATE_REGEX.containsMatchIn(this) || TIME_REGEX.containsMatchIn(this)) return false
        val letters = count { it.isLetter() }
        if (letters < 3) return false
        val nonBlank = count { !it.isWhitespace() }
        if (nonBlank == 0 || letters.toDouble() / nonBlank < 0.4) return false
        return true
    }

    private val QTY_REGEX = Regex("(\\d{1,3})\\s*[xX]\\s+")
    private val UNIT_AMOUNT_REGEX = Regex("(\\d+(?:[.,]\\d{1,3})?)\\s*(KG|G|GR|GRS|LT|L|ML)\\b", RegexOption.IGNORE_CASE)
    private val PRICE_REGEX = Regex("\\$?\\s*\\d{1,3}(\\.\\d{3})*,\\d{2}\\b")
    private val TRAILING_PRICE_REGEX = Regex("\\d+[.,]\\d{2}\\s*$")
    private val CODE_REGEX = Regex("^\\d{5,}\\s+")

    internal fun String.toScannedItem(): ScannedReceiptItem? {
        var working = this

        var quantity = 1.0
        var unit = QuantityUnit.UNIT
        var hasExplicitQuantity = false

        QTY_REGEX.find(working)?.let { m ->
            quantity = m.groupValues[1].toDouble()
            hasExplicitQuantity = true
            working = working.removeRange(m.range)
        }
        UNIT_AMOUNT_REGEX.find(working)?.let { m ->
            val amount = m.groupValues[1]
            if (!hasExplicitQuantity && (amount.contains('.') || amount.contains(','))) {
                quantity = amount.replace(',', '.').toDouble()
                unit = when (m.groupValues[2].uppercase()) {
                    "KG" -> QuantityUnit.KILOGRAMS
                    "G", "GR", "GRS" -> QuantityUnit.GRAMS
                    "LT", "L" -> QuantityUnit.LITERS
                    "ML" -> QuantityUnit.MILLILITERS
                    else -> QuantityUnit.UNIT
                }
            }
            working = working.removeRange(m.range)
        }

        val name = working
            .replace(PRICE_REGEX, " ")
            .replace(TRAILING_PRICE_REGEX, " ")
            .replace(CODE_REGEX, " ")
            .replace(Regex("[^A-Za-zÁÉÍÓÚÜÑáéíóúüñ\\s]"), " ")
            .replace(Regex("\\bX\\b", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
            .split(' ')
            .filter { it.isNotEmpty() }
            .joinToString(" ") { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() } }

        if (name.length < 3 || name.count { it.isLetter() } < 3) return null
        return ScannedReceiptItem(
            name = name,
            category = guessCategory(name),
            quantity = quantity,
            unit = unit,
            rawLine = this,
        )
    }

    private val CATEGORY_KEYWORDS: Map<FoodCategory, Set<String>> = mapOf(
        FoodCategory.DAIRY to setOf("leche", "yogur", "queso", "manteca", "crema", "ricota"),
        FoodCategory.MEAT to setOf("carne", "pollo", "picada", "jamon", "chorizo", "mortadela", "milanesa", "pescado", "vacio", "asado"),
        FoodCategory.VEGETABLES to setOf("papa", "tomate", "cebolla", "lechuga", "zanahoria", "zapallo", "espinaca", "morron", "verdeo"),
        FoodCategory.FRUITS to setOf("banana", "manzana", "naranja", "pera", "limon", "mandarina", "uva", "frutilla", "durazno", "pomelo"),
        FoodCategory.PANTRY to setOf("arroz", "fideos", "harina", "azucar", "sal", "aceite", "yerba", "te", "cafe", "lentejas", "porotos", "atun", "galletitas", "salsa", "pure"),
        FoodCategory.FROZEN to setOf("congelado", "congelada", "congelados", "frozen"),
        FoodCategory.BEVERAGES to setOf("agua", "gaseosa", "jugo", "cerveza", "vino", "soda", "coca", "sprite", "fanta"),
    )

    internal fun guessCategory(name: String): FoodCategory {
        val tokens = ProductNameMatcher.significantTokensOf(name).toSet()
        return CATEGORY_KEYWORDS.entries.firstOrNull { (_, keywords) -> tokens.any { it in keywords } }
            ?.key ?: FoodCategory.OTHER
    }
}

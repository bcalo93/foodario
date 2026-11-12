package com.foodario.core.domain.matching

import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class ProductNameMatcherTest {

    private fun foodItem(
        name: String,
        category: FoodCategory = FoodCategory.OTHER,
    ) = FoodItem(
        id = 0L,
        name = name,
        category = category,
        quantity = 1.0,
        unit = QuantityUnit.UNIT,
        isFrozen = false,
        expirationDate = null,
        createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    )

    @Test
    fun `normalize folds accents case and punctuation`() {
        assertEquals("leche ent 1l", ProductNameMatcher.normalize("  LECHE® Ent. 1L! "))
    }

    @Test
    fun `significantTokens drops stopwords unit tokens short and numeric tokens`() {
        val tokens = ProductNameMatcher.significantTokens(
            ProductNameMatcher.normalize("2 x leche de la casa 500g"),
        )
        assertFalse("2" in tokens)
        assertFalse("x" in tokens)
        assertFalse("de" in tokens)
        assertFalse("la" in tokens)
        assertFalse("500g" in tokens)
    }

    @Test
    fun `exact match wins over partial`() {
        val candidates = listOf(
            foodItem("Leche"),
            foodItem("Leche condensada"),
        )

        val match = ProductNameMatcher.findBestMatch("leche", candidates)

        assertEquals(MatchConfidence.EXACT, match?.confidence)
        assertEquals("Leche", match?.item?.name)
    }

    @Test
    fun `partial match on token containment`() {
        val leche = listOf(foodItem("Leche"))
        val yogur = listOf(foodItem("Yogur"))

        assertEquals(MatchConfidence.PARTIAL, ProductNameMatcher.findBestMatch("LECHE X 1LT", leche)?.confidence)
        assertEquals(MatchConfidence.PARTIAL, ProductNameMatcher.findBestMatch("Yogur Bebible", yogur)?.confidence)
    }

    @Test
    fun `no match when tokens differ`() {
        val candidates = listOf(foodItem("Fideos"))

        assertTrue(ProductNameMatcher.findMatches("Arroz", candidates).isEmpty())
    }

    @Test
    fun `blank query returns empty`() {
        val candidates = listOf(foodItem("Leche"))

        assertTrue(ProductNameMatcher.findMatches("   ", candidates).isEmpty())
    }
}

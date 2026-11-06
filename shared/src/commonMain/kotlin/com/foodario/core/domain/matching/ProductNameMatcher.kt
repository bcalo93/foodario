package com.foodario.core.domain.matching

import com.foodario.inventory.domain.model.FoodItem

enum class MatchConfidence { EXACT, PARTIAL }

data class ProductMatch(
    val item: FoodItem,
    val confidence: MatchConfidence,
)

/**
 * Matching anti-duplicados por nombre. Puro y determinista (sin dependencias de plataforma).
 *
 * Normalización KMP-safe: NO usar java.text.Normalizer (no existe en commonMain);
 * el folding de tildes se hace con un mapa fijo para español.
 */
object ProductNameMatcher {

    private val ACCENT_MAP = mapOf(
        'á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u',
        'à' to 'a', 'è' to 'e', 'ì' to 'i', 'ò' to 'o', 'ù' to 'u',
        'ü' to 'u', 'ñ' to 'n',
    )

    private val STOPWORDS = setOf(
        "de", "del", "la", "el", "los", "las", "en", "con", "sin", "por", "para", "x",
    )

    // Tokens de unidad/packaging típicos de tickets; no aportan identidad de producto.
    private val UNIT_TOKENS = setOf("kg", "gr", "grs", "lt", "ml", "cc", "un", "pack")

    fun normalize(raw: String): String =
        raw.lowercase()
            .map { ACCENT_MAP[it] ?: it }
            .joinToString("")
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    internal fun significantTokens(normalized: String): List<String> =
        normalized.split(' ')
            .filter { it.length >= 3 }
            .filterNot { it.any(Char::isDigit) }
            .filterNot { it in STOPWORDS || it in UNIT_TOKENS }

    fun findMatches(query: String, candidates: List<FoodItem>): List<ProductMatch> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank()) return emptyList()

        val exact = candidates
            .filter { normalize(it.name) == normalizedQuery }
            .map { ProductMatch(it, MatchConfidence.EXACT) }
        if (exact.isNotEmpty()) return exact

        val queryTokens = significantTokens(normalizedQuery).toSet()
        if (queryTokens.isEmpty()) return emptyList()

        return candidates.mapNotNull { item ->
            val itemTokens = significantTokens(normalize(item.name)).toSet()
            if (itemTokens.isEmpty()) return@mapNotNull null
            val shared = queryTokens intersect itemTokens
            if (shared.isEmpty()) return@mapNotNull null
            val containment = shared.size.toDouble() / minOf(queryTokens.size, itemTokens.size)
            if (containment >= 0.6) ProductMatch(item, MatchConfidence.PARTIAL) else null
        }.sortedBy { it.item.name.lowercase() }
    }

    fun findBestMatch(query: String, candidates: List<FoodItem>): ProductMatch? =
        findMatches(query, candidates).firstOrNull()
}

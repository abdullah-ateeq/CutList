package com.example.cutlist.domain

import java.math.BigDecimal

/** Safe parsing of user-entered numbers. Never produces NaN, Infinity or exponent notation. */
object NumberParser {
    private const val MAX_INPUT_LENGTH = 16
    private val DECIMAL_PATTERN = Regex("""^-?(\d+\.?\d*|\.\d+)$""")
    private val INTEGER_PATTERN = Regex("""^-?\d+$""")

    /** Parses plain decimal text; accepts "," as decimal separator. Returns null when malformed. */
    fun parseDecimal(text: String): BigDecimal? {
        val normalized = text.trim().replace(',', '.')
        if (normalized.isEmpty() || normalized.length > MAX_INPUT_LENGTH) return null
        if (!DECIMAL_PATTERN.matches(normalized)) return null
        return try {
            BigDecimal(normalized.removeSuffix("."))
        } catch (_: NumberFormatException) {
            null
        }
    }

    /** Parses a whole number. Returns null for decimals, malformed text or values outside Int range. */
    fun parseWholeNumber(text: String): Int? {
        val normalized = text.trim()
        if (normalized.isEmpty() || normalized.length > 10) return null
        if (!INTEGER_PATTERN.matches(normalized)) return null
        return normalized.toLongOrNull()?.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
    }
}

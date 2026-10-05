package com.example.cutlist.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** Centralized display formatting for lengths, dimensions and areas. */
object MeasurementFormatter {
    const val MIN_DECIMALS = 0
    const val MAX_DECIMALS = 4

    private val METRIC_M2_THRESHOLD = BigDecimal(100_000) // 0.1 m²
    private val METRIC_CM2_THRESHOLD = BigDecimal(100) // 1 cm²
    private val IMPERIAL_FT2_THRESHOLD = BigDecimal("92903.04") // 1 ft²

    /**
     * Rounds to [decimals] places. Whole results are shown without a fraction ("1200", not "1200.00");
     * fractional results keep the fixed number of places ("1.20").
     */
    fun formatNumber(value: BigDecimal, decimals: Int): String {
        val places = decimals.coerceIn(MIN_DECIMALS, MAX_DECIMALS)
        val rounded = value.setScale(places, RoundingMode.HALF_UP)
        val isWhole = rounded.signum() == 0 || rounded.stripTrailingZeros().scale() <= 0
        return if (isWhole) rounded.setScale(0, RoundingMode.HALF_UP).toPlainString() else rounded.toPlainString()
    }

    /** Rounds to exactly [decimals] places (used for areas, e.g. "5.20 m²"). */
    fun formatFixed(value: BigDecimal, decimals: Int): String =
        value.setScale(decimals.coerceIn(MIN_DECIMALS, MAX_DECIMALS), RoundingMode.HALF_UP).toPlainString()

    fun formatValue(millimeters: BigDecimal, unit: LengthUnit, decimals: Int): String =
        formatNumber(unit.fromMillimeters(millimeters), decimals)

    fun formatLength(millimeters: BigDecimal, unit: LengthUnit, decimals: Int): String =
        "${formatValue(millimeters, unit, decimals)} ${unit.symbol}"

    /** Always Length × Width, never swapped: "1200 × 300 mm". */
    fun formatDimensions(lengthMm: BigDecimal, widthMm: BigDecimal, unit: LengthUnit, decimals: Int): String =
        "${formatValue(lengthMm, unit, decimals)} × ${formatValue(widthMm, unit, decimals)} ${unit.symbol}"

    fun spokenDimensions(lengthMm: BigDecimal, widthMm: BigDecimal, unit: LengthUnit, decimals: Int): String =
        "${formatValue(lengthMm, unit, decimals)} by ${formatValue(widthMm, unit, decimals)} ${unit.spokenName}"

    fun spokenLength(millimeters: BigDecimal, unit: LengthUnit, decimals: Int): String =
        "${formatValue(millimeters, unit, decimals)} ${unit.spokenName}"

    data class FormattedArea(val value: String, val symbol: String, val spokenUnit: String) {
        val text: String get() = "$value $symbol"
        val spoken: String get() = "$value $spokenUnit"
    }

    /**
     * Picks a readable area unit by magnitude: m² / cm² / mm² for metric display units,
     * ft² / in² for imperial ones.
     */
    fun formatArea(squareMillimeters: BigDecimal, unit: LengthUnit, decimals: Int): FormattedArea {
        val large = decimals.coerceAtLeast(2)
        return if (unit.isImperial) {
            when {
                squareMillimeters.signum() == 0 ||
                    squareMillimeters >= IMPERIAL_FT2_THRESHOLD -> FormattedArea(
                    formatFixed(AreaConversions.toSquareFeet(squareMillimeters), large), "ft²", "square feet"
                )
                else -> FormattedArea(
                    formatFixed(AreaConversions.toSquareInches(squareMillimeters), decimals), "in²", "square inches"
                )
            }
        } else {
            when {
                squareMillimeters.signum() == 0 ||
                    squareMillimeters >= METRIC_M2_THRESHOLD -> FormattedArea(
                    formatFixed(AreaConversions.toSquareMeters(squareMillimeters), large), "m²", "square meters"
                )
                squareMillimeters >= METRIC_CM2_THRESHOLD -> FormattedArea(
                    formatFixed(AreaConversions.toSquareCentimeters(squareMillimeters), decimals),
                    "cm²", "square centimeters"
                )
                else -> FormattedArea(formatFixed(squareMillimeters, decimals), "mm²", "square millimeters")
            }
        }
    }

}

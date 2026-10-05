package com.example.cutlist.domain

import java.math.BigDecimal
import java.math.MathContext

/**
 * Supported entry/display units. Every dimension is stored internally in millimeters;
 * these functions are the only place unit conversion happens.
 */
enum class LengthUnit(
    val symbol: String,
    val spokenName: String,
    val millimetersPerUnit: BigDecimal,
    val isImperial: Boolean,
) {
    MM("mm", "millimeters", BigDecimal.ONE, false),
    CM("cm", "centimeters", BigDecimal.TEN, false),
    M("m", "meters", BigDecimal(1000), false),
    IN("in", "inches", BigDecimal("25.4"), true),
    FT("ft", "feet", BigDecimal("304.8"), true);

    /** Converts a value expressed in this unit to millimeters (exact). */
    fun toMillimeters(value: BigDecimal): BigDecimal = value.multiply(millimetersPerUnit)

    /** Converts millimeters to this unit. Inexact divisions keep 20 significant digits. */
    fun fromMillimeters(millimeters: BigDecimal): BigDecimal =
        millimeters.divide(millimetersPerUnit, CONVERSION_CONTEXT)

    companion object {
        val CONVERSION_CONTEXT: MathContext = MathContext(20)

        fun fromNameOrDefault(name: String?, default: LengthUnit = MM): LengthUnit =
            entries.firstOrNull { it.name == name } ?: default
    }
}

/** Area conversions from square millimeters. */
object AreaConversions {
    private val MM2_PER_FT2 = BigDecimal("92903.04")
    private val MM2_PER_IN2 = BigDecimal("645.16")

    fun toSquareMeters(squareMillimeters: BigDecimal): BigDecimal = squareMillimeters.movePointLeft(6)

    fun toSquareCentimeters(squareMillimeters: BigDecimal): BigDecimal = squareMillimeters.movePointLeft(2)

    fun toSquareFeet(squareMillimeters: BigDecimal): BigDecimal =
        squareMillimeters.divide(MM2_PER_FT2, LengthUnit.CONVERSION_CONTEXT)

    fun toSquareInches(squareMillimeters: BigDecimal): BigDecimal =
        squareMillimeters.divide(MM2_PER_IN2, LengthUnit.CONVERSION_CONTEXT)
}

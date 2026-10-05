package com.example.cutlist.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class UnitConversionTest {

    @Test
    fun millimeters() {
        assertNumber("1200", LengthUnit.MM.toMillimeters(BigDecimal("1200")))
        assertNumber("1200", LengthUnit.MM.fromMillimeters(BigDecimal("1200")))
    }

    @Test
    fun centimeters() {
        assertNumber("1200", LengthUnit.CM.toMillimeters(BigDecimal("120")))
        assertNumber("120", LengthUnit.CM.fromMillimeters(BigDecimal("1200")))
    }

    @Test
    fun meters() {
        assertNumber("1200", LengthUnit.M.toMillimeters(BigDecimal("1.2")))
        assertNumber("1.2", LengthUnit.M.fromMillimeters(BigDecimal("1200")))
    }

    @Test
    fun inches() {
        assertNumber("304.8", LengthUnit.IN.toMillimeters(BigDecimal("12")))
        assertNumber("12", LengthUnit.IN.fromMillimeters(BigDecimal("304.8")))
        assertEquals("1", MeasurementFormatter.formatValue(BigDecimal("25.4"), LengthUnit.IN, 2))
    }

    @Test
    fun feet() {
        assertNumber("304.8", LengthUnit.FT.toMillimeters(BigDecimal.ONE))
        assertNumber("2", LengthUnit.FT.fromMillimeters(BigDecimal("609.6")))
        assertNumber("1", LengthUnit.FT.fromMillimeters(LengthUnit.IN.toMillimeters(BigDecimal(12))))
    }

    @Test
    fun switchingUnits_preservesPhysicalSize() {
        // 120 cm entered, then displayed in m: 1.20 m (not 120 m).
        val mm = LengthUnit.CM.toMillimeters(BigDecimal("120"))
        assertEquals("1.20 m", MeasurementFormatter.formatLength(mm, LengthUnit.M, 2))
        assertEquals("1200 mm", MeasurementFormatter.formatLength(mm, LengthUnit.MM, 2))
        assertEquals("47.24 in", MeasurementFormatter.formatLength(mm, LengthUnit.IN, 2))
        assertEquals("3.94 ft", MeasurementFormatter.formatLength(mm, LengthUnit.FT, 2))
    }

    @Test
    fun areaConversions() {
        assertNumber("0.25", AreaConversions.toSquareMeters(BigDecimal(250_000)))
        assertNumber("2500", AreaConversions.toSquareCentimeters(BigDecimal(250_000)))
        assertNumber("1", AreaConversions.toSquareFeet(BigDecimal("92903.04")))
        assertNumber("1", AreaConversions.toSquareInches(BigDecimal("645.16")))
    }

    @Test
    fun unknownUnitName_fallsBackToDefault() {
        assertEquals(LengthUnit.MM, LengthUnit.fromNameOrDefault("parsecs"))
        assertEquals(LengthUnit.FT, LengthUnit.fromNameOrDefault("FT"))
    }
}

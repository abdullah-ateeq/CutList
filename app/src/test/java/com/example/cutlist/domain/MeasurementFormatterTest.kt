package com.example.cutlist.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MeasurementFormatterTest {

    @Test
    fun wholeNumbers_haveNoTrailingDecimals() {
        assertEquals("1200", MeasurementFormatter.formatNumber(BigDecimal("1200"), 2))
        assertEquals("1200", MeasurementFormatter.formatNumber(BigDecimal("1200.00"), 0))
        assertEquals("0", MeasurementFormatter.formatNumber(BigDecimal("0.001"), 2))
    }

    @Test
    fun fractions_useFixedPrecision() {
        assertEquals("1.20", MeasurementFormatter.formatNumber(BigDecimal("1.2"), 2))
        assertEquals("1.2", MeasurementFormatter.formatNumber(BigDecimal("1.2"), 1))
        assertEquals("1", MeasurementFormatter.formatNumber(BigDecimal("1.2"), 0))
        assertEquals("3.1416", MeasurementFormatter.formatNumber(BigDecimal("3.14159"), 4))
        assertEquals("3.1416", MeasurementFormatter.formatNumber(BigDecimal("3.14159"), 9))
    }

    @Test
    fun dimensions_alwaysLengthByWidth() {
        assertEquals(
            "1200 × 300 mm",
            MeasurementFormatter.formatDimensions(BigDecimal(1200), BigDecimal(300), LengthUnit.MM, 2),
        )
        assertEquals(
            "300 × 1200 mm",
            MeasurementFormatter.formatDimensions(BigDecimal(300), BigDecimal(1200), LengthUnit.MM, 2),
        )
        assertEquals(
            "120 by 30 centimeters",
            MeasurementFormatter.spokenDimensions(BigDecimal(1200), BigDecimal(300), LengthUnit.CM, 2),
        )
    }

    @Test
    fun area_picksReadableUnit() {
        assertEquals("0.72 m²", MeasurementFormatter.formatArea(BigDecimal(720_000), LengthUnit.MM, 2).text)
        assertEquals("2.16 m²", MeasurementFormatter.formatArea(BigDecimal(2_160_000), LengthUnit.CM, 2).text)
        assertEquals("0.25 m²", MeasurementFormatter.formatArea(BigDecimal(250_000), LengthUnit.MM, 2).text)
        assertEquals("5.20 m²", MeasurementFormatter.formatArea(BigDecimal(5_200_000), LengthUnit.M, 2).text)
        assertEquals("150.00 cm²", MeasurementFormatter.formatArea(BigDecimal(15_000), LengthUnit.MM, 2).text)
        assertEquals("50.00 mm²", MeasurementFormatter.formatArea(BigDecimal(50), LengthUnit.MM, 2).text)
        assertEquals("0.00 m²", MeasurementFormatter.formatArea(BigDecimal.ZERO, LengthUnit.MM, 2).text)
        assertEquals("2.50 ft²", MeasurementFormatter.formatArea(BigDecimal("232257.6"), LengthUnit.IN, 2).text)
        assertEquals("1.00 in²", MeasurementFormatter.formatArea(BigDecimal("645.16"), LengthUnit.FT, 2).text)
        assertEquals("0.72 square meters", MeasurementFormatter.formatArea(BigDecimal(720_000), LengthUnit.MM, 0).spoken)
    }

    @Test
    fun output_neverContainsNaNOrInfinity() {
        val values = listOf("0", "0.0001", "123456789.123456", "1E+12").map(::BigDecimal)
        for (value in values) {
            for (unit in LengthUnit.entries) {
                val text = MeasurementFormatter.formatLength(value, unit, 2) +
                    MeasurementFormatter.formatArea(value, unit, 2).text
                assertFalse(text, text.contains("NaN") || text.contains("Infinity") || text.contains("E"))
            }
        }
    }

    @Test
    fun copyText_matchesSpecFormat() {
        val project = CutListProject(
            id = "p", name = "Wall Shelf", unit = LengthUnit.MM, createdAt = 0, updatedAt = 0,
            pieces = listOf(
                piece("Top Shelf", "1200", "300", 2),
                piece("Side", "800", "300", 2),
                piece("Back", "1200", "800", 1, notes = "Paint visible side"),
            ),
        )
        val rows = CutListCalculator.buildRows(project.pieces, consolidate = true)
        val text = CutListTextFormatter.build(
            project, rows, CutListCalculator.summarize(project.pieces),
            CutListTextFormatter.Options(decimals = 2, showArea = true, showCutOrder = true),
        )
        assertTrue(text.startsWith("CUT LIST\nProject: Wall Shelf\n"))
        assertTrue(text.contains("1. Top Shelf — 1200 × 300 mm — Qty 2"))
        assertTrue(text.contains("2. Side — 800 × 300 mm — Qty 2"))
        assertTrue(text.contains("3. Back — 1200 × 800 mm — Qty 1"))
        assertTrue(text.contains("\"Paint visible side\""))
        assertTrue(text.contains("Total pieces: 5"))
        assertTrue(text.contains("Total area: 2.16 m²"))
        assertTrue(text.contains("Total piece length: 5200 mm"))
    }

    @Test
    fun untitledProject_usesDefaultName() {
        val project = CutListProject("p", "   ", LengthUnit.MM, emptyList(), 0, 0)
        assertEquals("Untitled Cut List", project.displayName)
    }
}

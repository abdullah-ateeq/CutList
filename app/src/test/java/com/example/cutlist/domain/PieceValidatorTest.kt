package com.example.cutlist.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PieceValidatorTest {

    private val valid = PieceInput(name = "Top shelf", length = "1200", width = "300", quantity = "2")

    private fun errors(input: PieceInput, unit: LengthUnit = LengthUnit.MM): PieceErrors {
        val result = PieceValidator.validate(input, unit, "id")
        assertTrue("expected invalid", result is PieceValidationResult.Invalid)
        return (result as PieceValidationResult.Invalid).errors
    }

    private fun validPiece(input: PieceInput, unit: LengthUnit = LengthUnit.MM): CutPiece {
        val result = PieceValidator.validate(input, unit, "id")
        assertTrue("expected valid but got $result", result is PieceValidationResult.Valid)
        return (result as PieceValidationResult.Valid).piece
    }

    @Test
    fun validInput_producesPieceInMillimeters() {
        val p = validPiece(valid.copy(length = "120", width = "30"), LengthUnit.CM)
        assertNumber("1200", p.lengthMm)
        assertNumber("300", p.widthMm)
        assertEquals(2, p.quantity)
        assertNull(p.thicknessMm)
        assertNull(p.material)
        assertNull(p.notes)
    }

    @Test
    fun emptyName_isRejected() {
        assertEquals("Enter a piece name.", errors(valid.copy(name = "  ")).name)
    }

    @Test
    fun zeroLength_isRejected() {
        assertEquals("Length must be greater than zero.", errors(valid.copy(length = "0")).length)
        assertEquals("Length must be greater than zero.", errors(valid.copy(length = "0.000")).length)
        assertEquals("Length must be greater than zero.", errors(valid.copy(length = "")).length)
    }

    @Test
    fun zeroWidth_isRejected() {
        assertEquals("Width must be greater than zero.", errors(valid.copy(width = "0")).width)
    }

    @Test
    fun zeroQuantity_isRejected() {
        assertEquals("Quantity must be at least 1.", errors(valid.copy(quantity = "0")).quantity)
        assertEquals("Quantity must be at least 1.", errors(valid.copy(quantity = "")).quantity)
    }

    @Test
    fun quantity_mustBeWholeAndAtMost1000() {
        assertEquals("Quantity must be a whole number.", errors(valid.copy(quantity = "1.5")).quantity)
        assertEquals("Quantity cannot exceed 1000.", errors(valid.copy(quantity = "1001")).quantity)
        assertEquals(1000, validPiece(valid.copy(quantity = "1000")).quantity)
        assertEquals(1, validPiece(valid.copy(quantity = "1")).quantity)
    }

    @Test
    fun negativeValues_areRejected() {
        val e = errors(valid.copy(length = "-5", width = "-0.1", quantity = "-3", thickness = "-18"))
        assertEquals("Length must be greater than zero.", e.length)
        assertEquals("Width must be greater than zero.", e.width)
        assertEquals("Quantity must be at least 1.", e.quantity)
        assertEquals("Thickness must be greater than zero.", e.thickness)
    }

    @Test
    fun malformedNumbers_nanAndInfinity_areRejected() {
        listOf("NaN", "Infinity", "-Infinity", "1e5", "12..5", "1.2.3", "abc", "0x10", "1,2,3", " . ").forEach { text ->
            assertEquals("for '$text'", "Enter a valid number.", errors(valid.copy(length = text)).length)
        }
        assertNull(NumberParser.parseDecimal("NaN"))
        assertNull(NumberParser.parseDecimal("Infinity"))
        assertNull(NumberParser.parseWholeNumber("99999999999"))
    }

    @Test
    fun decimalInput_acceptsCommaAndLeadingDot() {
        assertNumber("12.5", validPiece(valid.copy(length = "12,5")).lengthMm)
        assertNumber("0.5", validPiece(valid.copy(width = ".5")).widthMm)
        assertNumber("12", validPiece(valid.copy(length = "12.")).lengthMm)
    }

    @Test
    fun thickness_optionalButPositiveWhenEntered() {
        assertNull(validPiece(valid.copy(thickness = "")).thicknessMm)
        assertNumber("18", validPiece(valid.copy(thickness = "18")).thicknessMm!!)
        assertNumber("19.05", validPiece(valid.copy(thickness = "0.75"), LengthUnit.IN).thicknessMm!!)
        assertEquals("Thickness must be greater than zero.", errors(valid.copy(thickness = "0")).thickness)
    }

    @Test
    fun extremeDimensions_areBounded() {
        assertNumber("100000", validPiece(valid.copy(length = "100", width = "0.3"), LengthUnit.M).lengthMm)
        assertEquals("Length is too large (max 100 m).", errors(valid.copy(length = "100001")).length)
        assertNumber("0.001", validPiece(valid.copy(width = "0.001")).widthMm)
    }

    @Test
    fun optionalText_isTrimmedAndBlankBecomesNull() {
        val p = validPiece(valid.copy(material = "  Plywood ", notes = "   "))
        assertEquals("Plywood", p.material)
        assertNull(p.notes)
    }

    @Test
    fun toInput_roundTripsWithoutDisplayRounding() {
        val p = validPiece(valid.copy(length = "47.25", width = "11.875"), LengthUnit.IN)
        val input = PieceValidator.toInput(p, LengthUnit.IN)
        assertEquals("47.25", input.length)
        assertEquals("11.875", input.width)
        assertEquals("1200.15", PieceValidator.toInput(p, LengthUnit.MM).length)
    }
}

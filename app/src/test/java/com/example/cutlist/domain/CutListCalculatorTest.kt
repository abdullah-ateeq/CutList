package com.example.cutlist.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CutListCalculatorTest {

    private val shelf = piece("Shelf", "1200", "300", 2)
    private val side = piece("Side", "800", "300", 2)
    private val back = piece("Back", "1200", "800", 1)

    @Test
    fun singlePiece_example1() {
        val pieces = listOf(shelf)
        assertEquals(2, CutListCalculator.calculateTotalPieces(pieces))
        assertNumber("720000", CutListCalculator.calculateTotalArea(pieces))
        assertNumber("0.72", AreaConversions.toSquareMeters(CutListCalculator.calculateTotalArea(pieces)))
        assertNumber("2400", CutListCalculator.calculateTotalPieceLength(pieces))
    }

    @Test
    fun multiplePieces_example2() {
        val pieces = listOf(shelf, side, back)
        assertEquals(5, CutListCalculator.calculateTotalPieces(pieces))
        assertEquals(3, CutListCalculator.calculateUniqueItems(pieces))
        assertNumber("2160000", CutListCalculator.calculateTotalArea(pieces))
        assertNumber("2.16", AreaConversions.toSquareMeters(CutListCalculator.calculateTotalArea(pieces)))
        assertNumber("5200", CutListCalculator.calculateTotalPieceLength(pieces))
    }

    @Test
    fun pieceArea_isLengthTimesWidth_andTotalIncludesQuantity() {
        assertNumber("360000", CutListCalculator.calculatePieceArea(shelf))
        assertNumber("720000", CutListCalculator.calculatePieceTotalArea(shelf))
    }

    @Test
    fun quantityMultipliesAreaAndLength() {
        val one = piece("A", "500", "200", 1)
        val seven = one.copy(quantity = 7)
        assertNumber(
            CutListCalculator.calculateTotalArea(listOf(one)).multiply(BigDecimal(7)).toPlainString(),
            CutListCalculator.calculateTotalArea(listOf(seven)),
        )
        assertNumber("3500", CutListCalculator.calculateTotalPieceLength(listOf(seven)))
    }

    @Test
    fun totalPieces_sumsQuantities() {
        assertEquals(0, CutListCalculator.calculateTotalPieces(emptyList()))
        assertEquals(1005, CutListCalculator.calculateTotalPieces(listOf(shelf, piece("X", "1", "1", 1000), back, side)))
    }

    @Test
    fun emptyList_givesZeroTotals() {
        val summary = CutListCalculator.summarize(emptyList())
        assertEquals(0, summary.totalPieces)
        assertEquals(0, summary.uniqueItems)
        assertNumber("0", summary.totalAreaMm2)
        assertNumber("0", summary.totalPieceLengthMm)
    }

    @Test
    fun decimalDimensions_areExact() {
        val p = piece("Strip", "1200.5", "30.25", 3)
        assertNumber("108945.375", CutListCalculator.calculateTotalArea(listOf(p)))
        assertNumber("3601.5", CutListCalculator.calculateTotalPieceLength(listOf(p)))
    }

    @Test
    fun inchDimensions_noIntermediateRounding() {
        // 3 pieces of 1/3 inch is not representable, but 12 in × 10 in is: 304.8 × 254 = 77419.2 mm².
        val p = CutPiece(
            id = "a", name = "Panel",
            lengthMm = LengthUnit.IN.toMillimeters(BigDecimal(12)),
            widthMm = LengthUnit.IN.toMillimeters(BigDecimal(10)),
            quantity = 3,
        )
        assertNumber("232257.6", CutListCalculator.calculateTotalArea(listOf(p)))
        assertNumber("2.5", AreaConversions.toSquareFeet(CutListCalculator.calculateTotalArea(listOf(p))))
    }

    @Test
    fun thickness_groupedAndMissingNotAssumed() {
        val pieces = listOf(
            piece("A", "100", "100", 3, thicknessMm = "18"),
            piece("B", "100", "100", 2, thicknessMm = "18.0"),
            piece("C", "100", "100", 2, thicknessMm = "12"),
            piece("D", "100", "100", 4),
        )
        val groups = CutListCalculator.groupByThickness(pieces)
        assertEquals(3, groups.size)
        assertNumber("18", groups[0].thicknessMm!!)
        assertEquals(5, groups[0].pieceCount)
        assertNumber("12", groups[1].thicknessMm!!)
        assertEquals(2, groups[1].pieceCount)
        assertNull(groups[2].thicknessMm)
        assertEquals(4, groups[2].pieceCount)
    }

    @Test
    fun thickness_notUsedInArea() {
        val thin = piece("A", "1000", "500", 1, thicknessMm = "3")
        val thick = piece("A", "1000", "500", 1, thicknessMm = "40")
        assertEquals(
            0,
            CutListCalculator.calculateTotalArea(listOf(thin)).compareTo(CutListCalculator.calculateTotalArea(listOf(thick))),
        )
    }

    @Test
    fun material_groupedCaseInsensitively() {
        val pieces = listOf(
            piece("A", "100", "100", 4, material = "Plywood"),
            piece("B", "100", "100", 4, material = "plywood "),
            piece("C", "100", "100", 4, material = "MDF"),
            piece("D", "100", "100", 1),
        )
        val groups = CutListCalculator.groupByMaterial(pieces)
        assertEquals(listOf("Plywood", "MDF", null), groups.map { it.material })
        assertEquals(listOf(8, 4, 1), groups.map { it.pieceCount })
    }

    @Test
    fun consolidation_mergesOnlyTrueDuplicates() {
        val a = piece("Shelf", "1200", "300", 2, thicknessMm = "18", material = "Plywood", id = "1")
        val sameAgain = piece("shelf", "1200.0", "300", 1, thicknessMm = "18", material = "plywood", id = "2")
        val differentName = piece("Door", "1200", "300", 1, thicknessMm = "18", material = "Plywood", id = "3")
        val differentNotes = piece("Shelf", "1200", "300", 1, thicknessMm = "18", material = "Plywood", notes = "Edge band", id = "4")
        val differentMaterial = piece("Shelf", "1200", "300", 1, thicknessMm = "18", material = "MDF", id = "5")
        val differentThickness = piece("Shelf", "1200", "300", 1, thicknessMm = "12", material = "Plywood", id = "6")
        val pieces = listOf(a, sameAgain, differentName, differentNotes, differentMaterial, differentThickness)

        val rows = CutListCalculator.buildRows(pieces, consolidate = true)
        assertEquals(5, rows.size)
        assertEquals(listOf("1", "2"), rows[0].sourceIds)
        assertEquals(3, rows[0].piece.quantity)
        assertTrue(rows[0].isMerged)
        assertEquals("Shelf", rows[0].piece.name)
        // Totals are unaffected by consolidation.
        assertEquals(CutListCalculator.calculateTotalPieces(pieces), rows.sumOf { it.piece.quantity })
    }

    @Test
    fun consolidation_offKeepsEveryEntry() {
        val pieces = listOf(piece("A", "10", "10", 1, id = "1"), piece("A", "10", "10", 1, id = "2"))
        val rows = CutListCalculator.buildRows(pieces, consolidate = false)
        assertEquals(2, rows.size)
        assertFalse(rows.any { it.isMerged })
        assertEquals(1, CutListCalculator.calculateUniqueItems(pieces))
    }

    @Test
    fun sorting_doesNotChangeEntries_andTiesKeepEntryOrder() {
        val pieces = listOf(
            piece("b-side", "800", "300", 2, id = "1"),
            piece("Back", "1200", "800", 1, id = "2"),
            piece("apron", "600", "100", 4, id = "3"),
            piece("Shelf", "1200", "300", 2, id = "4"),
        )
        val rows = CutListCalculator.buildRows(pieces, consolidate = true)
        fun ids(option: SortOption) = CutListCalculator.sortRows(rows, option).map { it.piece.id }

        assertEquals(listOf("1", "2", "3", "4"), ids(SortOption.ORIGINAL))
        assertEquals(listOf("3", "1", "2", "4"), ids(SortOption.NAME))
        assertEquals(listOf("2", "4", "1", "3"), ids(SortOption.LENGTH))
        assertEquals(listOf("2", "1", "4", "3"), ids(SortOption.WIDTH))
        assertEquals(listOf("3", "1", "4", "2"), ids(SortOption.QUANTITY))
        assertEquals(listOf("1", "2", "3", "4"), pieces.map { it.id })
    }

    @Test
    fun largeQuantities_doNotOverflow() {
        val max = piece("Big", "100000", "100000", 1000)
        val pieces = List(200) { max.copy(id = "p$it") }
        assertEquals(200_000, CutListCalculator.calculateTotalPieces(pieces))
        // 1e5 × 1e5 × 1000 × 200 = 2e15 mm² exactly.
        assertNumber("2000000000000000", CutListCalculator.calculateTotalArea(pieces))
        assertNumber("20000000000", CutListCalculator.calculateTotalPieceLength(pieces))
    }

    @Test
    fun largePieceCounts_areFastAndCorrect() {
        val pieces = List(200) { i -> piece("Part $i", "${100 + i}", "50", (i % 5) + 1, id = "id$i") }
        val start = System.nanoTime()
        val summary = CutListCalculator.summarize(pieces)
        val rows = CutListCalculator.sortRows(CutListCalculator.buildRows(pieces, true), SortOption.LENGTH)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertEquals(200, summary.uniqueItems)
        assertEquals(200, rows.size)
        assertEquals((0 until 200).sumOf { (it % 5) + 1 }, summary.totalPieces)
        assertEquals("id199", rows.first().piece.id)
        assertTrue("took $elapsedMs ms", elapsedMs < 1000)
    }
}

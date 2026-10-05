package com.example.cutlist.domain

import java.math.BigDecimal

/**
 * All cut-list math. Values are exact BigDecimal in millimeters / square millimeters;
 * nothing is rounded here — rounding happens only when formatting for display.
 */
object CutListCalculator {

    /** Face area of one piece: length × width (mm²). */
    fun calculatePieceArea(piece: CutPiece): BigDecimal = piece.lengthMm.multiply(piece.widthMm)

    /** Face area of all copies of one piece: length × width × quantity (mm²). */
    fun calculatePieceTotalArea(piece: CutPiece): BigDecimal =
        calculatePieceArea(piece).multiply(BigDecimal(piece.quantity))

    fun calculateTotalArea(pieces: List<CutPiece>): BigDecimal =
        pieces.fold(BigDecimal.ZERO) { total, piece -> total.add(calculatePieceTotalArea(piece)) }

    /** Sum of length × quantity (mm). This is not the required stock length. */
    fun calculateTotalPieceLength(pieces: List<CutPiece>): BigDecimal =
        pieces.fold(BigDecimal.ZERO) { total, piece ->
            total.add(piece.lengthMm.multiply(BigDecimal(piece.quantity)))
        }

    fun calculateTotalPieces(pieces: List<CutPiece>): Int = pieces.sumOf { it.quantity }

    /** Number of distinct items once exact duplicates are combined. */
    fun calculateUniqueItems(pieces: List<CutPiece>): Int = pieces.distinctBy { consolidationKey(it) }.size

    fun summarize(pieces: List<CutPiece>): CutListSummary = CutListSummary(
        uniqueItems = calculateUniqueItems(pieces),
        totalPieces = calculateTotalPieces(pieces),
        totalAreaMm2 = calculateTotalArea(pieces),
        totalPieceLengthMm = calculateTotalPieceLength(pieces),
    )

    /** Groups physical piece counts by thickness, thickest first; unspecified thickness last. */
    fun groupByThickness(pieces: List<CutPiece>): List<ThicknessGroup> {
        val groups = linkedMapOf<String?, Pair<BigDecimal?, Int>>()
        for (piece in pieces) {
            val key = piece.thicknessMm?.let(::numericKey)
            val current = groups[key]
            groups[key] = (current?.first ?: piece.thicknessMm) to ((current?.second ?: 0) + piece.quantity)
        }
        return groups.values
            .map { (thickness, count) -> ThicknessGroup(thickness, count) }
            .sortedWith(compareBy<ThicknessGroup> { it.thicknessMm == null }.thenByDescending { it.thicknessMm })
    }

    /** Groups physical piece counts by material (case-insensitive), largest group first. */
    fun groupByMaterial(pieces: List<CutPiece>): List<MaterialGroup> {
        val groups = linkedMapOf<String?, Pair<String?, Int>>()
        for (piece in pieces) {
            val material = piece.material?.trim()?.ifEmpty { null }
            val key = material?.lowercase()
            val current = groups[key]
            groups[key] = (current?.first ?: material) to ((current?.second ?: 0) + piece.quantity)
        }
        return groups.values
            .map { (material, count) -> MaterialGroup(material, count) }
            .sortedWith(compareBy<MaterialGroup> { it.material == null }.thenByDescending { it.pieceCount })
    }

    /**
     * Builds display rows. With [consolidate], entries that are identical in every respect except
     * quantity (name, dimensions, thickness, material, notes, grain, rotation) are combined so no
     * information is lost. Different names or notes always stay separate.
     */
    fun buildRows(pieces: List<CutPiece>, consolidate: Boolean): List<CutListRow> {
        if (!consolidate) {
            return pieces.mapIndexed { index, piece -> CutListRow(piece, listOf(piece.id), index) }
        }
        val rows = linkedMapOf<ConsolidationKey, CutListRow>()
        pieces.forEachIndexed { index, piece ->
            val key = consolidationKey(piece)
            val existing = rows[key]
            rows[key] = if (existing == null) {
                CutListRow(piece, listOf(piece.id), index)
            } else {
                existing.copy(
                    piece = existing.piece.copy(quantity = existing.piece.quantity + piece.quantity),
                    sourceIds = existing.sourceIds + piece.id,
                )
            }
        }
        return rows.values.toList()
    }

    /** Returns a sorted copy for display; the underlying entry order is never changed. */
    fun sortRows(rows: List<CutListRow>, option: SortOption): List<CutListRow> {
        val byEntryOrder = compareBy<CutListRow> { it.originalIndex }
        val comparator = when (option) {
            SortOption.ORIGINAL -> byEntryOrder
            SortOption.NAME -> compareBy<CutListRow, String>(String.CASE_INSENSITIVE_ORDER) { it.piece.name }
                .then(byEntryOrder)
            SortOption.LENGTH -> compareByDescending<CutListRow> { it.piece.lengthMm }.then(byEntryOrder)
            SortOption.WIDTH -> compareByDescending<CutListRow> { it.piece.widthMm }.then(byEntryOrder)
            SortOption.QUANTITY -> compareByDescending<CutListRow> { it.piece.quantity }.then(byEntryOrder)
        }
        return rows.sortedWith(comparator)
    }

    private data class ConsolidationKey(
        val name: String,
        val length: String,
        val width: String,
        val thickness: String?,
        val material: String?,
        val notes: String?,
        val grain: GrainDirection,
        val rotationAllowed: Boolean,
    )

    private fun consolidationKey(piece: CutPiece) = ConsolidationKey(
        name = piece.name.trim().lowercase(),
        length = numericKey(piece.lengthMm),
        width = numericKey(piece.widthMm),
        thickness = piece.thicknessMm?.let(::numericKey),
        material = piece.material?.trim()?.lowercase()?.ifEmpty { null },
        notes = piece.notes?.trim()?.ifEmpty { null },
        grain = piece.grainDirection,
        rotationAllowed = piece.rotationAllowed,
    )

    /** Scale-independent key so 300 and 300.0 compare equal. */
    private fun numericKey(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()
}

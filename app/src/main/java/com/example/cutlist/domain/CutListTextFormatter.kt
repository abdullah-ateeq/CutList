package com.example.cutlist.domain

/** Builds the plain-text cut list used for Copy, Share and Export. */
object CutListTextFormatter {

    data class Options(
        val decimals: Int,
        val showArea: Boolean,
        val showCutOrder: Boolean,
    )

    fun build(
        project: CutListProject,
        rows: List<CutListRow>,
        summary: CutListSummary,
        options: Options,
    ): String = buildString {
        val unit = project.unit
        appendLine("CUT LIST")
        appendLine("Project: ${project.displayName}")
        appendLine()
        rows.forEachIndexed { index, row ->
            val piece = row.piece
            val prefix = if (options.showCutOrder) "${index + 1}. " else "• "
            val dimensions = MeasurementFormatter.formatDimensions(piece.lengthMm, piece.widthMm, unit, options.decimals)
            appendLine("$prefix${piece.name} — $dimensions — Qty ${piece.quantity}")
            val details = buildList {
                piece.thicknessMm?.let { add(MeasurementFormatter.formatLength(it, unit, options.decimals) + " thick") }
                piece.material?.let { add(it) }
                if (piece.grainDirection != GrainDirection.NONE) add("Grain: ${piece.grainDirection.label.lowercase()}")
                if (!piece.rotationAllowed) add("No rotation")
            }
            val indent = " ".repeat(prefix.length)
            if (details.isNotEmpty()) appendLine(indent + details.joinToString(" · "))
            piece.notes?.let { appendLine("$indent\"$it\"") }
        }
        appendLine()
        appendLine("Total pieces: ${summary.totalPieces}")
        appendLine("Unique items: ${summary.uniqueItems}")
        if (options.showArea) {
            appendLine("Total area: ${MeasurementFormatter.formatArea(summary.totalAreaMm2, unit, options.decimals).text}")
        }
        append("Total piece length: ${MeasurementFormatter.formatLength(summary.totalPieceLengthMm, unit, options.decimals)}")
    }
}

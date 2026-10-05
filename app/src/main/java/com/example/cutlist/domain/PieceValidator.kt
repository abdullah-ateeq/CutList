package com.example.cutlist.domain

import java.math.BigDecimal

/** Raw text the user typed in the piece editor. Dimensions are in [unit]. */
data class PieceInput(
    val name: String = "",
    val length: String = "",
    val width: String = "",
    val quantity: String = "1",
    val thickness: String = "",
    val material: String = "",
    val notes: String = "",
    val rotationAllowed: Boolean = true,
    val grainDirection: GrainDirection = GrainDirection.NONE,
)

data class PieceErrors(
    val name: String? = null,
    val length: String? = null,
    val width: String? = null,
    val quantity: String? = null,
    val thickness: String? = null,
) {
    val hasErrors: Boolean
        get() = name != null || length != null || width != null || quantity != null || thickness != null
}

sealed interface PieceValidationResult {
    data class Valid(val piece: CutPiece) : PieceValidationResult
    data class Invalid(val errors: PieceErrors) : PieceValidationResult
}

object PieceValidator {
    const val MAX_NAME_LENGTH = 60
    const val MAX_MATERIAL_LENGTH = 40
    const val MAX_NOTES_LENGTH = 200
    const val MIN_QUANTITY = 1
    const val MAX_QUANTITY = 1000

    /** 100 m — far beyond any realistic workshop part, small enough to keep totals sane. */
    val MAX_DIMENSION_MM = BigDecimal(100_000)

    /** 1 m upper bound for thickness. */
    val MAX_THICKNESS_MM = BigDecimal(1_000)

    fun validate(input: PieceInput, unit: LengthUnit, id: String): PieceValidationResult {
        val name = input.name.trim()
        val nameError = when {
            name.isEmpty() -> "Enter a piece name."
            name.length > MAX_NAME_LENGTH -> "Use $MAX_NAME_LENGTH characters or fewer."
            else -> null
        }

        val (lengthMm, lengthError) = requiredDimension(input.length, unit, "Length")
        val (widthMm, widthError) = requiredDimension(input.width, unit, "Width")

        val quantity = NumberParser.parseWholeNumber(input.quantity)
        val quantityError = when {
            input.quantity.isBlank() -> "Quantity must be at least 1."
            quantity == null -> if (NumberParser.parseDecimal(input.quantity) != null) {
                "Quantity must be a whole number."
            } else {
                "Enter a valid quantity."
            }
            quantity < MIN_QUANTITY -> "Quantity must be at least 1."
            quantity > MAX_QUANTITY -> "Quantity cannot exceed $MAX_QUANTITY."
            else -> null
        }

        var thicknessMm: BigDecimal? = null
        val thicknessError = if (input.thickness.isBlank()) {
            null
        } else {
            val value = NumberParser.parseDecimal(input.thickness)
            when {
                value == null -> "Enter a valid number."
                value.signum() <= 0 -> "Thickness must be greater than zero."
                else -> {
                    val mm = unit.toMillimeters(value)
                    if (mm > MAX_THICKNESS_MM) {
                        "Thickness is too large."
                    } else {
                        thicknessMm = mm
                        null
                    }
                }
            }
        }

        val errors = PieceErrors(nameError, lengthError, widthError, quantityError, thicknessError)
        if (errors.hasErrors || lengthMm == null || widthMm == null || quantity == null) {
            return PieceValidationResult.Invalid(errors)
        }

        return PieceValidationResult.Valid(
            CutPiece(
                id = id,
                name = name,
                lengthMm = lengthMm,
                widthMm = widthMm,
                quantity = quantity,
                thicknessMm = thicknessMm,
                material = input.material.trim().take(MAX_MATERIAL_LENGTH).ifEmpty { null },
                notes = input.notes.trim().take(MAX_NOTES_LENGTH).ifEmpty { null },
                rotationAllowed = input.rotationAllowed,
                grainDirection = input.grainDirection,
            )
        )
    }

    private fun requiredDimension(text: String, unit: LengthUnit, label: String): Pair<BigDecimal?, String?> {
        if (text.isBlank()) return null to "$label must be greater than zero."
        val value = NumberParser.parseDecimal(text) ?: return null to "Enter a valid number."
        if (value.signum() <= 0) return null to "$label must be greater than zero."
        val mm = unit.toMillimeters(value)
        if (mm > MAX_DIMENSION_MM) return null to "$label is too large (max 100 m)."
        return mm to null
    }

    /** Converts a stored piece back to editable text in [unit], without display rounding. */
    fun toInput(piece: CutPiece, unit: LengthUnit): PieceInput = PieceInput(
        name = piece.name,
        length = editableValue(piece.lengthMm, unit),
        width = editableValue(piece.widthMm, unit),
        quantity = piece.quantity.toString(),
        thickness = piece.thicknessMm?.let { editableValue(it, unit) }.orEmpty(),
        material = piece.material.orEmpty(),
        notes = piece.notes.orEmpty(),
        rotationAllowed = piece.rotationAllowed,
        grainDirection = piece.grainDirection,
    )

    fun editableValue(millimeters: BigDecimal, unit: LengthUnit): String =
        unit.fromMillimeters(millimeters)
            .setScale(6, java.math.RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
}

package com.example.cutlist.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal

const val UNTITLED_PROJECT_NAME = "Untitled Cut List"

@Serializable
enum class GrainDirection(val label: String) {
    NONE("None"),
    LENGTH("Length"),
    WIDTH("Width"),
}

@Serializable
data class CutPiece(
    val id: String,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class)
    val lengthMm: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val widthMm: BigDecimal,
    val quantity: Int,
    @Serializable(with = BigDecimalSerializer::class)
    val thicknessMm: BigDecimal? = null,
    val material: String? = null,
    val notes: String? = null,
    val rotationAllowed: Boolean = true,
    val grainDirection: GrainDirection = GrainDirection.NONE,
)

@Serializable
data class CutListProject(
    val id: String,
    val name: String,
    val unit: LengthUnit,
    val pieces: List<CutPiece>,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val displayName: String get() = name.trim().ifEmpty { UNTITLED_PROJECT_NAME }

    /** True when both projects hold the same user content, ignoring timestamps. */
    fun hasSameContentAs(other: CutListProject): Boolean =
        id == other.id && name == other.name && unit == other.unit && pieces == other.pieces
}

data class CutListSummary(
    val uniqueItems: Int,
    val totalPieces: Int,
    val totalAreaMm2: BigDecimal,
    val totalPieceLengthMm: BigDecimal,
)

/**
 * One displayed line of the cut list. [piece] carries the (possibly summed) quantity;
 * [sourceIds] lists the entries it was built from, in entry order.
 */
data class CutListRow(
    val piece: CutPiece,
    val sourceIds: List<String>,
    val originalIndex: Int,
) {
    val isMerged: Boolean get() = sourceIds.size > 1
}

/** Pieces grouped by thickness; [thicknessMm] is null for pieces without a thickness. */
data class ThicknessGroup(val thicknessMm: BigDecimal?, val pieceCount: Int)

/** Pieces grouped by material; [material] is null for pieces without a material. */
data class MaterialGroup(val material: String?, val pieceCount: Int)

enum class SortOption(val label: String) {
    ORIGINAL("Original order"),
    NAME("Name A–Z"),
    LENGTH("Length"),
    WIDTH("Width"),
    QUANTITY("Quantity"),
}

object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.math.BigDecimal", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal = BigDecimal(decoder.decodeString())
}

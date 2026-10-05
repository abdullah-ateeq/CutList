package com.example.cutlist.domain

import java.math.BigDecimal

internal fun piece(
    name: String,
    lengthMm: String,
    widthMm: String,
    quantity: Int,
    thicknessMm: String? = null,
    material: String? = null,
    notes: String? = null,
    id: String = "$name-$lengthMm-$widthMm-$quantity-${material.orEmpty()}-${notes.orEmpty()}",
) = CutPiece(
    id = id,
    name = name,
    lengthMm = BigDecimal(lengthMm),
    widthMm = BigDecimal(widthMm),
    quantity = quantity,
    thicknessMm = thicknessMm?.let(::BigDecimal),
    material = material,
    notes = notes,
)

/** BigDecimal equality ignoring scale (2.16 == 2.160). */
internal fun assertNumber(expected: String, actual: BigDecimal) {
    org.junit.Assert.assertEquals(
        "expected $expected but was ${actual.toPlainString()}",
        0,
        BigDecimal(expected).compareTo(actual),
    )
}

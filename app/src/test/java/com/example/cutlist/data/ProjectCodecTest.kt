package com.example.cutlist.data

import com.example.cutlist.domain.CutListProject
import com.example.cutlist.domain.CutPiece
import com.example.cutlist.domain.GrainDirection
import com.example.cutlist.domain.LengthUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ProjectCodecTest {

    private fun project(id: String, updatedAt: Long = 0) = CutListProject(
        id = id,
        name = "TV Unit $id",
        unit = LengthUnit.IN,
        pieces = listOf(
            CutPiece(
                id = "$id-1", name = "Top", lengthMm = BigDecimal("1200.15"), widthMm = BigDecimal("301.625"),
                quantity = 2, thicknessMm = BigDecimal("19.05"), material = "Oak", notes = "Round front edge",
                rotationAllowed = false, grainDirection = GrainDirection.LENGTH,
            ),
            CutPiece(id = "$id-2", name = "Back", lengthMm = BigDecimal("1200"), widthMm = BigDecimal("600"), quantity = 1),
        ),
        createdAt = 1_700_000_000_000,
        updatedAt = updatedAt,
    )

    @Test
    fun history_roundTripsExactly() {
        val history = listOf(project("a"), project("b"))
        val decoded = ProjectCodec.decodeHistory(ProjectCodec.encodeHistory(history))
        assertEquals(history, decoded)
        assertEquals(0, BigDecimal("301.625").compareTo(decoded[0].pieces[0].widthMm))
        assertNull(decoded[0].pieces[1].thicknessMm)
        assertEquals(GrainDirection.LENGTH, decoded[0].pieces[0].grainDirection)
    }

    @Test
    fun corruptOrMissingData_decodesSafely() {
        assertEquals(emptyList<CutListProject>(), ProjectCodec.decodeHistory(null))
        assertEquals(emptyList<CutListProject>(), ProjectCodec.decodeHistory("{not json"))
        assertEquals(emptyList<CutListProject>(), ProjectCodec.decodeHistory("""[{"id":"x"}]"""))
        assertEquals(
            emptyList<CutListProject>(),
            ProjectCodec.decodeHistory("""[{"id":"x","name":"","unit":"MM","createdAt":0,"updatedAt":0,"pieces":[{"id":"1","name":"a","lengthMm":"NaN","widthMm":"1","quantity":1}]}]"""),
        )
        assertNull(ProjectCodec.decodeProject("garbage"))
    }

    @Test
    fun draft_roundTrips() {
        val p = project("draft")
        assertEquals(p, ProjectCodec.decodeProject(ProjectCodec.encodeProject(p)))
    }

    @Test
    fun upsert_movesToTopAndReplaces() {
        val history = listOf(project("a"), project("b"), project("c"))
        val updatedB = project("b", updatedAt = 99)
        val result = ProjectCodec.upsert(history, updatedB)
        assertEquals(listOf("b", "a", "c"), result.map { it.id })
        assertEquals(99, result.first().updatedAt)
    }

    @Test
    fun history_isCappedAt50_droppingOldest() {
        var history = emptyList<CutListProject>()
        repeat(51) { i -> history = ProjectCodec.upsert(history, project("p$i")) }
        assertEquals(50, history.size)
        assertEquals("p50", history.first().id)
        assertTrue(history.none { it.id == "p0" })
    }

    @Test
    fun duplicate_createsIndependentCopy() {
        val source = project("a")
        val copy = ProjectRepository.duplicateOf(source, now = 5)
        assertEquals("Copy of TV Unit a", copy.name)
        assertNotEquals(source.id, copy.id)
        assertTrue(copy.pieces.map { it.id }.none { it in source.pieces.map(CutPiece::id) })
        assertEquals(source.pieces.map { it.copy(id = "") }, copy.pieces.map { it.copy(id = "") })
        assertEquals(5, copy.createdAt)
    }
}

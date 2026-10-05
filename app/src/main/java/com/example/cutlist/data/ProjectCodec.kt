package com.example.cutlist.data

import com.example.cutlist.domain.CutListProject
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** JSON encoding for saved projects. Corrupt data decodes to empty/null instead of crashing. */
object ProjectCodec {
    const val MAX_HISTORY = 50

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val listSerializer = ListSerializer(CutListProject.serializer())

    fun encodeHistory(projects: List<CutListProject>): String = json.encodeToString(listSerializer, projects)

    fun decodeHistory(text: String?): List<CutListProject> {
        if (text.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(listSerializer, text)
        } catch (_: SerializationException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }

    fun encodeProject(project: CutListProject): String = json.encodeToString(CutListProject.serializer(), project)

    fun decodeProject(text: String?): CutListProject? {
        if (text.isNullOrBlank()) return null
        return try {
            json.decodeFromString(CutListProject.serializer(), text)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * Inserts or replaces [project] at the top of [history] (newest first),
     * dropping the oldest entries beyond [max].
     */
    fun upsert(history: List<CutListProject>, project: CutListProject, max: Int = MAX_HISTORY): List<CutListProject> =
        (listOf(project) + history.filterNot { it.id == project.id }).take(max)
}

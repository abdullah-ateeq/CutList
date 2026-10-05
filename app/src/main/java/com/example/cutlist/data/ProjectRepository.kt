package com.example.cutlist.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.cutlist.domain.CutListProject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID

/** Saved cut-list history (max 50, newest first) and the in-progress draft. */
class ProjectRepository(private val dataStore: DataStore<Preferences>) {

    private val safeData: Flow<Preferences> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }

    val history: Flow<List<CutListProject>> = safeData.map { ProjectCodec.decodeHistory(it[HISTORY]) }

    suspend fun save(project: CutListProject) {
        dataStore.edit { prefs ->
            val updated = ProjectCodec.upsert(ProjectCodec.decodeHistory(prefs[HISTORY]), project)
            prefs[HISTORY] = ProjectCodec.encodeHistory(updated)
        }
    }

    suspend fun duplicate(projectId: String, now: Long = System.currentTimeMillis()): CutListProject? {
        var copy: CutListProject? = null
        dataStore.edit { prefs ->
            val history = ProjectCodec.decodeHistory(prefs[HISTORY])
            val source = history.firstOrNull { it.id == projectId } ?: return@edit
            val created = duplicateOf(source, now)
            copy = created
            prefs[HISTORY] = ProjectCodec.encodeHistory(ProjectCodec.upsert(history, created))
        }
        return copy
    }

    suspend fun delete(projectId: String) {
        dataStore.edit { prefs ->
            val history = ProjectCodec.decodeHistory(prefs[HISTORY]).filterNot { it.id == projectId }
            prefs[HISTORY] = ProjectCodec.encodeHistory(history)
        }
    }

    suspend fun clearHistory() {
        dataStore.edit { it.remove(HISTORY) }
    }

    suspend fun loadDraft(): CutListProject? = ProjectCodec.decodeProject(safeData.first()[DRAFT])

    suspend fun saveDraft(project: CutListProject) {
        dataStore.edit { it[DRAFT] = ProjectCodec.encodeProject(project) }
    }

    companion object {
        private val HISTORY = stringPreferencesKey("history_json")
        private val DRAFT = stringPreferencesKey("draft_json")

        fun duplicateOf(source: CutListProject, now: Long): CutListProject = source.copy(
            id = UUID.randomUUID().toString(),
            name = "Copy of ${source.displayName}",
            pieces = source.pieces.map { it.copy(id = UUID.randomUUID().toString()) },
            createdAt = now,
            updatedAt = now,
        )
    }
}

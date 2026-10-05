package com.example.cutlist.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.cutlist.domain.CutListProject
import com.example.cutlist.domain.LengthUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RepositoryPersistenceTest {

    @get:Rule
    val folder = TemporaryFolder()

    /** Opens a DataStore on [file], runs [block], then closes it — simulating one app session. */
    private fun <T> session(file: File, block: suspend (DataStore<Preferences>) -> T): T = runBlocking {
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file }
        try {
            block(store)
        } finally {
            job.cancelAndJoin()
        }
    }

    @Test
    fun settings_persistAcrossSessions() {
        val file = File(folder.root, "settings.preferences_pb")
        val defaults = session(file) { SettingsRepository(it).settings.first() }
        assertEquals(AppSettings(), defaults)

        session(file) { store ->
            SettingsRepository(store).update {
                it.copy(
                    themeMode = ThemeMode.DARK, dynamicColor = true, defaultUnit = LengthUnit.IN,
                    decimalPlaces = 3, consolidateIdentical = false, showAreaSummary = false,
                    hapticFeedback = false, confirmDelete = false,
                )
            }
        }
        val restored = session(file) { SettingsRepository(it).settings.first() }
        assertEquals(ThemeMode.DARK, restored.themeMode)
        assertEquals(true, restored.dynamicColor)
        assertEquals(LengthUnit.IN, restored.defaultUnit)
        assertEquals(3, restored.decimalPlaces)
        assertEquals(false, restored.consolidateIdentical)
        assertEquals(false, restored.showAreaSummary)
        assertEquals(false, restored.hapticFeedback)
        assertEquals(false, restored.confirmDelete)
        assertEquals(true, restored.showMaterialSummary)

        session(file) { SettingsRepository(it).reset() }
        assertEquals(AppSettings(), session(file) { SettingsRepository(it).settings.first() })
    }

    @Test
    fun history_persistsAcrossSessions_andCapsAt50() {
        val file = File(folder.root, "projects.preferences_pb")
        session(file) { store ->
            val repo = ProjectRepository(store)
            repeat(51) { i ->
                repo.save(CutListProject("p$i", "Project $i", LengthUnit.MM, emptyList(), i.toLong(), i.toLong()))
            }
        }
        val history = session(file) { ProjectRepository(it).history.first() }
        assertEquals(50, history.size)
        assertEquals("p50", history.first().id)
        assertEquals("p1", history.last().id)

        session(file) { store ->
            val repo = ProjectRepository(store)
            repo.delete("p50")
            repo.duplicate("p49", now = 1000)
        }
        val afterEdit = session(file) { ProjectRepository(it).history.first() }
        assertEquals("Copy of Project 49", afterEdit.first().name)
        assertEquals(50, afterEdit.size)

        session(file) { ProjectRepository(it).clearHistory() }
        assertEquals(0, session(file) { ProjectRepository(it).history.first() }.size)
    }

    @Test
    fun draft_persistsAcrossSessions() {
        val file = File(folder.root, "draft.preferences_pb")
        val draft = CutListProject("d", "Desk", LengthUnit.CM, emptyList(), 1, 2)
        session(file) { ProjectRepository(it).saveDraft(draft) }
        assertEquals(draft, session(file) { ProjectRepository(it).loadDraft() })
    }
}

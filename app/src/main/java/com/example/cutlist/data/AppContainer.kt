package com.example.cutlist.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private val Context.projectsDataStore: DataStore<Preferences> by preferencesDataStore(name = "projects")

/** Simple manual dependency container, created once by the Application. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val settingsRepository = SettingsRepository(appContext.settingsDataStore)
    val projectRepository = ProjectRepository(appContext.projectsDataStore)
}

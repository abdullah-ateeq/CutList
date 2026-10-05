package com.example.cutlist.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.cutlist.domain.LengthUnit
import com.example.cutlist.domain.MeasurementFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map(::fromPreferences)

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs -> writeTo(prefs, transform(fromPreferences(prefs))) }
    }

    suspend fun reset() {
        dataStore.edit { prefs -> Keys.all.forEach { prefs.remove(it) } }
    }

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_UNIT = stringPreferencesKey("default_unit")
        val DECIMALS = intPreferencesKey("decimal_places")
        val CONSOLIDATE = booleanPreferencesKey("consolidate_identical")
        val CUT_ORDER = booleanPreferencesKey("show_cut_order")
        val AREA = booleanPreferencesKey("show_area_summary")
        val MATERIAL = booleanPreferencesKey("show_material_summary")
        val THICKNESS = booleanPreferencesKey("show_thickness_summary")
        val HAPTICS = booleanPreferencesKey("haptic_feedback")
        val CONFIRM_DELETE = booleanPreferencesKey("confirm_delete")
        val all: List<Preferences.Key<*>> = listOf(
            THEME, DYNAMIC_COLOR, DEFAULT_UNIT, DECIMALS, CONSOLIDATE, CUT_ORDER,
            AREA, MATERIAL, THICKNESS, HAPTICS, CONFIRM_DELETE,
        )
    }

    private fun fromPreferences(prefs: Preferences): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[Keys.THEME] } ?: defaults.themeMode,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
            defaultUnit = LengthUnit.fromNameOrDefault(prefs[Keys.DEFAULT_UNIT], defaults.defaultUnit),
            decimalPlaces = (prefs[Keys.DECIMALS] ?: defaults.decimalPlaces)
                .coerceIn(MeasurementFormatter.MIN_DECIMALS, MeasurementFormatter.MAX_DECIMALS),
            consolidateIdentical = prefs[Keys.CONSOLIDATE] ?: defaults.consolidateIdentical,
            showCutOrder = prefs[Keys.CUT_ORDER] ?: defaults.showCutOrder,
            showAreaSummary = prefs[Keys.AREA] ?: defaults.showAreaSummary,
            showMaterialSummary = prefs[Keys.MATERIAL] ?: defaults.showMaterialSummary,
            showThicknessSummary = prefs[Keys.THICKNESS] ?: defaults.showThicknessSummary,
            hapticFeedback = prefs[Keys.HAPTICS] ?: defaults.hapticFeedback,
            confirmDelete = prefs[Keys.CONFIRM_DELETE] ?: defaults.confirmDelete,
        )
    }

    private fun writeTo(prefs: MutablePreferences, settings: AppSettings) {
        prefs[Keys.THEME] = settings.themeMode.name
        prefs[Keys.DYNAMIC_COLOR] = settings.dynamicColor
        prefs[Keys.DEFAULT_UNIT] = settings.defaultUnit.name
        prefs[Keys.DECIMALS] = settings.decimalPlaces
            .coerceIn(MeasurementFormatter.MIN_DECIMALS, MeasurementFormatter.MAX_DECIMALS)
        prefs[Keys.CONSOLIDATE] = settings.consolidateIdentical
        prefs[Keys.CUT_ORDER] = settings.showCutOrder
        prefs[Keys.AREA] = settings.showAreaSummary
        prefs[Keys.MATERIAL] = settings.showMaterialSummary
        prefs[Keys.THICKNESS] = settings.showThicknessSummary
        prefs[Keys.HAPTICS] = settings.hapticFeedback
        prefs[Keys.CONFIRM_DELETE] = settings.confirmDelete
    }
}

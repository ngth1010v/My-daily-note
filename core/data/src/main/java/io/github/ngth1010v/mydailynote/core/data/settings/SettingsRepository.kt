package io.github.ngth1010v.mydailynote.core.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsStore

    private object Keys {
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val railSide = stringPreferencesKey("rail_side")
        val dayStartHour = intPreferencesKey("day_start_hour")
        val timeFormat = stringPreferencesKey("time_format")
        val autoBackup = booleanPreferencesKey("auto_backup")
        val updateCheck = booleanPreferencesKey("update_check")
        val autoGroupActions = booleanPreferencesKey("auto_group_actions")
        val autoCreateGroups = booleanPreferencesKey("auto_create_groups")
    }

    val settings: Flow<AppSettings> = store.data.map { it.toSettings() }.distinctUntilChanged()

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[Keys.dynamicColor] = s.dynamicColor
            prefs[Keys.railSide] = s.railSide.name
            prefs[Keys.dayStartHour] = s.dayStartHour.coerceIn(0, 23)
            prefs[Keys.timeFormat] = s.timeFormat.name
            prefs[Keys.autoBackup] = s.autoBackup
            prefs[Keys.updateCheck] = s.updateCheck
            prefs[Keys.autoGroupActions] = s.autoGroupActions
            prefs[Keys.autoCreateGroups] = s.autoCreateGroups
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            dynamicColor = this[Keys.dynamicColor] ?: d.dynamicColor,
            railSide = enumOr(this[Keys.railSide], d.railSide),
            dayStartHour = this[Keys.dayStartHour] ?: d.dayStartHour,
            timeFormat = enumOr(this[Keys.timeFormat], d.timeFormat),
            autoBackup = this[Keys.autoBackup] ?: d.autoBackup,
            updateCheck = this[Keys.updateCheck] ?: d.updateCheck,
            autoGroupActions = this[Keys.autoGroupActions] ?: d.autoGroupActions,
            autoCreateGroups = this[Keys.autoCreateGroups] ?: d.autoCreateGroups,
        )
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: default
}

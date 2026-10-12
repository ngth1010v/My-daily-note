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
        val theme = stringPreferencesKey("theme")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val railSide = stringPreferencesKey("rail_side")
        val dayStartHour = intPreferencesKey("day_start_hour")
        val timeFormat = stringPreferencesKey("time_format")
        val defaultColor = stringPreferencesKey("default_color")
        val voiceLanguage = stringPreferencesKey("voice_language")
        val speechEngine = stringPreferencesKey("speech_engine")
        val autoStopMs = intPreferencesKey("auto_stop_ms")
        val highAccuracy = booleanPreferencesKey("high_accuracy")
        val whisperModel = stringPreferencesKey("whisper_model")
        val autoBackup = booleanPreferencesKey("auto_backup")
        val updateCheck = booleanPreferencesKey("update_check")
    }

    val settings: Flow<AppSettings> = store.data.map { it.toSettings() }.distinctUntilChanged()

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[Keys.theme] = s.themeMode.name
            prefs[Keys.dynamicColor] = s.dynamicColor
            prefs[Keys.railSide] = s.railSide.name
            prefs[Keys.dayStartHour] = s.dayStartHour.coerceIn(0, 23)
            prefs[Keys.timeFormat] = s.timeFormat.name
            prefs[Keys.defaultColor] = s.defaultColor
            if (s.voiceLanguage == null) prefs.remove(Keys.voiceLanguage)
            else prefs[Keys.voiceLanguage] = s.voiceLanguage
            prefs[Keys.speechEngine] = s.speechEngine
            prefs[Keys.autoStopMs] = s.autoStopMs
            prefs[Keys.highAccuracy] = s.highAccuracy
            prefs[Keys.whisperModel] = s.whisperModelId
            prefs[Keys.autoBackup] = s.autoBackup
            prefs[Keys.updateCheck] = s.updateCheck
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            themeMode = enumOr(this[Keys.theme], d.themeMode),
            dynamicColor = this[Keys.dynamicColor] ?: d.dynamicColor,
            railSide = enumOr(this[Keys.railSide], d.railSide),
            dayStartHour = this[Keys.dayStartHour] ?: d.dayStartHour,
            timeFormat = enumOr(this[Keys.timeFormat], d.timeFormat),
            defaultColor = this[Keys.defaultColor] ?: d.defaultColor,
            voiceLanguage = this[Keys.voiceLanguage],
            speechEngine = this[Keys.speechEngine] ?: d.speechEngine,
            autoStopMs = this[Keys.autoStopMs] ?: d.autoStopMs,
            highAccuracy = this[Keys.highAccuracy] ?: d.highAccuracy,
            whisperModelId = this[Keys.whisperModel] ?: d.whisperModelId,
            autoBackup = this[Keys.autoBackup] ?: d.autoBackup,
            updateCheck = this[Keys.updateCheck] ?: d.updateCheck,
        )
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: default
}

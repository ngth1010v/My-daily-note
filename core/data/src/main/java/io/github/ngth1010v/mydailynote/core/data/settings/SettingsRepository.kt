package io.github.ngth1010v.mydailynote.core.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.ngth1010v.mydailynote.core.data.model.RecommendOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

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
        val recommendActions = booleanPreferencesKey("recommend_actions")
        val recommendMinConfidence = intPreferencesKey("recommend_min_confidence")
        val recommendUsualCount = intPreferencesKey("recommend_usual_count")
        val recommendMaxCount = intPreferencesKey("recommend_max_count")
        val recommendHistoryDays = intPreferencesKey("recommend_history_days")
        val recommendWeekdayWeight = intPreferencesKey("recommend_weekday_weight")
        val recommendMinDays = intPreferencesKey("recommend_min_days")
        /** "day|key" of recommendations hidden for that day. */
        val dismissedRecommendations = stringSetPreferencesKey("dismissed_recommendations")
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
            prefs[Keys.recommendActions] = s.recommendActions
            prefs[Keys.recommendMinConfidence] = s.recommend.minConfidencePercent.coerceIn(0, 100)
            prefs[Keys.recommendUsualCount] = s.recommend.usualCount.coerceAtLeast(1)
            prefs[Keys.recommendMaxCount] = s.recommend.maxCount.coerceAtLeast(1)
            prefs[Keys.recommendHistoryDays] = s.recommend.historyDays.coerceAtLeast(1)
            prefs[Keys.recommendWeekdayWeight] = s.recommend.weekdayWeight.coerceAtLeast(1)
            prefs[Keys.recommendMinDays] = s.recommend.minDays.coerceAtLeast(1)
        }
    }

    /** Keys of the recommendations hidden on [day]. */
    fun dismissedRecommendations(day: LocalDate): Flow<Set<String>> = store.data.map { prefs ->
        prefs[Keys.dismissedRecommendations].orEmpty()
            .mapNotNullTo(HashSet()) { it.removePrefix("$day|").takeIf { k -> k != it } }
    }.distinctUntilChanged()

    /** Hides recommendation [key] on [day]; other days' keys are dropped. */
    suspend fun dismissRecommendation(day: LocalDate, key: String) {
        store.edit { prefs ->
            prefs[Keys.dismissedRecommendations] =
                prefs[Keys.dismissedRecommendations].orEmpty().filterTo(HashSet()) { it.startsWith("$day|") } + "$day|$key"
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
            recommendActions = this[Keys.recommendActions] ?: d.recommendActions,
            recommend = RecommendOptions(
                minConfidencePercent = this[Keys.recommendMinConfidence] ?: d.recommend.minConfidencePercent,
                usualCount = this[Keys.recommendUsualCount] ?: d.recommend.usualCount,
                maxCount = this[Keys.recommendMaxCount] ?: d.recommend.maxCount,
                historyDays = this[Keys.recommendHistoryDays] ?: d.recommend.historyDays,
                weekdayWeight = this[Keys.recommendWeekdayWeight] ?: d.recommend.weekdayWeight,
                minDays = this[Keys.recommendMinDays] ?: d.recommend.minDays,
            ),
        )
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: default
}

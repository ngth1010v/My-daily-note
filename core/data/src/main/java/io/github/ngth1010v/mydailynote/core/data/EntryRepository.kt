package io.github.ngth1010v.mydailynote.core.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import io.github.ngth1010v.mydailynote.core.data.autostyle.AutoStyle
import io.github.ngth1010v.mydailynote.core.data.autostyle.StyledText
import io.github.ngth1010v.mydailynote.core.data.db.AppDatabase
import io.github.ngth1010v.mydailynote.core.data.model.Entry
import io.github.ngth1010v.mydailynote.core.data.model.EntrySource
import io.github.ngth1010v.mydailynote.core.data.model.Suggestion
import io.github.ngth1010v.mydailynote.core.data.model.TextNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Duration
import java.time.Instant

class EntryRepository(
    private val db: AppDatabase,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val dao = db.entryDao()

    /** All entries, newest first, loaded a window at a time. */
    fun pagedNewestFirst(): Flow<PagingData<Entry>> =
        Pager(PagingConfig(pageSize = 60, prefetchDistance = 30, enablePlaceholders = false)) {
            dao.pagingNewestFirst()
        }.flow.map { page -> page.map { it.toModel() } }

    val count: Flow<Int> = dao.count()

    suspend fun get(id: Long): Entry? = dao.get(id)?.toModel()

    fun search(query: String, limit: Int = 200): Flow<List<Entry>> {
        val match = TextNormalizer.ftsQuery(query) ?: return flowOf(emptyList())
        return dao.search(match, limit).map { rows -> rows.map { it.toModel() } }
    }

    fun suggestions(prefix: String, limit: Int = 5): Flow<List<Suggestion>> =
        dao.suggestions(TextNormalizer.likePrefix(prefix), limit).map { rows ->
            rows.map { Suggestion(it.text, it.colorOverride, it.iconOverride, it.count) }
        }

    /** Past actions' styles for [AutoStyle], most recent first. */
    suspend fun styleHistory(limit: Int = 500): List<StyledText> =
        dao.styledTexts(limit).map { StyledText(it.text, it.color, it.icon) }

    /** Gives every action shown without a color or icon an auto-picked one, oldest first. */
    suspend fun fillMissingStyles(autoStyle: AutoStyle) {
        val missing = dao.unstyled().map { it.toModel() }
        if (missing.isEmpty()) return
        val history = styleHistory().toMutableList()
        val known = history.mapTo(HashSet()) { TextNormalizer.normalize(it.text) }
        for (e in missing) {
            val picked = autoStyle.pick(e.text, history)
            val color = e.colorOverride ?: picked.color.takeIf { e.groupColor == null }
            val icon = e.iconOverride ?: picked.icon.takeIf { e.groupIcon == null }
            dao.setStyle(e.id, color, icon)
            if (known.add(TextNormalizer.normalize(e.text))) {
                history.add(0, StyledText(e.text, e.color ?: picked.color, e.icon ?: picked.icon))
            }
        }
    }

    /**
     * Replaces every action's color and icon (its own and its group's look) with a fresh pick, oldest
     * first, as if each were typed in that order. Returns how many actions were restyled.
     */
    suspend fun restyleAll(autoStyle: AutoStyle): Int = db.withTransaction {
        val all = dao.allForExport()
        val history = ArrayList<StyledText>()
        val known = HashSet<String>()
        for (e in all) {
            val picked = autoStyle.pick(e.text, history)
            dao.setStyle(e.id, picked.color, picked.icon)
            if (known.add(e.searchText)) history.add(0, StyledText(e.text, picked.color, picked.icon))
        }
        all.size
    }

    /** Logs a new entry achieved now (or at [achievedAt]). Returns its id. */
    suspend fun add(
        text: String,
        source: EntrySource,
        color: String? = null,
        icon: String? = null,
        achievedAt: Instant = clock.instant(),
    ): Long {
        val now = clock.instant()
        val entry = Entry(
            text = text.trim(),
            achievedAt = achievedAt,
            zoneOffset = clock.zone.rules.getOffset(achievedAt),
            createdAt = now,
            updatedAt = now,
            colorOverride = color,
            iconOverride = icon,
            source = source,
        )
        return dao.insert(entry.toEntity())
    }

    /**
     * Logs [text] again as the latest entry with the same text (ignoring case and accents): its
     * spelling, description, color, icon and group are copied; only the time is new. Returns the new
     * id, or null when there is no such entry.
     */
    suspend fun addAsExisting(text: String, source: EntrySource, achievedAt: Instant = clock.instant()): Long? {
        val existing = dao.latestWithText(TextNormalizer.normalize(text))?.toModel() ?: return null
        val now = clock.instant()
        val entry = existing.copy(
            id = 0,
            achievedAt = achievedAt,
            zoneOffset = clock.zone.rules.getOffset(achievedAt),
            createdAt = now,
            updatedAt = now,
            source = source,
        )
        return dao.insert(entry.toEntity())
    }

    suspend fun update(entry: Entry) {
        dao.update(entry.copy(text = entry.text.trim(), updatedAt = clock.instant()).toEntity())
    }

    /** Moves the entry to [achievedAt], recording the device's current offset for that instant. */
    suspend fun changeTime(entry: Entry, achievedAt: Instant) {
        update(entry.copy(achievedAt = achievedAt, zoneOffset = clock.zone.rules.getOffset(achievedAt)))
    }

    suspend fun delete(id: Long) = dao.setDeleted(id, clock.millis())

    suspend fun undoDelete(id: Long) = dao.setDeleted(id, null)

    /** Hard-deletes entries soft-deleted more than a day ago. */
    suspend fun purgeDeleted() = dao.purgeDeleted(clock.instant().minus(Duration.ofDays(1)).toEpochMilli())

    suspend fun clearAll() = dao.clearAll()
}

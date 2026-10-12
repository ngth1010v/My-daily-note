package io.github.ngth1010v.mydailynote.core.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
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

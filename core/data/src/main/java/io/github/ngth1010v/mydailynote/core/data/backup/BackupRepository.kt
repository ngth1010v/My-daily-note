package io.github.ngth1010v.mydailynote.core.data.backup

import androidx.room.withTransaction
import io.github.ngth1010v.mydailynote.core.data.db.AppDatabase
import io.github.ngth1010v.mydailynote.core.data.model.TextNormalizer
import io.github.ngth1010v.mydailynote.core.data.toEntity
import io.github.ngth1010v.mydailynote.core.data.toModel
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock

data class ImportResult(val imported: Int, val skippedDuplicates: Int)

class BackupRepository(
    private val db: AppDatabase,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val dao = db.entryDao()
    private val groupDao = db.groupDao()

    /** Writes all live entries; returns how many were written. */
    suspend fun export(format: BackupFormat, out: OutputStream): Int {
        val entries = dao.allForExport().map { it.toModel() }
        val text = when (format) {
            BackupFormat.JSON -> BackupCodec.encodeJson(
                BackupData(
                    groupDao.groups().map { it.toModel() },
                    entries,
                    groupDao.actions().map { it.toModel() },
                ),
                clock.instant(),
            )
            BackupFormat.CSV -> "\uFEFF" + BackupCodec.encodeCsv(entries) // BOM so Excel reads UTF-8
        }
        out.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
        return entries.size
    }

    /**
     * Adds entries from a JSON or CSV export (detected from the content). Entries with the same
     * achieved time and text as an existing one are skipped.
     */
    suspend fun import(input: InputStream): ImportResult {
        val text = input.bufferedReader(Charsets.UTF_8).use { it.readText() }.removePrefix("\uFEFF")
        val format = if (text.trimStart().startsWith("{")) BackupFormat.JSON else BackupFormat.CSV
        val data = when (format) {
            BackupFormat.JSON -> BackupCodec.decodeJson(text)
            BackupFormat.CSV -> BackupData(emptyList(), BackupCodec.decodeCsv(text))
        }
        return db.withTransaction {
            // Groups with the same name are reused, so importing twice doesn't duplicate them; the
            // built-in group maps to this device's.
            val local = groupDao.groups()
            val byName = local.filterNot { it.isDefault }.associate { it.name to it.id }
            var nextPosition = (local.maxOfOrNull { it.position } ?: -1) + 1
            val groupIds = data.groups.sortedBy { it.position }.associate { g ->
                val existing = if (g.isDefault) local.firstOrNull { it.isDefault }?.id else byName[g.name]
                g.id to (existing ?: groupDao.insertGroup(g.copy(id = 0, position = nextPosition++).toEntity()))
            }
            // Actions this device doesn't have yet join their group, after its current actions.
            data.actions.sortedBy { it.position }.forEach { a ->
                val groupId = groupIds[a.groupId] ?: return@forEach
                if (groupDao.actionWithText(TextNormalizer.normalize(a.text)) == null) {
                    groupDao.insertAction(
                        a.copy(id = 0, groupId = groupId, position = groupDao.nextActionPosition(groupId)).toEntity()
                    )
                }
            }
            val existing = dao.allKeys().toHashSet()
            val now = clock.instant()
            var skipped = 0
            val fresh = data.entries.filter { e ->
                (existing.add("${e.achievedAt.toEpochMilli()}|${e.text}")).also { if (!it) skipped++ }
            }.map { e ->
                e.copy(id = 0, groupId = e.groupId?.let(groupIds::get), updatedAt = now).toEntity()
            }
            dao.insertAll(fresh)
            ImportResult(fresh.size, skipped)
        }
    }
}

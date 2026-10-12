package io.github.ngth1010v.mydailynote.core.data.backup

import androidx.room.withTransaction
import io.github.ngth1010v.mydailynote.core.data.db.AppDatabase
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

    /** Writes all live entries; returns how many were written. */
    suspend fun export(format: BackupFormat, out: OutputStream): Int {
        val entries = dao.allForExport().map { it.toModel() }
        val text = when (format) {
            BackupFormat.JSON -> BackupCodec.encodeJson(
                BackupData(dao.allGroups().map { it.toModel() }, entries), clock.instant(),
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
            // Groups with the same name are reused, so importing twice doesn't duplicate them.
            val byName = dao.allGroups().associate { it.name to it.id }
            val groupIds = data.groups.associate { g ->
                g.id to (byName[g.name] ?: dao.insertGroup(g.copy(id = 0).toEntity()))
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

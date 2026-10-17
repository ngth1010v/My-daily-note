package io.github.ngth1010v.mydailynote.core.data.backup

import io.github.ngth1010v.mydailynote.core.data.model.Action
import io.github.ngth1010v.mydailynote.core.data.model.Entry
import io.github.ngth1010v.mydailynote.core.data.model.EntrySource
import io.github.ngth1010v.mydailynote.core.data.model.Group
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime

enum class BackupFormat(val mimeType: String, val extension: String) {
    JSON("application/json", "json"),
    CSV("text/csv", "csv"),
}

data class BackupData(val groups: List<Group>, val entries: List<Entry>, val actions: List<Action> = emptyList())

class BackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Converts entries to/from the export formats.
 * - JSON is lossless (groups and their actions, both timestamps, color/icon, source).
 * - CSV is for spreadsheets: one row per entry, achieved time as ISO-8601 with offset.
 */
object BackupCodec {
    /** 2: group position/default/category and actions. */
    const val JSON_VERSION = 2
    private const val APP_ID = "my-daily-note"
    val CSV_HEADER = listOf("achieved_at", "text", "description", "color", "icon", "source")

    fun encodeJson(data: BackupData, exportedAt: Instant): String {
        val root = JSONObject()
            .put("app", APP_ID)
            .put("version", JSON_VERSION)
            .put("exportedAt", exportedAt.toString())
        root.put("groups", JSONArray().apply {
            data.groups.forEach { g ->
                put(JSONObject()
                    .put("id", g.id)
                    .put("name", g.name)
                    .putOpt("color", g.color)
                    .putOpt("icon", g.icon)
                    .putOpt("description", g.description)
                    .put("createdAt", g.createdAt.toString())
                    .put("position", g.position)
                    .put("isDefault", g.isDefault)
                    .putOpt("category", g.category))
            }
        })
        root.put("actions", JSONArray().apply {
            data.actions.forEach { a ->
                put(JSONObject()
                    .put("text", a.text)
                    .put("groupId", a.groupId)
                    .put("position", a.position)
                    .putOpt("color", a.color)
                    .putOpt("icon", a.icon)
                    .put("createdAt", a.createdAt.toString()))
            }
        })
        root.put("entries", JSONArray().apply {
            data.entries.forEach { e ->
                put(JSONObject()
                    .put("text", e.text)
                    .putOpt("description", e.description)
                    .put("achievedAt", e.achievedAt.atOffset(e.zoneOffset).toString())
                    .put("createdAt", e.createdAt.toString())
                    .putOpt("color", e.colorOverride)
                    .putOpt("icon", e.iconOverride)
                    .putOpt("groupId", e.groupId)
                    .put("source", e.source.name))
            }
        })
        return root.toString(2)
    }

    fun decodeJson(text: String): BackupData = try {
        val root = JSONObject(text)
        if (root.optString("app") != APP_ID) throw BackupFormatException("Not a My diary backup")
        val groups = root.optJSONArray("groups").objects().map { o ->
            Group(
                id = o.getLong("id"),
                name = o.getString("name"),
                color = o.optStringOrNull("color"),
                icon = o.optStringOrNull("icon"),
                description = o.optStringOrNull("description"),
                createdAt = Instant.parse(o.getString("createdAt")),
                position = o.optInt("position", 0),
                isDefault = o.optBoolean("isDefault", false),
                category = if (o.has("category") && !o.isNull("category")) o.getInt("category") else null,
            )
        }
        val actions = root.optJSONArray("actions").objects().map { o ->
            Action(
                text = o.getString("text"),
                groupId = o.getLong("groupId"),
                position = o.optInt("position", 0),
                color = o.optStringOrNull("color"),
                icon = o.optStringOrNull("icon"),
                createdAt = Instant.parse(o.getString("createdAt")),
            )
        }
        val entries = root.optJSONArray("entries").objects().map { o ->
            val achieved = OffsetDateTime.parse(o.getString("achievedAt"))
            Entry(
                text = o.getString("text"),
                description = o.optStringOrNull("description"),
                achievedAt = achieved.toInstant(),
                zoneOffset = achieved.offset,
                createdAt = o.optStringOrNull("createdAt")?.let(Instant::parse) ?: achieved.toInstant(),
                colorOverride = o.optStringOrNull("color"),
                iconOverride = o.optStringOrNull("icon"),
                groupId = if (o.has("groupId") && !o.isNull("groupId")) o.getLong("groupId") else null,
                source = parseSource(o.optStringOrNull("source")),
            )
        }
        BackupData(groups, entries, actions)
    } catch (e: BackupFormatException) {
        throw e
    } catch (e: Exception) {
        throw BackupFormatException("Invalid JSON backup", e)
    }

    fun encodeCsv(entries: List<Entry>): String = buildString {
        appendCsvRow(CSV_HEADER)
        entries.forEach { e ->
            appendCsvRow(listOf(
                e.achievedAt.atOffset(e.zoneOffset).toString(),
                e.text,
                e.description.orEmpty(),
                e.colorOverride.orEmpty(),
                e.iconOverride.orEmpty(),
                e.source.name,
            ))
        }
    }

    fun decodeCsv(text: String): List<Entry> {
        val rows = parseCsv(text.removePrefix("\uFEFF"))
        if (rows.isEmpty()) return emptyList()
        val header = rows.first().map { it.trim().lowercase() }
        val col = { name: String -> header.indexOf(name) }
        val iTime = col("achieved_at")
        val iText = col("text")
        if (iTime < 0 || iText < 0) throw BackupFormatException("CSV needs achieved_at and text columns")
        val iDesc = col("description")
        val iColor = col("color")
        val iIcon = col("icon")
        val iSource = col("source")
        return rows.drop(1).filter { r -> r.any { it.isNotBlank() } }.mapIndexed { n, r ->
            val cell = { i: Int -> r.getOrNull(i)?.takeIf { i >= 0 && it.isNotBlank() } }
            val achieved = try {
                OffsetDateTime.parse(cell(iTime))
            } catch (e: Exception) {
                throw BackupFormatException("Row ${n + 2}: bad achieved_at", e)
            }
            Entry(
                text = cell(iText) ?: throw BackupFormatException("Row ${n + 2}: empty text"),
                description = cell(iDesc),
                achievedAt = achieved.toInstant(),
                zoneOffset = achieved.offset,
                colorOverride = cell(iColor),
                iconOverride = cell(iIcon),
                source = parseSource(cell(iSource)),
            )
        }
    }

    private fun parseSource(s: String?) =
        EntrySource.entries.firstOrNull { it.name == s } ?: EntrySource.TEXT

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else List(length()) { getJSONObject(it) }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null

    private fun StringBuilder.appendCsvRow(cells: List<String>) {
        cells.forEachIndexed { i, c ->
            if (i > 0) append(',')
            if (c.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
                append('"').append(c.replace("\"", "\"\"")).append('"')
            } else append(c)
        }
        append("\r\n")
    }

    /** RFC 4180 parser: quoted fields, escaped quotes, newlines inside quotes. */
    internal fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') { cell.append('"'); i++ } else inQuotes = false
                } else cell.append(c)
            } else when (c) {
                '"' -> inQuotes = true
                ',' -> { row.add(cell.toString()); cell.clear() }
                '\r' -> {}
                '\n' -> { row.add(cell.toString()); cell.clear(); rows.add(row); row = mutableListOf() }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row.add(cell.toString()); rows.add(row) }
        return rows
    }
}

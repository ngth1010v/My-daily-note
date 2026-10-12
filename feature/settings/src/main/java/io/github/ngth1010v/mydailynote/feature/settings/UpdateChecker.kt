package io.github.ngth1010v.mydailynote.feature.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Asks the GitHub releases API for the latest release. Only runs when the user turned it on. */
internal object UpdateChecker {
    data class Release(val version: String, val url: String)

    suspend fun latest(repoSlug: String): Release? = withContext(Dispatchers.IO) {
        val conn = URL("https://api.github.com/repos/$repoSlug/releases/latest").openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            when (conn.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    Release(json.getString("tag_name").removePrefix("v"), json.getString("html_url"))
                }
                HttpURLConnection.HTTP_NOT_FOUND -> null // no releases yet
                else -> throw IOException("HTTP ${conn.responseCode}")
            }
        } finally {
            conn.disconnect()
        }
    }

    /** Compares dotted numeric versions ("1.10.0" > "1.9.2"); suffixes like "-beta" are ignored. */
    fun isNewer(candidate: String, current: String): Boolean {
        fun parts(v: String) = v.removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val a = parts(candidate)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}

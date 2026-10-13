package io.github.ngth1010v.mydailynote.core.data.model

import java.text.Normalizer

/** Lowercase, diacritic-free form used for search, so "ca phe" matches "Cà phê". */
object TextNormalizer {
    private val marks = Regex("\\p{Mn}+")
    private val spaces = Regex("\\s+")

    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(marks, "")
            .replace('đ', 'd')
            .replace(spaces, " ")
            .trim()

    /** Builds an FTS MATCH expression: every word must match as a prefix. Null if nothing to search. */
    fun ftsQuery(query: String): String? {
        val tokens = normalize(query).split(' ')
            .map { t -> t.filter { it.isLetterOrDigit() } }
            .filter { it.isNotEmpty() }
        return if (tokens.isEmpty()) null else tokens.joinToString(" ") { "\"$it\"*" }
    }

    /**
     * The past text [heard] most likely meant (e.g. voice "ca phe" -> logged "Cà phê"), or null.
     * Matches ignoring case/diacritics, else with a small edit distance (<= 15 %, min 5 chars).
     */
    fun closestKnown(heard: String, known: List<String>): String? {
        val h = normalize(heard)
        if (h.isEmpty()) return null
        known.firstOrNull { normalize(it) == h }?.let { return it }
        if (h.length < 5) return null
        return known
            .map { it to editDistance(h, normalize(it)) }
            .filter { (k, d) -> d <= maxOf(h.length, k.length) * 0.15 }
            .minByOrNull { it.second }?.first
    }

    private fun editDistance(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val cur = IntArray(b.length + 1)
            cur[0] = i
            for (j in 1..b.length) {
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            }
            prev = cur
        }
        return prev[b.length]
    }

    /** Escapes LIKE wildcards so a prefix is matched literally (used with ESCAPE '\'). */
    fun likePrefix(text: String): String =
        normalize(text).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}

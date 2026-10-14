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

    /** Escapes LIKE wildcards so a prefix is matched literally (used with ESCAPE '\'). */
    fun likePrefix(text: String): String =
        normalize(text).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}

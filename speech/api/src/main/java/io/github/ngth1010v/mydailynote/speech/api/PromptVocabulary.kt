package io.github.ngth1010v.mydailynote.speech.api

import java.text.Normalizer

/** Picks the user's own phrases to prime Whisper with, so the prompt never pulls in another language. */
object PromptVocabulary {
    /** Small enough that Whisper treats it as context instead of text to repeat. */
    const val MAX_PHRASES = 10

    private val vietnameseLetters = Regex("[đĐơƠưƯăĂâÂêÊôÔ]")
    private val marks = Regex("\\p{Mn}")

    /** Vietnamese letters or tone marks; a phrase without either counts as English. */
    fun isVietnamese(text: String): Boolean =
        vietnameseLetters.containsMatchIn(text) || marks.containsMatchIn(Normalizer.normalize(text, Normalizer.Form.NFD))

    /** The first [MAX_PHRASES] of [phrases] (most frequent first) in [language], as one prompt; null if none. */
    fun prompt(phrases: List<String>, language: String): String? =
        phrases.filter { isVietnamese(it) == (language == VoiceLanguage.VI.tag) }
            .take(MAX_PHRASES)
            .joinToString(", ")
            .ifEmpty { null }
}

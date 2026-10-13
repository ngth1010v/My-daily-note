package io.github.ngth1010v.mydailynote.speech.api

/** One recognized word and the recognizer's confidence in it (0..1, mean over its tokens). */
data class ScoredWord(val text: String, val prob: Float)

/** Picks the final transcript out of several decodes of the same audio. */
object TranscriptVote {
    /** A word below this makes the clip worth decoding again. */
    const val LOW_CONFIDENCE = 0.6f

    fun needsRecheck(words: List<ScoredWord>): Boolean = words.isEmpty() || words.any { it.prob < LOW_CONFIDENCE }

    /**
     * Always returns one run whole (mixing words of different runs produced scrambled sentences):
     * 1. a transcript that at least two runs produced wins;
     * 2. else the run with the best mean confidence.
     */
    fun choose(runs: List<List<ScoredWord>>): List<ScoredWord> {
        val valid = runs.filter { it.isNotEmpty() }
        if (valid.isEmpty()) return emptyList()
        if (valid.size == 1) return valid.first()
        fun key(run: List<ScoredWord>) = run.joinToString(" ") { it.text.lowercase() }
        valid.groupBy(::key).values.filter { it.size >= 2 }.maxByOrNull { it.size }?.let { return it.first() }
        return valid.maxBy { run -> run.map { it.prob }.average() }
    }
}

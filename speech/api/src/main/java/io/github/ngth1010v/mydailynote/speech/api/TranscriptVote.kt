package io.github.ngth1010v.mydailynote.speech.api

/** One recognized word and the recognizer's confidence in it (0..1, its weakest token). */
data class ScoredWord(val text: String, val prob: Float)

/** Picks the final transcript out of several decodes of the same audio. */
object TranscriptVote {
    /** A word below this makes the clip worth decoding again. */
    const val LOW_CONFIDENCE = 0.6f

    fun needsRecheck(words: List<ScoredWord>): Boolean = words.isEmpty() || words.any { it.prob < LOW_CONFIDENCE }

    /**
     * 1. a transcript that at least two runs produced wins;
     * 2. else, with equal word counts, each position takes the word most runs agree on
     *    (ties: the most confident one);
     * 3. else the run with the best mean confidence.
     */
    fun choose(runs: List<List<ScoredWord>>): List<ScoredWord> {
        val valid = runs.filter { it.isNotEmpty() }
        if (valid.isEmpty()) return emptyList()
        if (valid.size == 1) return valid.first()
        fun key(run: List<ScoredWord>) = run.joinToString(" ") { it.text.lowercase() }
        valid.groupBy(::key).values.filter { it.size >= 2 }.maxByOrNull { it.size }?.let { return it.first() }
        if (valid.map { it.size }.distinct().size == 1) {
            return valid.first().indices.map { i ->
                valid.map { it[i] }
                    .groupBy { it.text.lowercase() }.values
                    .maxWith(compareBy({ it.size }, { g -> g.maxOf { it.prob } }))
                    .maxBy { it.prob }
            }
        }
        return valid.maxBy { run -> run.map { it.prob }.average() }
    }
}

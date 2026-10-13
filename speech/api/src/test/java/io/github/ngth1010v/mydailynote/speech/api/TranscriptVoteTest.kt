package io.github.ngth1010v.mydailynote.speech.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptVoteTest {
    private fun run(vararg w: Pair<String, Float>) = w.map { ScoredWord(it.first, it.second) }
    private fun List<ScoredWord>.text() = joinToString(" ") { it.text }

    @Test
    fun recheckOnlyWhenSomeWordIsWeak() {
        assertFalse(TranscriptVote.needsRecheck(run("drink" to 0.9f, "water" to 0.8f)))
        assertTrue(TranscriptVote.needsRecheck(run("drink" to 0.9f, "wader" to 0.3f)))
        assertTrue(TranscriptVote.needsRecheck(emptyList()))
    }

    @Test
    fun identicalTranscriptWinsByMajority() {
        val a = run("drink" to 0.9f, "wader" to 0.3f)
        val b = run("drink" to 0.9f, "water" to 0.5f)
        val c = run("Drink" to 0.8f, "water" to 0.7f)
        assertEquals("drink water", TranscriptVote.choose(listOf(a, b, c)).text().lowercase())
    }

    @Test
    fun neverMixesWordsOfDifferentRuns() {
        val a = run("call" to 0.9f, "mom" to 0.4f, "now" to 0.9f)
        val b = run("call" to 0.9f, "mum" to 0.5f, "no" to 0.5f)
        val c = run("cool" to 0.5f, "mom" to 0.6f, "now" to 0.9f)
        assertEquals("call mom now", TranscriptVote.choose(listOf(a, b, c)).text())
        val d = run("go" to 0.7f, "home" to 0.7f)
        val e = run("so" to 0.9f, "hum" to 0.4f)
        assertEquals("go home", TranscriptVote.choose(listOf(e, d)).text())
    }

    @Test
    fun differentLengthsFallBackToMostConfidentRun() {
        val a = run("walk" to 0.9f, "dog" to 0.9f)
        val b = run("walk" to 0.4f, "the" to 0.4f, "dog" to 0.4f)
        assertEquals("walk dog", TranscriptVote.choose(listOf(b, a)).text())
    }
}

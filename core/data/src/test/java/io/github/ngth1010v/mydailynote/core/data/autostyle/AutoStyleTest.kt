package io.github.ngth1010v.mydailynote.core.data.autostyle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class AutoStyleTest {
    /** 5 shade rows x 4 hue columns, keyed "c<column>r<row>". */
    private val palette = List(5) { r -> List(4) { c -> "c${c}r$r" } }
    private fun cell(key: String) = key.substring(1).split('r').let { (c, r) -> r.toInt() to c.toInt() }

    /** Texts are unit vectors at an angle (degrees): similarity = cos(angle difference). */
    private val angles = mapOf(
        "drink coffee" to 0.0, "coffee" to 0.0, "tea" to 30.0, "latte" to 10.0,
        "espresso" to 60.0, "bakery" to 58.0, "run" to 180.0, "Gym" to 175.0,
    )
    private fun embed(t: String): FloatArray {
        val a = Math.toRadians(angles.getValue(t))
        return floatArrayOf(cos(a).toFloat(), sin(a).toFloat())
    }

    private val icons = listOf(
        IconMeaning("coffee", 0, "coffee"), IconMeaning("tea", 0, "tea"),
        IconMeaning("bakery", 1, "bakery"), IconMeaning("run", 2, "run"),
    )
    private fun style() = AutoStyle(::embed, icons, palette, Random(7))

    @Test fun sameTextReusesStyle() {
        val history = listOf(StyledText("Drink Coffee", "c1r2", "tea"))
        assertEquals(Style("c1r2", "tea"), style().pick("drink coffee", history))
    }

    @Test fun veryRelatedKeepsIconAndShiftsOneShade() {
        val picked = style().pick("latte", listOf(StyledText("coffee", "c2r2", "coffee"), StyledText("run", "c0r2", "run")))
        assertEquals("coffee", picked.icon)
        val (r, c) = cell(picked.color)
        assertEquals(2, c)
        assertEquals(1, abs(r - 2))
    }

    @Test fun relatedPicksIconInsideTheGroupAndAFurtherColor() {
        // espresso is 60° from coffee (0.5): related; best icon overall would be "bakery" (58°), but
        // it must come from the coffee icon's group.
        val picked = style().pick("espresso", listOf(StyledText("coffee", "c1r2", "coffee")))
        assertEquals("tea", picked.icon)
        val (r, c) = cell(picked.color)
        assertTrue(picked.color, (c == 1 && abs(r - 2) >= 2) || (r == 2 && abs(c - 1) == 1))
    }

    @Test fun unrelatedPicksIconByMeaningAndTheLeastUsedHue() {
        val history = listOf(
            StyledText("coffee", "c0r1", "coffee"), StyledText("tea", "c1r2", "tea"), StyledText("bakery", "c3r3", "bakery"),
        )
        val picked = style().pick("Gym", history)
        assertEquals("run", picked.icon)
        assertEquals(2, cell(picked.color).second)
        assertTrue(cell(picked.color).first in 1..3)
    }

    @Test fun noHistoryPicksIconByMeaning() {
        val picked = style().pick("run", emptyList())
        assertEquals("run", picked.icon)
        assertTrue(cell(picked.color).first in 1..3)
    }
}

/** Runs against the shipped model file. */
class TextEmbedderTest {
    private val embedder = File("src/main/assets/autostyle/embeddings.bin").inputStream().use(::TextEmbedder)

    private fun pieces(text: String) = embedder.tokenize(text).map { embedder.piece(it) }
    private fun sim(a: String, b: String) = embedder.embed(a).zip(embedder.embed(b)).sumOf { (x, y) -> (x * y).toDouble() }

    @Test fun tokenizesLikeTheSourceTokenizer() {
        // Expected pieces from the original tokenizer.json (lowercased input).
        assertEquals(listOf("▁drink", "▁coffee"), pieces("Drink coffee"))
        assertEquals(listOf("▁cà", "▁phê", "▁sữa", "▁đá"), pieces("Cà phê sữa đá"))
        assertEquals(listOf("▁gọi", "▁mẹ", "▁lúc", "▁8h"), pieces("gọi mẹ lúc 8h"))
        assertEquals(listOf("▁code", "▁review", "▁!"), pieces("Code review!"))
    }

    @Test fun relatedTextsScoreHigher() {
        assertTrue(sim("Gọi mẹ", "Call mom") > 0.6)
        assertTrue(sim("Clean kitchen", "Dọn nhà") > sim("Clean kitchen", "Morning run"))
        assertTrue(sim("Morning run", "Chạy bộ") > sim("Morning run", "Reply emails"))
        assertEquals(0.0, embedder.embed("").sumOf { abs(it).toDouble() }, 0.0)
    }
}

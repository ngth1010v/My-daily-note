package io.github.ngth1010v.mydailynote.speech.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class PromptVocabularyTest {
    private val phrases = listOf("Cà phê", "Walk dog", "Đi bộ", "Gym", "Uống nước")

    @Test
    fun keepsOnlyPhrasesInTheClipLanguage() {
        assertEquals("Cà phê, Đi bộ, Uống nước", PromptVocabulary.prompt(phrases, "vi"))
        assertEquals("Walk dog, Gym", PromptVocabulary.prompt(phrases, "en"))
        assertNull(PromptVocabulary.prompt(listOf("Cà phê"), "en"))
    }

    @Test
    fun capsThePromptLength() {
        val many = (1..30).map { "task $it" }
        assertEquals(PromptVocabulary.MAX_PHRASES, PromptVocabulary.prompt(many, "en")!!.split(", ").size)
    }
}

class NoiseLevelTest {
    /** 1 s of "speech" (a tone) between 1 s stretches of background at [noise] amplitude. */
    private fun clip(noise: Int): ShortArray {
        val rnd = Random(1)
        return ShortArray(48_000) { i ->
            val bg = rnd.nextInt(-noise, noise + 1)
            val tone = if (i in 16_000 until 32_000) (8000 * sin(2 * PI * 220 * i / 16_000)).toInt() else 0
            (bg + tone).coerceIn(-32768, 32767).toShort()
        }
    }

    @Test
    fun quietRoomIsNotDenoised() {
        assertFalse(NoiseLevel.isNoisy(clip(noise = 30)))
    }

    @Test
    fun loudBackgroundIsDenoised() {
        assertTrue(NoiseLevel.isNoisy(clip(noise = 3000)))
    }

    @Test
    fun tooShortToTell() {
        assertNull(NoiseLevel.snrDb(ShortArray(1000)))
    }
}

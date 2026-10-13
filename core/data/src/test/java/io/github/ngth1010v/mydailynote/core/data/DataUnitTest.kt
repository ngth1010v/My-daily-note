package io.github.ngth1010v.mydailynote.core.data

import io.github.ngth1010v.mydailynote.core.data.backup.BackupCodec
import io.github.ngth1010v.mydailynote.core.data.backup.BackupData
import io.github.ngth1010v.mydailynote.core.data.backup.BackupFormatException
import io.github.ngth1010v.mydailynote.core.data.model.Entry
import io.github.ngth1010v.mydailynote.core.data.model.EntrySource
import io.github.ngth1010v.mydailynote.core.data.model.Group
import io.github.ngth1010v.mydailynote.core.data.model.TextNormalizer
import io.github.ngth1010v.mydailynote.core.data.model.dayOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class TextNormalizerTest {
    @Test fun snapsVoiceTextToKnownAction() {
        val known = listOf("Cà phê", "Walk dog", "Call mom")
        assertEquals("Cà phê", TextNormalizer.closestKnown("ca phe", known))
        assertEquals("Walk dog", TextNormalizer.closestKnown("walk dogs", known))
        assertNull(TextNormalizer.closestKnown("call dad", known))
        assertNull(TextNormalizer.closestKnown("read a long book", known))
    }

    @Test fun stripsVietnameseDiacritics() {
        assertEquals("ca phe khong duong", TextNormalizer.normalize("  Cà  phê KHÔNG đường "))
        assertEquals("di bo", TextNormalizer.normalize("Đi bộ"))
    }

    @Test fun ftsQueryUsesPrefixTokens() {
        assertEquals("\"ca\"* \"phe\"*", TextNormalizer.ftsQuery("Cà phê!"))
        assertNull(TextNormalizer.ftsQuery("  ?! "))
    }

    @Test fun likePrefixEscapesWildcards() {
        assertEquals("100\\% \\_x", TextNormalizer.likePrefix("100% _x"))
    }
}

class DayKeyTest {
    private val plus7 = ZoneOffset.ofHours(7)

    @Test fun usesStoredOffset() {
        // 2026-10-11T18:30Z is 01:30 on the 12th in UTC+7.
        val t = Instant.parse("2026-10-11T18:30:00Z")
        assertEquals(LocalDate.of(2026, 10, 12), dayOf(t, plus7, 0))
        assertEquals(LocalDate.of(2026, 10, 11), dayOf(t, ZoneOffset.UTC, 0))
    }

    @Test fun dayStartHourMovesEarlyMorningToPreviousDay() {
        val t = Instant.parse("2026-10-11T18:30:00Z") // 01:30 local
        assertEquals(LocalDate.of(2026, 10, 11), dayOf(t, plus7, 4))
        assertEquals(LocalDate.of(2026, 10, 12), dayOf(t, plus7, 1))
    }
}

class BackupCodecTest {
    private val plus7 = ZoneOffset.ofHours(7)
    private val entries = listOf(
        Entry(
            text = "uống nước, 2 ly \"to\"",
            description = "line1\nline2",
            achievedAt = Instant.parse("2026-10-12T00:15:00Z"),
            zoneOffset = plus7,
            createdAt = Instant.parse("2026-10-12T00:16:00Z"),
            colorOverride = "blue",
            iconOverride = "water",
            groupId = 5,
            source = EntrySource.VOICE,
        ),
        Entry(text = "gym", achievedAt = Instant.parse("2026-10-12T01:00:00Z"), zoneOffset = ZoneOffset.UTC),
    )

    @Test fun jsonRoundTrip() {
        val groups = listOf(Group(5, "Drink", "blue", "water", null, Instant.parse("2026-01-01T00:00:00Z")))
        val json = BackupCodec.encodeJson(BackupData(groups, entries), Instant.EPOCH)
        val back = BackupCodec.decodeJson(json)
        assertEquals(groups, back.groups)
        assertEquals(entries.map { it.copy(updatedAt = Instant.EPOCH) }, back.entries.map { it.copy(updatedAt = Instant.EPOCH) })
    }

    @Test fun csvRoundTrip() {
        val back = BackupCodec.decodeCsv(BackupCodec.encodeCsv(entries))
        assertEquals(2, back.size)
        assertEquals(entries[0].text, back[0].text)
        assertEquals(entries[0].description, back[0].description)
        assertEquals(entries[0].achievedAt, back[0].achievedAt)
        assertEquals(plus7, back[0].zoneOffset)
        assertEquals(EntrySource.VOICE, back[0].source)
        assertNull(back[1].description)
    }

    @Test(expected = BackupFormatException::class)
    fun csvWithoutRequiredColumnsFails() {
        BackupCodec.decodeCsv("foo,bar\n1,2\n")
    }

    @Test(expected = BackupFormatException::class)
    fun foreignJsonFails() {
        BackupCodec.decodeJson("{\"app\":\"other\"}")
    }
}

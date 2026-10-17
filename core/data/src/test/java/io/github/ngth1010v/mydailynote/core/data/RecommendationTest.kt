package io.github.ngth1010v.mydailynote.core.data

import io.github.ngth1010v.mydailynote.core.data.model.Entry
import io.github.ngth1010v.mydailynote.core.data.model.RecommendOptions
import io.github.ngth1010v.mydailynote.core.data.model.recommend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class RecommendationTest {
    private val utc = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 17)

    private fun entry(text: String, daysAgo: Long, time: String) = Entry(
        text = text,
        achievedAt = today.minusDays(daysAgo).atTime(LocalTime.parse(time)).toInstant(utc),
        zoneOffset = utc,
    )

    /** Coffee ~07:30 every day, lunch ~12:00 every day, a run once. */
    private val history = (1L..20L).flatMap { d ->
        listOf(entry("coffee", d, if (d % 2 == 0L) "07:20" else "07:40"), entry("Lunch", d, "12:00"))
    } + entry("run", 3, "18:00")

    @Test fun habitsAreRecommendedAtTheirUsualTimeOldestFirst() {
        val recs = recommend(history, today, 0, utc)
        assertEquals(listOf("coffee", "Lunch"), recs.map { it.text })
        assertEquals(LocalTime.of(7, 40), recs[0].at.atOffset(utc).toLocalTime())
        assertTrue(recs.all { it.confidence > 0.6f })
    }

    @Test fun doneTodayOrDismissedIsHidden() {
        val coffee = recommend(history, today, 0, utc).first()
        val done = recommend(history + entry("Coffee", 0, "08:10"), today, 0, utc)
        assertEquals(listOf("Lunch"), done.map { it.text })
        assertEquals(listOf("Lunch"), recommend(history, today, 0, utc, setOf(coffee.key)).map { it.text })
    }

    @Test fun littleHistoryIsNotConfident() {
        // One day of data: damped well below the cut-off.
        assertTrue(recommend(listOf(entry("coffee", 1, "07:00")), today, 0, utc).isEmpty())
    }

    @Test fun optionsTuneWhatIsShown() {
        // "run" happened on one day only: no habit by default, shown once one day is enough and the bar is low.
        assertTrue("run" !in recommend(history, today, 0, utc).map { it.text })
        val loose = RecommendOptions(minConfidencePercent = 1, minDays = 1)
        assertTrue("run" in recommend(history, today, 0, utc, options = loose).map { it.text })
        assertEquals(1, recommend(history, today, 0, utc, options = RecommendOptions(usualCount = 1, maxCount = 1)).size)
        assertTrue(recommend(history, today, 0, utc, options = RecommendOptions(minConfidencePercent = 95)).isEmpty())
    }

    @Test fun weeklyHabitShowsOnItsWeekdayOnly() {
        // Gym every 7th day for 8 weeks, among daily coffee: only today's weekday has it.
        val weekly = history + (1L..8L).map { entry("gym", it * 7, "18:00") }
        assertTrue("gym" in recommend(weekly, today, 0, utc).map { it.text })
        assertTrue("gym" !in recommend(weekly, today.plusDays(1), 0, utc).map { it.text })
    }

    @Test fun dayStartShiftsTheDay() {
        // Logged at 01:00 with the day starting at 04:00: belongs to the day before, still recommended.
        val late = (1L..10L).map { entry("read", it - 1, "01:00") }
        val recs = recommend(late, today, 4, utc)
        assertEquals(listOf("read"), recs.map { it.text })
        assertEquals(today.plusDays(1).atTime(1, 0).toInstant(utc), recs[0].at)
    }
}

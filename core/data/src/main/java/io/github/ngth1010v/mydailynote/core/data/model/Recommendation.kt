package io.github.ngth1010v.mydailynote.core.data.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.exp

/** An action the user usually logs around [at] on a day like today, but hasn't logged yet. */
data class Recommendation(
    val text: String,
    val color: String?,
    val icon: String?,
    val at: Instant,
    /** 0..1: how sure the guess is (share of recent days with this habit, damped for little history). */
    val confidence: Float,
) {
    /** Stable for the whole day (history before today doesn't change): used to dismiss it. */
    val key: String get() = "${TextNormalizer.normalize(text)}@${at.epochSecond / 60}"
}

/** User-tunable knobs of [recommend] (Settings → Suggestions). */
data class RecommendOptions(
    /** Habits scoring below this (0..100) are not shown. */
    val minConfidencePercent: Int = 25,
    /** Shown when unsure; more (up to [maxCount]) only when very sure. */
    val usualCount: Int = 5,
    val maxCount: Int = 10,
    /** How many days back the habits are learned from. */
    val historyDays: Int = 56,
    /** A day on today's weekday counts this many times more: weekly habits (Monday gym) show on their day. 1 = off. */
    val weekdayWeight: Int = 4,
    /** A habit needs to have happened on at least this many days. */
    val minDays: Int = 2,
)

/** Times of one action further apart than this (minutes) are different habits. */
private const val SLOT_GAP = 90
/** A logged entry this close (minutes) to a habit's times counts as having done it. */
private const val DONE_MARGIN = 60
/** Days of history the confidence is damped by: 3 days alike say less than 30. */
private const val PRIOR_DAYS = 3.0
/** A day [HALF_LIFE] days ago counts half as much as yesterday. */
private const val HALF_LIFE = 21.0
/** Past [RecommendOptions.usualCount], only habits at least this sure are added. */
private const val SURE_CONFIDENCE = 0.6f

/**
 * Today's recommendations from [entries] (any order; those before [RecommendOptions.historyDays] are ignored):
 * each action's times of day are split into habits, and each habit not done yet today (nor in
 * [dismissed] keys) is scored by the recency- and weekday-weighted share of active days it happened on. Up to
 * [RecommendOptions.usualCount] are kept, more when very sure, fewer when unsure. Oldest first.
 */
fun recommend(
    entries: List<Entry>,
    today: LocalDate,
    dayStartHour: Int,
    offset: ZoneOffset,
    dismissed: Set<String> = emptySet(),
    options: RecommendOptions = RecommendOptions(),
): List<Recommendation> {
    val from = today.minusDays(options.historyDays.toLong())
    // Minutes since the day started, so a habit never wraps around midnight.
    fun minuteOf(e: Entry): Int {
        val t = e.achievedAt.atOffset(e.zoneOffset).minusHours(dayStartHour.toLong()).toLocalTime()
        return t.hour * 60 + t.minute
    }
    val past = entries.filter { val d = it.day(dayStartHour); d < today && d >= from }
    val done = entries.filter { it.day(dayStartHour) == today }
        .groupBy({ TextNormalizer.normalize(it.text) }, ::minuteOf)

    fun weight(day: LocalDate) = exp(-ChronoUnit.DAYS.between(day, today) * Math.log(2.0) / HALF_LIFE) *
        (if (day.dayOfWeek == today.dayOfWeek) options.weekdayWeight.toDouble() else 1.0)
    val activeWeight = past.mapTo(HashSet()) { it.day(dayStartHour) }.sumOf(::weight) + PRIOR_DAYS
    val dayStart = today.atTime(dayStartHour, 0).toInstant(offset)

    val all = past.groupBy { TextNormalizer.normalize(it.text) }.flatMap { (searchText, list) ->
        val latest = list.maxBy { it.achievedAt }
        // ponytail: an action done many times a day (hourly water) chains into one wide habit; split by count if that matters.
        slots(list.map { minuteOf(it) to it.day(dayStartHour) }.sortedBy { it.first }).mapNotNull { slot ->
            val minutes = slot.map { it.first }
            val first = minutes.first() - DONE_MARGIN
            val last = minutes.last() + DONE_MARGIN
            if (done[searchText].orEmpty().any { it in first..last }) return@mapNotNull null
            val days = slot.mapTo(HashSet()) { it.second }
            if (days.size < options.minDays) return@mapNotNull null
            val hits = days.sumOf(::weight)
            Recommendation(
                text = latest.text,
                color = latest.color,
                icon = latest.icon,
                at = dayStart.plusSeconds(minutes[minutes.size / 2] * 60L),
                confidence = (hits / activeWeight).toFloat(),
            )
        }
    }
    val min = options.minConfidencePercent / 100f
    return all.filter { it.confidence >= min && it.key !in dismissed }
        .sortedByDescending { it.confidence }
        .filterIndexed { i, r -> i < options.usualCount || r.confidence >= maxOf(SURE_CONFIDENCE, min) }
        .take(options.maxCount)
        .sortedBy { it.at }
}

/** Splits (minute, day) pairs sorted by minute wherever two times are more than [SLOT_GAP] apart. */
private fun slots(times: List<Pair<Int, LocalDate>>): List<List<Pair<Int, LocalDate>>> {
    val out = ArrayList<MutableList<Pair<Int, LocalDate>>>()
    for (t in times) {
        if (out.isEmpty() || t.first - out.last().last().first > SLOT_GAP) out.add(mutableListOf())
        out.last().add(t)
    }
    return out
}

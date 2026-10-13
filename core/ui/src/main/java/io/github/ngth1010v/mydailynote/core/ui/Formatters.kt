package io.github.ngth1010v.mydailynote.core.ui

import android.content.Context
import android.text.format.DateFormat
import io.github.ngth1010v.mydailynote.core.data.settings.TimeFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Locale-aware day labels and entry times. Create once per configuration and reuse. */
class Formatters(private val context: Context, timeFormat: TimeFormat) {
    private val locale: Locale = context.resources.configuration.locales[0]

    val use24h = when (timeFormat) {
        TimeFormat.SYSTEM -> DateFormat.is24HourFormat(context)
        TimeFormat.H12 -> false
        TimeFormat.H24 -> true
    }

    private val time = DateTimeFormatter.ofPattern(
        DateFormat.getBestDateTimePattern(locale, if (use24h) "Hm" else "hm"), locale,
    )
    private val time24 = DateTimeFormatter.ofPattern(
        DateFormat.getBestDateTimePattern(locale, "Hm"), locale,
    )
    private val weekday = DateTimeFormatter.ofPattern("EEEE", locale)
    private val monthDay = DateTimeFormatter.ofPattern(
        DateFormat.getBestDateTimePattern(locale, "MMMd"), locale,
    )
    private val fullDate = DateTimeFormatter.ofPattern(
        DateFormat.getBestDateTimePattern(locale, "MMMdyyyy"), locale,
    )

    fun time(at: OffsetDateTime): String = time.format(at)

    /** Always 24-hour, regardless of the time format setting. */
    fun time24(at: OffsetDateTime): String = time24.format(at)

    /** "<weekday>, <month day>", plus " (today)" / " (yesterday)" when it matches. */
    fun day(date: LocalDate, today: LocalDate): String {
        val label = weekday.format(date) + ", " + monthDay.format(date)
        val relative = when (date) {
            today -> context.getString(R.string.day_today)
            today.minusDays(1) -> context.getString(R.string.day_yesterday)
            else -> return label
        }
        return "$label (${relative.lowercase(locale)})"
    }

    fun date(date: LocalDate): String = fullDate.format(date)
}

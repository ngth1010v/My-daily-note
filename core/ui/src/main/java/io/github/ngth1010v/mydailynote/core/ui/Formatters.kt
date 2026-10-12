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
    private val dayThisYear = DateTimeFormatter.ofPattern(
        DateFormat.getBestDateTimePattern(locale, "EEEMMMd"), locale,
    )
    private val dayOtherYear = DateTimeFormatter.ofPattern(
        DateFormat.getBestDateTimePattern(locale, "EEEMMMdyyyy"), locale,
    )
    private val fullDate = DateTimeFormatter.ofPattern(
        DateFormat.getBestDateTimePattern(locale, "MMMdyyyy"), locale,
    )

    fun time(at: OffsetDateTime): String = time.format(at)

    fun day(date: LocalDate, today: LocalDate): String = when (date) {
        today -> context.getString(R.string.day_today)
        today.minusDays(1) -> context.getString(R.string.day_yesterday)
        else -> (if (date.year == today.year) dayThisYear else dayOtherYear).format(date)
    }

    fun date(date: LocalDate): String = fullDate.format(date)
}

package io.github.ngth1010v.mydailynote.core.data.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The calendar day an entry belongs to. Uses the offset stored with the entry (so travelling
 * doesn't reshuffle history) and the user's "day starts at" hour (so 01:00 can count as yesterday).
 */
fun dayOf(instant: Instant, offset: ZoneOffset, dayStartHour: Int): LocalDate =
    instant.atOffset(offset).minusHours(dayStartHour.toLong()).toLocalDate()

fun Entry.day(dayStartHour: Int): LocalDate = dayOf(achievedAt, zoneOffset, dayStartHour)

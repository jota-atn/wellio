package com.projeto.wellio.domain.time

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/** Primeiro e último dia da semana que contém [day]. */
fun weekOf(day: LocalDate, weekStart: DayOfWeek = DayOfWeek.MONDAY): ClosedRange<LocalDate> {
    val start = day.with(TemporalAdjusters.previousOrSame(weekStart))
    return start..start.plusDays(6)
}

/** Tempo até a próxima meia-noite, quando o dia vira. */
fun untilNextDay(now: ZonedDateTime): Duration {
    val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return Duration.between(now, nextMidnight)
}

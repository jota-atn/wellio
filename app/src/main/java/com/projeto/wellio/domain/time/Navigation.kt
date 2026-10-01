package com.projeto.wellio.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Dias futuros não podem ser abertos nem receber registros. */
fun canOpen(day: LocalDate, today: LocalDate): Boolean = !day.isAfter(today)

fun previousDay(day: LocalDate): LocalDate = day.minusDays(1)

/** Dia seguinte, ou null se passaria de hoje. */
fun nextDay(day: LocalDate, today: LocalDate): LocalDate? = day.plusDays(1).takeIf { canOpen(it, today) }

/** Momento de um registro feito num [day] e [time] escolhidos. */
fun instantAt(day: LocalDate, time: LocalTime, zone: ZoneId): Instant = day.atTime(time).atZone(zone).toInstant()

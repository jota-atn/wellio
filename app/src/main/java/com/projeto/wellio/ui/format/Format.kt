package com.projeto.wellio.ui.format

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ptBR: Locale = Locale.forLanguageTag("pt-BR")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** "1.400 ml" */
fun formatMl(ml: Int): String = "${NumberFormat.getIntegerInstance(ptBR).format(ml)} ml"

/** "08:10" */
fun formatTime(instant: Instant, zone: ZoneId): String = timeFormatter.format(instant.atZone(zone))

/** "Hoje", "Ontem" ou "Qui, 1 out" (com o ano se não for o atual). */
fun dayLabel(day: LocalDate, today: LocalDate): String = when (day) {
    today -> "Hoje"
    today.minusDays(1) -> "Ontem"
    else -> {
        val weekday = day.dayOfWeek.getDisplayName(TextStyle.SHORT, ptBR).trimEnd('.').replaceFirstChar { it.uppercase() }
        val month = day.month.getDisplayName(TextStyle.SHORT, ptBR).trimEnd('.')
        val year = if (day.year != today.year) " ${day.year}" else ""
        "$weekday, ${day.dayOfMonth} $month$year"
    }
}

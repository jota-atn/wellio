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

/** "72,4 kg" */
fun formatKg(kg: Double): String = "${decimal(kg)} kg"

/** "+0,3 kg", "−0,7 kg" ou "0,0 kg" */
fun formatKgChange(kg: Double): String = when {
    kg > 0 -> "+${decimal(kg)} kg"
    kg < 0 -> "−${decimal(-kg)} kg"
    else -> "0,0 kg"
}

private fun decimal(value: Double): String =
    NumberFormat.getNumberInstance(ptBR).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }.format(value)

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

/** "28 set", ou "28 set 2025" se não for do ano atual de [today]. */
fun shortDate(day: LocalDate, today: LocalDate): String {
    val month = day.month.getDisplayName(TextStyle.SHORT, ptBR).trimEnd('.')
    val year = if (day.year != today.year) " ${day.year}" else ""
    return "${day.dayOfMonth} $month$year"
}

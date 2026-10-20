package com.projeto.wellio.ui.goals

import com.projeto.wellio.domain.goals.PeriodKind
import com.projeto.wellio.domain.goals.PeriodMetric
import com.projeto.wellio.ui.format.formatInt
import com.projeto.wellio.ui.format.formatKg
import com.projeto.wellio.ui.format.formatLiters
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val ptBR: Locale = Locale.forLanguageTag("pt-BR")
private val shortDay = DateTimeFormatter.ofPattern("dd/MM")

/** Nome e exemplo de cada métrica, para a escolha na criação (SPEC §9.2). */
fun metricLabel(metric: PeriodMetric): String = when (metric) {
    PeriodMetric.WaterTotal -> "Água no total"
    PeriodMetric.WaterDays -> "Dias batendo a água"
    PeriodMetric.KcalTotal -> "Calorias no total"
    PeriodMetric.KcalDays -> "Dias dentro da meta de calorias"
    PeriodMetric.ExerciseKcalTotal -> "kcal queimadas no total"
    PeriodMetric.ExerciseMinutesTotal -> "Minutos de exercício no total"
    PeriodMetric.ExerciseDays -> "Dias batendo o exercício"
    PeriodMetric.Workouts -> "Número de treinos"
    PeriodMetric.Weight -> "Peso alvo"
}

fun metricExample(metric: PeriodMetric): String = when (metric) {
    PeriodMetric.WaterTotal -> "Beber 60 L em outubro"
    PeriodMetric.WaterDays -> "25 dias batendo a água no mês"
    PeriodMetric.KcalTotal -> "14.000 kcal na semana"
    PeriodMetric.KcalDays -> "5 dias dentro da meta na semana"
    PeriodMetric.ExerciseKcalTotal -> "Queimar 50.000 kcal no ano"
    PeriodMetric.ExerciseMinutesTotal -> "150 min por semana"
    PeriodMetric.ExerciseDays -> "4 dias batendo o exercício na semana"
    PeriodMetric.Workouts -> "12 treinos no mês"
    PeriodMetric.Weight -> "Chegar a 75 kg até dezembro"
}

/** Valor de uma métrica: "34 L", "12.500 kcal", "150 min", "12 dias", "3 treinos", "72,4 kg". */
fun formatMetric(metric: PeriodMetric, value: Double): String = when (metric) {
    PeriodMetric.WaterTotal -> formatLiters(value.roundToInt())
    PeriodMetric.KcalTotal, PeriodMetric.ExerciseKcalTotal -> "${formatInt(value.roundToInt())} kcal"
    PeriodMetric.ExerciseMinutesTotal -> "${formatInt(value.roundToInt())} min"
    PeriodMetric.WaterDays, PeriodMetric.KcalDays, PeriodMetric.ExerciseDays ->
        value.roundToInt().let { if (it == 1) "1 dia" else "$it dias" }
    PeriodMetric.Workouts -> value.roundToInt().let { if (it == 1) "1 treino" else "$it treinos" }
    PeriodMetric.Weight -> formatKg(value)
}

/** "por semana", "em outubro", "nesta semana", "até 15/11"... */
fun periodPhrase(kind: PeriodKind, start: LocalDate, end: LocalDate, recurring: Boolean, today: LocalDate): String = when {
    recurring && kind == PeriodKind.Week -> "por semana"
    recurring && kind == PeriodKind.Month -> "por mês"
    recurring && kind == PeriodKind.Year -> "por ano"
    kind == PeriodKind.Week -> if (today in start..end) "nesta semana" else "na semana de ${start.format(shortDay)}"
    kind == PeriodKind.Month -> "em " + start.month.getDisplayName(TextStyle.FULL, ptBR) +
        (if (start.year != today.year) " de ${start.year}" else "")
    kind == PeriodKind.Year -> "em ${start.year}"
    else -> "até ${end.format(shortDay)}"
}

/** Nome gerado para a meta (SPEC §9.2), editável: "Beber 60 L em outubro". */
fun periodGoalTitle(metric: PeriodMetric, target: Double, kind: PeriodKind, start: LocalDate, end: LocalDate, recurring: Boolean, today: LocalDate): String {
    val value = formatMetric(metric, target)
    val period = periodPhrase(kind, start, end, recurring, today)
    return when (metric) {
        PeriodMetric.WaterTotal -> "Beber $value $period"
        PeriodMetric.WaterDays -> "$value batendo a água $period"
        PeriodMetric.KcalTotal -> "$value $period"
        PeriodMetric.KcalDays -> "$value dentro da meta de calorias $period"
        PeriodMetric.ExerciseKcalTotal -> "Queimar $value $period"
        PeriodMetric.ExerciseMinutesTotal -> "$value de exercício $period"
        PeriodMetric.ExerciseDays -> "$value batendo o exercício $period"
        PeriodMetric.Workouts -> "$value $period"
        PeriodMetric.Weight -> "Chegar a $value até ${end.format(shortDay)}"
    }
}

/** Inicial do dia da semana ("S", "T", "Q"...). */
fun weekdayInitial(day: LocalDate): String =
    day.dayOfWeek.getDisplayName(TextStyle.NARROW, ptBR).uppercase(ptBR)

/** Nome do dia da semana, para o início da semana ("segunda"). */
fun weekdayName(day: java.time.DayOfWeek): String =
    day.getDisplayName(TextStyle.FULL, ptBR).removeSuffix("-feira")

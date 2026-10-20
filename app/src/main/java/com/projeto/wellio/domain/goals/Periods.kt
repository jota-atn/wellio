package com.projeto.wellio.domain.goals

import com.projeto.wellio.domain.model.ExerciseUnit
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.domain.model.Goals
import com.projeto.wellio.domain.model.Totals
import com.projeto.wellio.domain.time.weekOf
import com.projeto.wellio.domain.weight.WeightGoal
import com.projeto.wellio.domain.weight.goalProgress
import com.projeto.wellio.domain.weight.remainingKg
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Metas diárias que valiam em [day] (SPEC §9.2): a mudança mais recente até aquele dia.
 * Antes da primeira mudança registrada vale a primeira; sem histórico, as metas de agora.
 */
fun goalsOn(day: LocalDate, history: List<DailyGoalsSnapshot>, current: Goals): Goals =
    history.lastOrNull { !it.effectiveFrom.isAfter(day) }?.goals ?: history.firstOrNull()?.goals ?: current

/** Os 7 dias da semana de [day], começando em [weekStart]. */
fun weekDays(day: LocalDate, weekStart: DayOfWeek): List<LocalDate> {
    val start = weekOf(day, weekStart).start
    return (0L..6L).map { start.plusDays(it) }
}

/** Quanto cada anel fechou, de 0 a 1 (SPEC §5.4): água por fora, kcal no meio, exercício por dentro. */
data class Rings(val water: Float, val food: Float, val exercise: Float) {
    val allClosed: Boolean get() = water >= 1f && food >= 1f && exercise >= 1f

    /** "% do dia": a média dos três (a mesma conta do mini-gráfico do card). */
    val score: Float get() = (water + food + exercise) / 3
}

/** Anéis de um dia, com exercício na unidade da meta. */
fun rings(totals: Totals, goals: Goals): Rings {
    fun ring(type: GoalType) = progress(totals.toward(type, goals), goals[type].daily)
    return Rings(water = ring(GoalType.WATER), food = ring(GoalType.FOOD), exercise = ring(GoalType.EXERCISE))
}

/** Intervalo do período de [kind] que contém [day]; [PeriodKind.Custom] não tem um padrão. */
fun periodContaining(kind: PeriodKind, day: LocalDate, weekStart: DayOfWeek): ClosedRange<LocalDate> = when (kind) {
    PeriodKind.Week -> weekOf(day, weekStart)
    PeriodKind.Month -> day.withDayOfMonth(1)..day.with(TemporalAdjusters.lastDayOfMonth())
    PeriodKind.Year -> day.withDayOfYear(1)..day.with(TemporalAdjusters.lastDayOfYear())
    PeriodKind.Custom -> day..day
}

/** Dias do intervalo, com as duas pontas. */
fun daysIn(range: ClosedRange<LocalDate>): Int = ChronoUnit.DAYS.between(range.start, range.endInclusive).toInt() + 1

/** O que foi registrado num dia, para avaliar as metas de período. */
data class DayLog(val totals: Totals = Totals(), val workouts: Int = 0)

/** Dia que bate a métrica de "dias" com a meta que valia nele (kcal: dentro de ±10%, com algo registrado). */
fun dayHits(metric: PeriodMetric, log: DayLog, goals: Goals): Boolean = when (metric) {
    PeriodMetric.WaterDays -> log.totals.waterMl >= goals.water.daily
    PeriodMetric.KcalDays -> log.totals.kcalConsumed > 0 &&
        abs(log.totals.kcalConsumed - goals.food.daily) <= goals.food.daily * 0.1
    PeriodMetric.ExerciseDays -> log.totals.exercise(goals.exerciseUnit) >= goals.exercise.daily
    else -> false
}

/** Valor de um dia para as métricas de total. */
private fun dayValue(metric: PeriodMetric, log: DayLog): Double = when (metric) {
    PeriodMetric.WaterTotal -> log.totals.waterMl.toDouble()
    PeriodMetric.KcalTotal -> log.totals.kcalConsumed.toDouble()
    PeriodMetric.ExerciseKcalTotal -> log.totals.kcalBurned.toDouble()
    PeriodMetric.ExerciseMinutesTotal -> log.totals.exerciseMinutes.toDouble()
    PeriodMetric.Workouts -> log.workouts.toDouble()
    else -> 0.0
}

/**
 * Valor alcançado até [today] (ou até o fim, se já passou): soma das métricas de total ou
 * contagem de dias batidos, cada dia com a meta que valia nele. Peso não passa por aqui.
 */
fun periodValue(
    metric: PeriodMetric,
    range: ClosedRange<LocalDate>,
    today: LocalDate,
    logs: Map<LocalDate, DayLog>,
    goalsFor: (LocalDate) -> Goals
): Double {
    val last = minOf(range.endInclusive, today)
    if (last.isBefore(range.start)) return 0.0
    return generateSequence(range.start) { it.plusDays(1) }
        .takeWhile { !it.isAfter(last) }
        .sumOf { day ->
            val log = logs[day] ?: DayLog()
            if (metric.countsDays) (if (dayHits(metric, log, goalsFor(day))) 1.0 else 0.0) else dayValue(metric, log)
        }
}

/** Andamento de uma meta de período (SPEC §9.2). */
data class PeriodProgress(
    val value: Double,
    val target: Double,
    /** Fração alcançada, 0–1. */
    val fraction: Float,
    /** Onde deveria estar hoje, 0–1 (dias passados / dias do período). */
    val expected: Float,
    val daysLeft: Int,
    /** Valor no fim do período pelo ritmo até agora; null antes de passar um dia. */
    val projection: Double?
) {
    val reached: Boolean get() = value >= target
}

/** Andamento de uma métrica de total ou de dias, com a projeção pelo ritmo médio diário. */
fun periodProgress(goal: PeriodGoal, value: Double, today: LocalDate): PeriodProgress {
    val range = goal.start..goal.end
    val total = daysIn(range)
    val elapsed = when {
        today.isBefore(goal.start) -> 0
        today.isAfter(goal.end) -> total
        else -> daysIn(goal.start..today)
    }
    val daysLeft = (total - elapsed).coerceAtLeast(0)
    return PeriodProgress(
        value = value,
        target = goal.target,
        fraction = if (goal.target <= 0) 1f else (value / goal.target).toFloat().coerceIn(0f, 1f),
        expected = elapsed.toFloat() / total,
        daysLeft = daysLeft,
        projection = if (elapsed == 0) null else value / elapsed * total
    )
}

/** Valor sugerido ao criar (SPEC §9.2): o do período anterior igual, ou a meta diária × dias. */
fun suggestedTarget(metric: PeriodMetric, days: Int, previousValue: Double?, goals: Goals): Double {
    if (previousValue != null && previousValue > 0) return niceRound(metric, previousValue)
    return when (metric) {
        PeriodMetric.WaterTotal -> niceRound(metric, goals.water.daily.toDouble() * days)
        PeriodMetric.KcalTotal -> niceRound(metric, goals.food.daily.toDouble() * days)
        PeriodMetric.ExerciseKcalTotal -> niceRound(
            metric,
            (if (goals.exerciseUnit == ExerciseUnit.Kcal) goals.exercise.daily else ExerciseUnit.Kcal.defaultDaily).toDouble() * days
        )
        PeriodMetric.ExerciseMinutesTotal -> niceRound(
            metric,
            (if (goals.exerciseUnit == ExerciseUnit.Minutes) goals.exercise.daily else ExerciseUnit.Minutes.defaultDaily).toDouble() * days
        )
        // Dias e treinos: cerca de 70% dos dias, sem passar de todos
        PeriodMetric.WaterDays, PeriodMetric.KcalDays, PeriodMetric.ExerciseDays, PeriodMetric.Workouts ->
            (days * 0.7).roundToInt().coerceIn(1, days).toDouble()
        PeriodMetric.Weight -> 0.0
    }
}

/** Arredonda para um número "redondo" da unidade: 1 L, 100 kcal, 10 min. */
private fun niceRound(metric: PeriodMetric, value: Double): Double {
    val step = when (metric) {
        PeriodMetric.WaterTotal -> 1000.0
        PeriodMetric.KcalTotal, PeriodMetric.ExerciseKcalTotal -> 100.0
        PeriodMetric.ExerciseMinutesTotal -> 10.0
        else -> 1.0
    }
    return ((value / step).roundToInt() * step).coerceAtLeast(step)
}

/**
 * Fecha a meta cujo período acabou antes de [today] (SPEC §9.2): vai para o histórico como
 * concluída ou não, com o valor final. Recorrente, devolve também a próxima, no período
 * que contém hoje, com o mesmo valor.
 */
fun closePeriod(goal: PeriodGoal, finalValue: Double, today: LocalDate, weekStart: DayOfWeek): Pair<PeriodGoal, PeriodGoal?> {
    val reached = finalValue >= goal.target
    val closed = goal.copy(
        status = if (reached) PeriodGoalStatus.Completed else PeriodGoalStatus.Missed,
        finalValue = finalValue
    )
    val next = if (goal.recurring && goal.kind != PeriodKind.Custom) {
        val range = periodContaining(goal.kind, today, weekStart)
        goal.copy(
            id = "",
            start = range.start,
            end = range.endInclusive,
            status = PeriodGoalStatus.Active,
            completedAt = null,
            celebrated = false,
            finalValue = null
        )
    } else {
        null
    }
    return closed to next
}

/**
 * Meta de peso como meta de período (SPEC §9.2): o peso alvo com prazo. [PeriodProgress]
 * em kg percorridos: [PeriodProgress.value] desde o início, [PeriodProgress.target] o
 * caminho todo e [PeriodProgress.projection] o percorrido até o prazo pelo ritmo semanal.
 */
fun weightPeriodProgress(goal: WeightGoal, currentKg: Double, ratePerWeek: Double?, today: LocalDate): PeriodProgress? {
    val deadline = goal.deadline ?: return null
    val since = goal.since ?: today
    val total = abs(goal.targetKg - goal.startKg)
    val done = total - remainingKg(goal, currentKg).coerceAtMost(total)
    val totalDays = daysIn(since..maxOf(since, deadline))
    val elapsed = when {
        today.isAfter(deadline) -> totalDays
        today.isBefore(since) -> 0
        else -> daysIn(since..today)
    }
    val losing = goal.targetKg < goal.startKg
    val projection = ratePerWeek?.let { rate ->
        val daysLeft = ChronoUnit.DAYS.between(today, deadline).coerceAtLeast(0)
        // Ritmo na direção do alvo conta a favor; na contrária, contra
        val towards = if (losing) -rate else rate
        (done + towards * daysLeft / 7).coerceAtLeast(0.0)
    }
    return PeriodProgress(
        value = done,
        target = total,
        fraction = goalProgress(goal, currentKg),
        expected = elapsed.toFloat() / totalDays,
        daysLeft = (totalDays - elapsed).coerceAtLeast(0),
        projection = projection
    )
}

package com.projeto.wellio.domain.goals

import com.projeto.wellio.domain.model.Goals
import java.time.Instant
import java.time.LocalDate

/** O que uma meta de período mede (SPEC §9.2): total no período ou dias batendo a meta diária. */
enum class PeriodMetric {
    WaterTotal,
    WaterDays,
    KcalTotal,
    /** Dias dentro da meta calórica (±10%). */
    KcalDays,
    ExerciseKcalTotal,
    ExerciseMinutesTotal,
    ExerciseDays,
    Workouts,
    Weight;

    /** Métricas que contam dias, não somam valores. */
    val countsDays: Boolean get() = this == WaterDays || this == KcalDays || this == ExerciseDays
}

/** Período da meta; [Custom] são datas escolhidas, sem recorrência. */
enum class PeriodKind { Week, Month, Year, Custom }

enum class PeriodGoalStatus { Active, Completed, Missed }

/**
 * Meta de período (SPEC §9.2). [target] na unidade da métrica (ml, kcal, min, dias, treinos
 * ou kg). [celebrated]: a comemoração da conclusão já foi mostrada.
 */
data class PeriodGoal(
    val id: String,
    val title: String,
    val metric: PeriodMetric,
    val target: Double,
    val kind: PeriodKind,
    val start: LocalDate,
    val end: LocalDate,
    val recurring: Boolean = false,
    val status: PeriodGoalStatus = PeriodGoalStatus.Active,
    val completedAt: Instant? = null,
    val celebrated: Boolean = false,
    /** Valor alcançado quando o período terminou (histórico). */
    val finalValue: Double? = null
)

/** Metas diárias que valem a partir de [effectiveFrom] (SPEC §9.2). */
data class DailyGoalsSnapshot(val effectiveFrom: LocalDate, val goals: Goals)

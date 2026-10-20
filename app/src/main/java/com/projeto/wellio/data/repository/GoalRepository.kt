package com.projeto.wellio.data.repository

import com.projeto.wellio.data.db.DailyGoalHistoryEntity
import com.projeto.wellio.data.db.GoalDao
import com.projeto.wellio.data.db.PeriodGoalEntity
import com.projeto.wellio.domain.goals.DailyGoalsSnapshot
import com.projeto.wellio.domain.goals.PeriodGoal
import com.projeto.wellio.domain.goals.PeriodGoalStatus
import com.projeto.wellio.domain.goals.PeriodKind
import com.projeto.wellio.domain.goals.PeriodMetric
import com.projeto.wellio.domain.model.ExerciseUnit
import com.projeto.wellio.domain.model.Goal
import com.projeto.wellio.domain.model.Goals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Histórico das metas diárias e metas de período (SPEC §9.2). */
class GoalRepository(private val dao: GoalDao, private val clock: Clock) {

    /** Do mais antigo para o mais recente. */
    val history: Flow<List<DailyGoalsSnapshot>> = dao.history().map { rows -> rows.map { it.toDomain() } }

    /** As metas diárias mudaram: valem a partir de hoje (várias mudanças no mesmo dia ficam numa linha só). */
    suspend fun recordDaily(goals: Goals) = dao.upsertHistory(goals.toEntity(LocalDate.now(clock)))

    val periodGoals: Flow<List<PeriodGoal>> = dao.periodGoals().map { rows -> rows.map { it.toDomain() } }

    suspend fun create(goal: PeriodGoal): String {
        val id = goal.id.ifEmpty { UUID.randomUUID().toString() }
        val now = Instant.now(clock)
        dao.insert(goal.copy(id = id).toEntity(now, now))
        return id
    }

    suspend fun update(goal: PeriodGoal) {
        val current = dao.periodGoal(goal.id) ?: return
        dao.update(goal.toEntity(current.createdAt, Instant.now(clock)))
    }

    suspend fun delete(id: String) = dao.softDelete(id, Instant.now(clock))

    suspend fun restore(id: String) = dao.restore(id, Instant.now(clock))
}

private fun DailyGoalHistoryEntity.toDomain() = DailyGoalsSnapshot(
    effectiveFrom,
    Goals(
        water = Goal(waterMl, waterWeekly),
        food = Goal(kcal, kcalWeekly),
        exercise = Goal(exercise, exerciseWeekly),
        exerciseUnit = ExerciseUnit.entries.firstOrNull { it.name == exerciseUnit } ?: ExerciseUnit.Kcal
    )
)

private fun Goals.toEntity(day: LocalDate) = DailyGoalHistoryEntity(
    effectiveFrom = day,
    waterMl = water.daily,
    waterWeekly = water.weeklyOverride,
    kcal = food.daily,
    kcalWeekly = food.weeklyOverride,
    exerciseUnit = exerciseUnit.name,
    exercise = exercise.daily,
    exerciseWeekly = exercise.weeklyOverride
)

private fun PeriodGoalEntity.toDomain() = PeriodGoal(
    id = id,
    title = title,
    metric = PeriodMetric.valueOf(metric),
    target = target,
    kind = PeriodKind.valueOf(periodKind),
    start = startDate,
    end = endDate,
    recurring = recurring,
    status = PeriodGoalStatus.valueOf(status),
    completedAt = completedAt,
    celebrated = celebrated,
    finalValue = finalValue
)

private fun PeriodGoal.toEntity(createdAt: Instant, updatedAt: Instant) = PeriodGoalEntity(
    id = id,
    title = title,
    metric = metric.name,
    target = target,
    periodKind = kind.name,
    startDate = start,
    endDate = end,
    recurring = recurring,
    status = status.name,
    completedAt = completedAt,
    celebrated = celebrated,
    finalValue = finalValue,
    createdAt = createdAt,
    updatedAt = updatedAt
)

package com.projeto.wellio.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.projeto.wellio.domain.model.ExerciseUnit
import com.projeto.wellio.domain.model.Goal
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.domain.model.Goals
import com.projeto.wellio.domain.weight.WeightGoal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Metas diárias e semanais, início da semana e peso alvo, guardados no DataStore. Toda
 * mudança das metas diárias passa por [onDailyGoalsChanged], que grava o histórico (SPEC §9.2).
 */
class GoalsStore(
    private val dataStore: DataStore<Preferences>,
    private val onDailyGoalsChanged: suspend (Goals) -> Unit = {}
) {

    val goals: Flow<Goals> = dataStore.data.map { prefs ->
        val unit = ExerciseUnit.entries.firstOrNull { it.name == prefs[exerciseUnitKey] } ?: ExerciseUnit.Kcal
        GoalType.entries.fold(Goals.Default.copy(exerciseUnit = unit)) { goals, type ->
            val default = Goals.Default[type]
            goals.with(
                type,
                Goal(
                    daily = prefs[dailyKey(type)] ?: default.daily,
                    weeklyOverride = prefs[weeklyKey(type)]
                )
            )
        }
    }

    suspend fun setGoal(type: GoalType, goal: Goal) {
        dataStore.edit { prefs ->
            prefs[dailyKey(type)] = goal.daily
            val weekly = goal.weeklyOverride
            if (weekly == null) prefs.remove(weeklyKey(type)) else prefs[weeklyKey(type)] = weekly
        }
        onDailyGoalsChanged(goals.first())
    }

    /** Meta de exercício em kcal ou minutos (SPEC §8.0); trocar de unidade volta a meta ao padrão dela. */
    suspend fun setExerciseUnit(unit: ExerciseUnit) {
        dataStore.edit { prefs ->
            val current = ExerciseUnit.entries.firstOrNull { it.name == prefs[exerciseUnitKey] } ?: ExerciseUnit.Kcal
            if (current == unit) return@edit
            prefs[exerciseUnitKey] = unit.name
            prefs[dailyKey(GoalType.EXERCISE)] = unit.defaultDaily
            prefs.remove(weeklyKey(GoalType.EXERCISE))
        }
        onDailyGoalsChanged(goals.first())
    }

    /** Dia em que a semana começa (SPEC §9.2); padrão segunda. */
    val weekStart: Flow<DayOfWeek> = dataStore.data.map { prefs ->
        DayOfWeek.entries.firstOrNull { it.name == prefs[weekStartKey] } ?: DayOfWeek.MONDAY
    }

    suspend fun setWeekStart(day: DayOfWeek) {
        dataStore.edit { it[weekStartKey] = day.name }
    }

    private val weekStartKey = stringPreferencesKey("goal_week_start")

    /** Alguma meta já foi salva pelo usuário (quem já usa o app, SPEC §5.0). */
    suspend fun hasSavedGoals(): Boolean {
        val prefs = dataStore.data.first()
        return prefs.contains(exerciseUnitKey) || GoalType.entries.any { prefs.contains(dailyKey(it)) } ||
            prefs.contains(weightTargetKey)
    }

    private val exerciseUnitKey = stringPreferencesKey("goal_exercise_unit")

    /** Peso alvo (SPEC §6.1), com prazo quando for meta de período (§9.2); null enquanto não houver. */
    val weightGoal: Flow<WeightGoal?> = dataStore.data.map { prefs ->
        val target = prefs[weightTargetKey] ?: return@map null
        WeightGoal(
            targetKg = target,
            startKg = prefs[weightStartKey] ?: target,
            deadline = prefs[weightDeadlineKey]?.let(LocalDate::parse)
        )
    }

    /** Define o alvo inteiro (com início e prazo); null remove o alvo e a comemoração. */
    suspend fun setWeightGoal(goal: WeightGoal?) {
        dataStore.edit { prefs ->
            if (goal == null) {
                prefs.remove(weightTargetKey)
                prefs.remove(weightStartKey)
                prefs.remove(weightDeadlineKey)
                prefs.remove(weightCelebratedKey)
            } else {
                prefs[weightTargetKey] = goal.targetKg
                prefs[weightStartKey] = goal.startKg
                if (goal.deadline == null) prefs.remove(weightDeadlineKey) else prefs[weightDeadlineKey] = goal.deadline.toString()
            }
        }
    }

    /**
     * Muda só o valor do alvo: o início continua (ou começa em [currentKg], num alvo novo)
     * e o prazo, se houver, fica. É o que as telas de Peso e Perfil e o onboarding usam.
     */
    suspend fun setWeightTarget(targetKg: Double, currentKg: Double) {
        dataStore.edit { prefs ->
            if (prefs[weightTargetKey] == null) prefs[weightStartKey] = currentKg
            prefs[weightTargetKey] = targetKg
            prefs.remove(weightCelebratedKey)
        }
    }

    /** A conclusão da meta de peso já foi comemorada (SPEC §9.2). */
    val weightCelebrated: Flow<Boolean> = dataStore.data.map { it[weightCelebratedKey] ?: false }

    suspend fun setWeightCelebrated() {
        dataStore.edit { it[weightCelebratedKey] = true }
    }

    private val weightTargetKey = doublePreferencesKey("goal_weight_target_kg")
    private val weightStartKey = doublePreferencesKey("goal_weight_start_kg")
    private val weightDeadlineKey = stringPreferencesKey("goal_weight_deadline")
    private val weightCelebratedKey = booleanPreferencesKey("goal_weight_celebrated")

    private fun dailyKey(type: GoalType) = intPreferencesKey("goal_${type.name.lowercase()}_daily")
    private fun weeklyKey(type: GoalType) = intPreferencesKey("goal_${type.name.lowercase()}_weekly")
}

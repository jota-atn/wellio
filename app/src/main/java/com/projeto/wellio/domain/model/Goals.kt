package com.projeto.wellio.domain.model

/** Meta diária com a semanal derivada (diária × 7), a menos que o usuário defina outra. */
data class Goal(
    val daily: Int,
    val weeklyOverride: Int? = null
) {
    val weekly: Int get() = weeklyOverride ?: daily * 7
}

/** Unidade da meta de exercício, à escolha do usuário (SPEC §8.0). */
enum class ExerciseUnit(val label: String, val defaultDaily: Int) {
    Kcal("kcal", 500),
    Minutes("min", 30)
}

data class Goals(
    val water: Goal,
    val food: Goal,
    val exercise: Goal,
    val exerciseUnit: ExerciseUnit = ExerciseUnit.Kcal
) {
    operator fun get(type: GoalType): Goal = when (type) {
        GoalType.WATER -> water
        GoalType.FOOD -> food
        GoalType.EXERCISE -> exercise
    }

    /** Troca a unidade da meta de exercício; trocando de verdade, a meta vai para o padrão da unidade nova. */
    fun withExerciseUnit(unit: ExerciseUnit): Goals =
        if (unit == exerciseUnit) this else copy(exerciseUnit = unit, exercise = Goal(daily = unit.defaultDaily))

    fun with(type: GoalType, goal: Goal): Goals = when (type) {
        GoalType.WATER -> copy(water = goal)
        GoalType.FOOD -> copy(food = goal)
        GoalType.EXERCISE -> copy(exercise = goal)
    }

    companion object {
        val Default = Goals(
            water = Goal(daily = 2000),
            food = Goal(daily = 2000),
            exercise = Goal(daily = 500)
        )
    }
}

/** Totais de um período (um dia ou uma semana). */
data class Totals(
    val waterMl: Int = 0,
    val kcalConsumed: Int = 0,
    val kcalBurned: Int = 0,
    val exerciseMinutes: Int = 0
) {
    /** Exercício conta em kcal queimadas. Para a unidade da meta, use [exercise]. */
    operator fun get(type: GoalType): Int = when (type) {
        GoalType.WATER -> waterMl
        GoalType.FOOD -> kcalConsumed
        GoalType.EXERCISE -> kcalBurned
    }

    /** Exercício na unidade da meta. */
    fun exercise(unit: ExerciseUnit): Int = if (unit == ExerciseUnit.Minutes) exerciseMinutes else kcalBurned

    /** Total de [type] na unidade da meta (exercício em kcal ou minutos). */
    fun toward(type: GoalType, goals: Goals): Int = if (type == GoalType.EXERCISE) exercise(goals.exerciseUnit) else get(type)
}

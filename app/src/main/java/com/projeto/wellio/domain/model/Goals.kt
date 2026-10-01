package com.projeto.wellio.domain.model

/** Meta diária com a semanal derivada (diária × 7), a menos que o usuário defina outra. */
data class Goal(
    val daily: Int,
    val weeklyOverride: Int? = null
) {
    val weekly: Int get() = weeklyOverride ?: daily * 7
}

data class Goals(
    val water: Goal,
    val food: Goal,
    val exercise: Goal
) {
    operator fun get(type: GoalType): Goal = when (type) {
        GoalType.WATER -> water
        GoalType.FOOD -> food
        GoalType.EXERCISE -> exercise
    }

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
    val kcalBurned: Int = 0
) {
    operator fun get(type: GoalType): Int = when (type) {
        GoalType.WATER -> waterMl
        GoalType.FOOD -> kcalConsumed
        GoalType.EXERCISE -> kcalBurned
    }
}

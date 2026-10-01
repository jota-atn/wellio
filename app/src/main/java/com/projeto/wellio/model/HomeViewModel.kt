package com.projeto.wellio.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf

class HomeViewModel : ViewModel() {

    var waterCurrentMl by mutableIntStateOf(0)
        private set
    var waterGoalMl = 2000

    private val _exercisesDone = mutableStateListOf<RegisteredExercise>()
    val exercisesDone: List<RegisteredExercise> get() = _exercisesDone

    var userWeightKg = 70.0

    private val _foodLog = mutableStateListOf<EatenFood>()
    val foodLog: List<EatenFood> get() = _foodLog

    val totalCaloriesConsumed: Int
        get() = _foodLog.sumOf { it.totalCalories }

    private val _goals = mutableStateMapOf<GoalType, Pair<Int, Int>>()
    val goals: Map<GoalType, Pair<Int, Int>> get() = _goals

    init {
        _goals[GoalType.WATER] = 2000 to 14000
        _goals[GoalType.EXERCISE] = 500 to 3500 // Kcal
        _goals[GoalType.FOOD] = 2000 to 14000 // Kcal
    }

    fun addWater(amount: Int) {
        waterCurrentMl = (waterCurrentMl + amount).coerceIn(0, waterGoalMl)
    }

    fun addFood(blueprint: FoodBlueprint, quantity: Double) {
        // Cálculo simples: (CaloriasBase * Quantidade)
        // Se a base for "100g" e a pessoa colocar "1" (significando 1 porção de 100g), multiplica direto.
        val totalKcal = (blueprint.kcal * quantity).toInt()

        _foodLog.add(EatenFood(blueprint, quantity, totalKcal))
    }

    fun updateGoal(type: GoalType, daily: Int, weekly: Int) {
        _goals[type] = daily to weekly

        if (type == GoalType.WATER) {
            waterGoalMl = daily
        }
    }

    fun getCurrentProgress(type: GoalType): Int {
        return when(type) {
            GoalType.WATER -> waterCurrentMl
            GoalType.EXERCISE -> getTotalCaloriesBurned
            GoalType.FOOD -> totalCaloriesConsumed
        }
    }

    val getTotalCaloriesBurned: Int
        get() = _exercisesDone.sumOf { it.caloriesBurned }

    fun addExercise(blueprint: ExerciseBlueprint, minutes: Int) {
        // FÓRMULA: METs * Peso * (Minutos / 60)
        val calories = (blueprint.mets * userWeightKg * (minutes / 60.0)).toInt()

        val newExercise = RegisteredExercise(blueprint, minutes, calories)
        _exercisesDone.add(newExercise)
    }
}

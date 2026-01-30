package com.projeto.wellio.model

import android.annotation.SuppressLint
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlin.random.Random
import com.projeto.wellio.model.GoalType

class HomeViewModel : ViewModel() {

    var waterCurrentMl by mutableIntStateOf(0)
        private set
    var waterGoalMl = 2000

    var bedTime by mutableStateOf("22:00")
        private set
    var wakeTime by mutableStateOf("06:00")
        private set

    var selectedMood by mutableStateOf<String?>(null)
        private set

    val sleepDuration: String
        get() = calculateDuration(bedTime, wakeTime)

    var isStudying by mutableStateOf(false)
        private set

    var currentSessionSeconds by mutableStateOf(0L)
        private set

    var totalStudySeconds by mutableStateOf(0L)
        private set

    private var timerJob: Job? = null

    private val _exercisesDone = mutableStateListOf<RegisteredExercise>()
    val exercisesDone: List<RegisteredExercise> get() = _exercisesDone

    var userWeightKg = 70.0

    private val _foodLog = mutableStateListOf<EatenFood>()
    val foodLog: List<EatenFood> get() = _foodLog

    val totalCaloriesConsumed: Int
        get() = _foodLog.sumOf { it.totalCalories }

    private val _tasks = mutableStateListOf<Task>(
        Task(1, "Ler 10 páginas"),
        Task(2, "Meditar 5 min")
    )
    val tasks: List<Task> get() = _tasks

    private val _goals = mutableStateMapOf<GoalType, Pair<Int, Int>>()
    val goals: Map<GoalType, Pair<Int, Int>> get() = _goals

    init {
        _goals[GoalType.WATER] = 2000 to 14000
        _goals[GoalType.SLEEP] = 8 to 56 // Horas
        _goals[GoalType.EXERCISE] = 500 to 3500 // Kcal
        _goals[GoalType.FOOD] = 2000 to 14000 // Kcal
        _goals[GoalType.STUDY] = 4 to 28 // Horas
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

    fun updateBedTime(newTime: String) {
        bedTime = newTime
    }

    fun updateWakeTime(newTime: String) {
        wakeTime = newTime
    }

    fun updateGoal(type: GoalType, daily: Int, weekly: Int) {
        _goals[type] = daily to weekly

        if (type == GoalType.WATER) {
            waterGoalMl = daily
        }
    }

    fun selectMood(emoji: String) {
        selectedMood = emoji
    }

    fun startStudySession() {
        if (isStudying) return
        isStudying = true
        currentSessionSeconds = 0

        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                currentSessionSeconds++
            }
        }
    }

    fun stopStudySession() {
        isStudying = false
        totalStudySeconds += currentSessionSeconds
        currentSessionSeconds = 0
        timerJob?.cancel()
    }

    @SuppressLint("DefaultLocale")
    fun getFormattedSessionTime(): String {
        val hours = currentSessionSeconds / 3600
        val minutes = (currentSessionSeconds % 3600) / 60
        val seconds = currentSessionSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun getTotalStudyHours(): String {
        val hours = totalStudySeconds / 3600
        val minutes = (totalStudySeconds % 3600) / 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "0h"
        }
    }

    fun getCurrentProgress(type: GoalType): Int {
        return when(type) {
            GoalType.WATER -> waterCurrentMl
            GoalType.SLEEP -> 7
            GoalType.EXERCISE -> getTotalCaloriesBurned
            GoalType.FOOD -> totalCaloriesConsumed
            GoalType.STUDY -> (totalStudySeconds / 3600).toInt()
        }
    }

    val getTotalCaloriesBurned: Int
        get() = _exercisesDone.sumOf { it.caloriesBurned }

    fun addTask(title: String) {
        if (title.isBlank()) return
        val newTask = Task(id = Random.nextInt(), title = title, isDone = false)
        _tasks.add(newTask)
    }

    fun addExercise(blueprint: ExerciseBlueprint, minutes: Int) {
        // FÓRMULA: METs * Peso * (Minutos / 60)
        val calories = (blueprint.mets * userWeightKg * (minutes / 60.0)).toInt()

        val newExercise = RegisteredExercise(blueprint, minutes, calories)
        _exercisesDone.add(newExercise)
    }

    fun toggleTask(task: Task) {
        val index = _tasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            _tasks[index] = _tasks[index].copy(isDone = !task.isDone)
        }
    }

    fun deleteTask(task: Task) {
        _tasks.remove(task)
    }

    private fun calculateDuration(start: String, end: String): String {
        return try {
            val startParts = start.split(":")
            val endParts = end.split(":")

            val startHour = startParts[0].toInt()
            val startMin = startParts[1].toInt()

            val endHour = endParts[0].toInt()
            val endMin = endParts[1].toInt()

            val startTotalMinutes = (startHour * 60) + startMin
            val endTotalMinutes = (endHour * 60) + endMin

            var diff = endTotalMinutes - startTotalMinutes

            if (diff < 0) {
                diff += 1440
            }

            val hours = diff / 60
            "${hours}h"
        } catch (e: Exception) {
            "--"
        }
    }
}
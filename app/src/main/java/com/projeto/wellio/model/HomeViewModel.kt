package com.projeto.wellio.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateListOf
import kotlin.random.Random

class HomeViewModel : ViewModel() {

    var waterCurrentMl by mutableIntStateOf(0)
        private set
    var waterGoalMl = 2000

    fun addWater(amount: Int) {
        waterCurrentMl = (waterCurrentMl + amount).coerceIn(0, waterGoalMl)
    }

    var bedTime by mutableStateOf("22:00")
        private set
    var wakeTime by mutableStateOf("06:00")
        private set

    var selectedMood by mutableStateOf<String?>(null)
        private set

    val sleepDuration: String
        get() = calculateDuration(bedTime, wakeTime)

    fun updateBedTime(newTime: String) {
        bedTime = newTime
    }

    fun updateWakeTime(newTime: String) {
        wakeTime = newTime
    }

    fun selectMood(emoji: String) {
        selectedMood = emoji
    }

    private val _tasks = mutableStateListOf<Task>(
        Task(1, "Ler 10 páginas"),
        Task(2, "Meditar 5 min")
    )
    val tasks: List<Task> get() = _tasks

    fun addTask(title: String) {
        if (title.isBlank()) return
        val newTask = Task(id = Random.nextInt(), title = title, isDone = false)
        _tasks.add(newTask)
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
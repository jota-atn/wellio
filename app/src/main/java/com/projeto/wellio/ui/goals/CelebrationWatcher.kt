package com.projeto.wellio.ui.goals

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.projeto.wellio.AppContainer
import com.projeto.wellio.WellioApp
import com.projeto.wellio.data.time.todayFlow
import com.projeto.wellio.domain.goals.GoalReading
import com.projeto.wellio.domain.goals.PeriodGoalStatus
import com.projeto.wellio.domain.goals.justReached
import com.projeto.wellio.domain.goals.goalsOn
import com.projeto.wellio.domain.goals.periodValue
import com.projeto.wellio.domain.goals.rings
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.domain.model.Totals
import com.projeto.wellio.domain.weight.latestWeight
import com.projeto.wellio.domain.weight.remainingKg
import com.projeto.wellio.ui.theme.DarkWellioColors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import kotlin.math.abs

/** Uma comemoração para mostrar: o aviso e a cor dele. */
data class CelebrationEvent(val message: String, val color: Color)

private fun goalColor(type: GoalType) = when (type) {
    GoalType.WATER -> DarkWellioColors.water.accent
    GoalType.FOOD -> DarkWellioColors.food.accent
    GoalType.EXERCISE -> DarkWellioColors.exercise.accent
}

private fun goalName(type: GoalType) = when (type) {
    GoalType.WATER -> "água"
    GoalType.FOOD -> "calorias"
    GoalType.EXERCISE -> "exercício"
}

/** "Parabéns! Meta de água concluída", várias juntas, ou os três anéis (SPEC §9.2). */
fun dailyCelebrationMessage(claimed: List<GoalType>, allClosed: Boolean): String = when {
    allClosed && claimed.isNotEmpty() -> "Parabéns! Três anéis fechados. Lenda!"
    claimed.size >= 2 -> "Parabéns! Metas de ${claimed.dropLast(1).joinToString(", ") { goalName(it) }} e ${goalName(claimed.last())} concluídas"
    else -> "Parabéns! Meta de ${goalName(claimed.first())} concluída"
}

/**
 * Vigia de metas (SPEC §9.2): de onde a pessoa estiver no app, toda vez que um registro faz
 * uma meta diária, uma de período ou a de peso passar de não cumprida para cumprida, avisa
 * para a comemoração aparecer por cima de tudo (se estiver ligada nas Configurações).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CelebrationWatcher(private val container: AppContainer) : ViewModel() {

    private val goalsStore = container.goalsStore
    private val repository = container.goalRepository
    private val channel = Channel<CelebrationEvent>(Channel.BUFFERED)
    val events: Flow<CelebrationEvent> = channel.receiveAsFlow()

    init {
        viewModelScope.launch { watchDaily() }
        viewModelScope.launch { watchPeriods() }
        viewModelScope.launch { watchWeight() }
    }

    private suspend fun celebrate(event: CelebrationEvent) {
        if (container.settingsStore.celebrationsEnabled.first()) channel.send(event)
    }

    private suspend fun watchDaily() {
        // Última leitura de cada meta no dia; a primeira de cada dia só serve de base
        var previous: Pair<LocalDate, Map<GoalType, GoalReading>>? = null
        todayFlow(container.clock).flatMapLatest { day ->
            combine(
                container.waterRepository.totalMl(day..day),
                container.foodRepository.kcal(day..day),
                container.exerciseRepository.kcal(day..day),
                container.exerciseRepository.minutes(day..day),
                goalsStore.goals
            ) { water, food, burned, minutes, goals ->
                val totals = Totals(water, food, burned, minutes)
                Triple(day, GoalType.entries.associateWith { GoalReading(totals.toward(it, goals).toDouble(), goals[it].daily.toDouble()) }, rings(totals, goals))
            }
        }.collect { (day, readings, rings) ->
            val before = previous?.takeIf { it.first == day }?.second
            previous = day to readings
            if (before == null) return@collect
            val reached = listOf(GoalType.WATER, GoalType.FOOD, GoalType.EXERCISE).filter { justReached(before[it], readings.getValue(it)) }
            if (reached.isNotEmpty()) celebrate(CelebrationEvent(dailyCelebrationMessage(reached, rings.allClosed), goalColor(reached.first())))
        }
    }

    private suspend fun watchPeriods() {
        val previous = mutableMapOf<String, GoalReading>()
        combine(todayFlow(container.clock), repository.periodGoals, ::Pair).flatMapLatest { (today, goals) ->
            val active = goals.filter { it.finalValue == null && it.status != PeriodGoalStatus.Missed && !it.start.isAfter(today) }
            val from = active.minOfOrNull { it.start } ?: today
            combine(dayLogs(container, from..today), repository.history, goalsStore.goals) { logs, history, current ->
                active.map { goal ->
                    goal to GoalReading(periodValue(goal.metric, goal.start..goal.end, today, logs) { goalsOn(it, history, current) }, goal.target)
                }
            }
        }.collect { readings ->
            readings.forEach { (goal, reading) ->
                val before = previous.put(goal.id, reading)
                if (justReached(before, reading)) {
                    repository.update(goal.copy(status = PeriodGoalStatus.Completed, completedAt = Instant.now(container.clock)))
                    celebrate(CelebrationEvent("Parabéns! Meta concluída: ${goal.title}", DarkWellioColors.base.accent))
                }
            }
        }
    }

    private suspend fun watchWeight() {
        var previous: GoalReading? = null
        combine(goalsStore.weightGoal, container.weightRepository.entries, ::Pair).collect { (goal, entries) ->
            val current = latestWeight(entries)?.weightKg
            if (goal?.deadline == null || current == null) {
                previous = null
                return@collect
            }
            // Em kg percorridos desde o início: cumprida quando não falta nada
            val total = abs(goal.targetKg - goal.startKg)
            val reading = GoalReading(total - remainingKg(goal, current).coerceAtMost(total), total)
            val before = previous
            previous = reading
            if (justReached(before, reading)) celebrate(CelebrationEvent("Parabéns! Meta de peso concluída", DarkWellioColors.base.accent))
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { CelebrationWatcher((this[APPLICATION_KEY] as WellioApp).container) }
        }
    }
}


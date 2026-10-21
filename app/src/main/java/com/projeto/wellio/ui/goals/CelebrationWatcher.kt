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
import com.projeto.wellio.domain.goals.PeriodGoalStatus
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
 * Vigia de metas (SPEC §9.2): de onde a pessoa estiver no app, quando uma meta diária fecha
 * ou uma meta de período (ou a de peso) é concluída, avisa uma vez para a comemoração
 * aparecer por cima de tudo.
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

    private suspend fun watchDaily() {
        todayFlow(container.clock).flatMapLatest { day ->
            combine(
                container.waterRepository.totalMl(day..day),
                container.foodRepository.kcal(day..day),
                container.exerciseRepository.kcal(day..day),
                container.exerciseRepository.minutes(day..day),
                goalsStore.goals
            ) { water, food, burned, minutes, goals -> day to rings(Totals(water, food, burned, minutes), goals) }
        }.collect { (day, rings) ->
            val closed = listOf(GoalType.WATER to rings.water, GoalType.FOOD to rings.food, GoalType.EXERCISE to rings.exercise)
                .filter { it.second >= 1f }
                .map { it.first }
            val claimed = closed.filter { goalsStore.claimDailyCelebration(day, it) }
            if (claimed.isNotEmpty()) channel.send(CelebrationEvent(dailyCelebrationMessage(claimed, rings.allClosed), goalColor(claimed.first())))
        }
    }

    private suspend fun watchPeriods() {
        combine(todayFlow(container.clock), repository.periodGoals, ::Pair).flatMapLatest { (today, goals) ->
            val pending = goals.filter { it.finalValue == null && !it.celebrated && it.status != PeriodGoalStatus.Missed && !it.start.isAfter(today) }
            val from = pending.minOfOrNull { it.start } ?: today
            combine(dayLogs(container, from..today), repository.history, goalsStore.goals) { logs, history, current ->
                pending.filter { goal ->
                    periodValue(goal.metric, goal.start..goal.end, today, logs) { goalsOn(it, history, current) } >= goal.target
                }
            }
        }.collect { reached ->
            reached.forEach { goal ->
                repository.update(goal.copy(celebrated = true, status = PeriodGoalStatus.Completed, completedAt = Instant.now(container.clock)))
                channel.send(CelebrationEvent("Parabéns! Meta concluída: ${goal.title}", DarkWellioColors.base.accent))
            }
        }
    }

    private suspend fun watchWeight() {
        combine(goalsStore.weightGoal, container.weightRepository.entries, goalsStore.weightCelebrated, ::Triple).collect { (goal, entries, celebrated) ->
            val current = latestWeight(entries)?.weightKg ?: return@collect
            if (goal?.deadline != null && !celebrated && remainingKg(goal, current) == 0.0) {
                goalsStore.setWeightCelebrated()
                channel.send(CelebrationEvent("Parabéns! Meta de peso concluída", DarkWellioColors.base.accent))
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { CelebrationWatcher((this[APPLICATION_KEY] as WellioApp).container) }
        }
    }
}


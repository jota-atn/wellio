package com.projeto.wellio.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.projeto.wellio.AppContainer
import com.projeto.wellio.WellioApp
import com.projeto.wellio.data.time.todayFlow
import com.projeto.wellio.domain.goals.DailyGoalsSnapshot
import com.projeto.wellio.domain.goals.DayLog
import com.projeto.wellio.domain.goals.PeriodGoal
import com.projeto.wellio.domain.goals.PeriodGoalStatus
import com.projeto.wellio.domain.goals.PeriodKind
import com.projeto.wellio.domain.goals.PeriodMetric
import com.projeto.wellio.domain.goals.PeriodProgress
import com.projeto.wellio.domain.goals.Rings
import com.projeto.wellio.domain.goals.closePeriod
import com.projeto.wellio.domain.goals.daysIn
import com.projeto.wellio.domain.goals.previousPeriod
import com.projeto.wellio.domain.goals.suggestedTarget
import com.projeto.wellio.domain.goals.goalsOn
import com.projeto.wellio.domain.goals.periodContaining
import com.projeto.wellio.domain.goals.periodProgress
import com.projeto.wellio.domain.goals.periodValue
import com.projeto.wellio.domain.goals.rings
import com.projeto.wellio.domain.goals.weekDays
import com.projeto.wellio.domain.goals.weightPeriodProgress
import com.projeto.wellio.domain.model.ExerciseUnit
import com.projeto.wellio.domain.model.Goal
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.domain.model.Goals
import com.projeto.wellio.domain.model.Totals
import com.projeto.wellio.domain.weight.WeightGoal
import com.projeto.wellio.domain.weight.WeightPeriod
import com.projeto.wellio.domain.weight.latestWeight
import com.projeto.wellio.domain.weight.remainingKg
import com.projeto.wellio.domain.weight.weeklyRate
import com.projeto.wellio.domain.weight.weightTrend
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/** Um dia da semana em anéis; [future] fica vazio. */
data class WeekDay(val date: LocalDate, val rings: Rings, val future: Boolean)

/** Meta de período com o andamento calculado. */
data class PeriodGoalView(val goal: PeriodGoal, val progress: PeriodProgress)

/** O peso alvo com prazo, como meta de período (SPEC §9.2). */
data class WeightPeriodView(val goal: WeightGoal, val currentKg: Double, val progress: PeriodProgress) {
    val reached: Boolean get() = remainingKg(goal, currentKg) == 0.0
}

data class GoalsUiState(
    val today: LocalDate,
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
    val week: List<WeekDay> = emptyList(),
    val goals: Goals = Goals.Default,
    val suggestions: Map<GoalType, Int> = emptyMap(),
    val active: List<PeriodGoalView> = emptyList(),
    val weight: WeightPeriodView? = null,
    /** Metas terminadas, da mais recente para a mais antiga. */
    val finished: List<PeriodGoal> = emptyList(),
    /** Peso mais recente, para criar a meta de peso. */
    val currentKg: Double? = null
) {
    val closedDays: Int get() = week.count { !it.future && it.rings.allClosed }
}

/** O que foi registrado em cada dia de um intervalo. */
fun dayLogs(container: AppContainer, range: ClosedRange<LocalDate>): Flow<Map<LocalDate, DayLog>> = combine(
    container.waterRepository.totalsByDay(range),
    container.foodRepository.kcalByDay(range),
    container.exerciseRepository.kcalByDay(range),
    container.exerciseRepository.minutesByDay(range),
    container.exerciseRepository.workoutsByDay(range)
) { water, food, burned, minutes, workouts ->
    (water.keys + food.keys + burned.keys + minutes.keys + workouts.keys).associateWith { day ->
        DayLog(Totals(water[day] ?: 0, food[day] ?: 0, burned[day] ?: 0, minutes[day] ?: 0), workouts[day] ?: 0)
    }
}

/** Tela de Metas (SPEC §9.2): semana em anéis, metas diárias, início da semana e metas de período. */
@OptIn(ExperimentalCoroutinesApi::class)
class GoalsViewModel(private val container: AppContainer) : ViewModel() {

    private val goalsStore = container.goalsStore
    private val repository = container.goalRepository
    private val initialToday = LocalDate.now(container.clock)

    init {
        viewModelScope.launch { rollOver() }
    }

    private val base = combine(
        todayFlow(container.clock),
        goalsStore.weekStart,
        goalsStore.goals,
        repository.history,
        repository.periodGoals
    ) { today, weekStart, goals, history, periodGoals -> Base(today, weekStart, goals, history, periodGoals) }

    private data class Base(
        val today: LocalDate,
        val weekStart: DayOfWeek,
        val goals: Goals,
        val history: List<DailyGoalsSnapshot>,
        val periodGoals: List<PeriodGoal>
    )

    val uiState: StateFlow<GoalsUiState> = base.flatMapLatest { b ->
        val week = weekDays(b.today, b.weekStart)
        val active = b.periodGoals.filter { it.status != PeriodGoalStatus.Missed && it.finalValue == null }
        val from = (active.map { it.start } + week.first()).min()
        combine(
            dayLogs(container, from..b.today),
            goalSuggestions(container),
            container.weightRepository.entries,
            goalsStore.weightGoal
        ) { logs, suggestions, weights, weightGoal ->
            val goalsFor = { day: LocalDate -> goalsOn(day, b.history, b.goals) }
            val currentKg = latestWeight(weights)?.weightKg
            val weight = if (weightGoal?.deadline != null && currentKg != null) {
                val rate = weeklyRate(weightTrend(weights, b.today, WeightPeriod.Month))
                weightPeriodProgress(weightGoal, currentKg, rate, b.today)?.let {
                    WeightPeriodView(weightGoal, currentKg, it)
                }
            } else {
                null
            }
            GoalsUiState(
                today = b.today,
                weekStart = b.weekStart,
                week = week.map { day ->
                    WeekDay(day, rings(logs[day]?.totals ?: Totals(), goalsFor(day)), future = day.isAfter(b.today))
                },
                goals = b.goals,
                suggestions = suggestions,
                active = active.map { goal ->
                    PeriodGoalView(goal, periodProgress(goal, periodValue(goal.metric, goal.start..goal.end, b.today, logs, goalsFor), b.today))
                },
                weight = weight,
                finished = b.periodGoals.filter { it.finalValue != null }.sortedByDescending { it.end },
                currentKg = currentKg
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState(initialToday))

    /**
     * Fecha as metas cujo período acabou (SPEC §9.2): vão para o histórico e, se recorrentes,
     * começa a próxima. A de peso, ao passar o prazo, entra no histórico e o alvo perde o prazo.
     */
    private suspend fun rollOver() {
        val today = LocalDate.now(container.clock)
        val weekStart = goalsStore.weekStart.first()
        val expired = repository.periodGoals.first().filter { it.finalValue == null && it.end.isBefore(today) }
        if (expired.isNotEmpty()) {
            val history = repository.history.first()
            val goals = goalsStore.goals.first()
            val logs = dayLogs(container, expired.minOf { it.start }..today).first()
            expired.forEach { goal ->
                val value = periodValue(goal.metric, goal.start..goal.end, today, logs) { goalsOn(it, history, goals) }
                val (closed, next) = closePeriod(goal, value, today, weekStart)
                repository.update(closed)
                next?.let { repository.create(it) }
            }
        }
        val weightGoal = goalsStore.weightGoal.first()
        val deadline = weightGoal?.deadline
        if (weightGoal != null && deadline != null && deadline.isBefore(today)) {
            val currentKg = latestWeight(container.weightRepository.entries.first())?.weightKg ?: weightGoal.startKg
            val reached = remainingKg(weightGoal, currentKg) == 0.0
            repository.create(
                PeriodGoal(
                    id = "",
                    title = periodGoalTitle(PeriodMetric.Weight, weightGoal.targetKg, PeriodKind.Custom, weightGoal.since ?: deadline, deadline, false, today),
                    metric = PeriodMetric.Weight,
                    target = weightGoal.targetKg,
                    kind = PeriodKind.Custom,
                    start = weightGoal.since ?: deadline,
                    end = deadline,
                    status = if (reached) PeriodGoalStatus.Completed else PeriodGoalStatus.Missed,
                    finalValue = currentKg
                )
            )
            goalsStore.setWeightGoal(weightGoal.copy(deadline = null, since = null))
        }
    }

    /** Valor sugerido ao criar (SPEC §9.2): o do período anterior igual, ou a meta diária × dias. */
    suspend fun suggestTarget(metric: PeriodMetric, kind: PeriodKind, range: ClosedRange<LocalDate>): Double {
        val today = LocalDate.now(container.clock)
        val previous = previousPeriod(kind, range, goalsStore.weekStart.first())
        val history = repository.history.first()
        val goals = goalsStore.goals.first()
        val logs = dayLogs(container, previous).first()
        val value = periodValue(metric, previous, today, logs) { goalsOn(it, history, goals) }
        return suggestedTarget(metric, daysIn(range), value, goals)
    }

    fun saveDaily(type: GoalType, goal: Goal, unit: ExerciseUnit) {
        viewModelScope.launch { goalsStore.saveDaily(type, goal, unit) }
    }

    fun setWeekStart(day: DayOfWeek) {
        viewModelScope.launch { goalsStore.setWeekStart(day) }
    }

    fun create(goal: PeriodGoal) {
        viewModelScope.launch { repository.create(goal) }
    }

    fun update(goal: PeriodGoal) {
        viewModelScope.launch { repository.update(goal) }
    }

    fun delete(goal: PeriodGoal) {
        viewModelScope.launch { repository.delete(goal.id) }
    }

    fun restore(goal: PeriodGoal) {
        viewModelScope.launch { repository.restore(goal.id) }
    }

    /** Meta de peso: o mesmo alvo da tela de Peso, com prazo; substitui o alvo atual (SPEC §9.2). */
    fun setWeightPeriod(targetKg: Double, deadline: LocalDate) {
        val state = uiState.value
        val currentKg = state.currentKg ?: targetKg
        viewModelScope.launch {
            val existing = goalsStore.weightGoal.first()
            // Mesmo alvo ganhando (ou mudando) prazo: o início continua; alvo novo começa agora
            val same = existing?.takeIf { it.targetKg == targetKg }
            goalsStore.setWeightGoal(WeightGoal(targetKg, same?.startKg ?: currentKg, deadline, since = same?.since ?: state.today))
        }
    }

    /** Tira o prazo: o alvo continua na tela de Peso, sem ser meta de período. */
    fun removeWeightPeriod() {
        viewModelScope.launch {
            goalsStore.weightGoal.first()?.let { goalsStore.setWeightGoal(it.copy(deadline = null, since = null)) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { GoalsViewModel((this[APPLICATION_KEY] as WellioApp).container) }
        }
    }
}

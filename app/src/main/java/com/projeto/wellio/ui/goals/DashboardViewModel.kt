package com.projeto.wellio.ui.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.projeto.wellio.AppContainer
import com.projeto.wellio.WellioApp
import com.projeto.wellio.data.time.todayFlow
import com.projeto.wellio.domain.goals.Rings
import com.projeto.wellio.domain.goals.goalsOn
import com.projeto.wellio.domain.goals.rings
import com.projeto.wellio.domain.model.ExerciseUnit
import com.projeto.wellio.domain.model.Goal
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.domain.model.Goals
import com.projeto.wellio.domain.model.Totals
import com.projeto.wellio.domain.time.canOpen
import com.projeto.wellio.ui.navigation.DashboardDestination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DashboardUiState(
    val day: LocalDate,
    val today: LocalDate,
    val totals: Totals = Totals(),
    /** Metas que valiam no dia escolhido (SPEC §9.2). */
    val goals: Goals = Goals.Default,
    /** Metas de agora, que o sheet edita. */
    val currentGoals: Goals = Goals.Default,
    val suggestions: Map<GoalType, Int> = emptyMap(),
    /** Para o balão de cada anel (SPEC §5.4). */
    val details: DayDetails = DayDetails(),
    /** "% do dia" de ontem, para a comparação (só vendo hoje; null se ontem não teve registro). */
    val yesterdayScore: Float? = null
) {
    val rings: Rings get() = rings(totals, goals)
}

/** O que o balão de cada anel conta além do valor: registros de água e os treinos do dia. */
data class DayDetails(
    val waterEntries: Int = 0,
    val lastWaterAt: Instant? = null,
    val workoutNames: List<String> = emptyList()
)

/** Tela do Dashboard (SPEC §5.4): anéis e metas de um dia, com navegação entre dias. */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(private val container: AppContainer, handle: SavedStateHandle) : ViewModel() {

    private val goalsStore = container.goalsStore
    val zone: ZoneId = container.clock.zone
    private val initialToday = LocalDate.now(container.clock)
    private val selectedDay = MutableStateFlow(
        handle.toRoute<DashboardDestination>().day?.let(LocalDate::parse)?.takeIf { !it.isAfter(initialToday) } ?: initialToday
    )

    val uiState: StateFlow<DashboardUiState> = combine(
        combine(selectedDay, todayFlow(container.clock), ::Pair),
        selectedDay.flatMapLatest { day -> combine(totals(day), details(day), totals(day.minusDays(1)), ::Triple) },
        container.goalRepository.history,
        goalsStore.goals,
        goalSuggestions(container)
    ) { (day, today), (totals, details, yesterday), history, current, suggestions ->
        val yesterdayScore = if (day == today && yesterday != Totals()) {
            rings(yesterday, goalsOn(day.minusDays(1), history, current)).score
        } else {
            null
        }
        DashboardUiState(day, today, totals, goalsOn(day, history, current), current, suggestions, details, yesterdayScore)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState(selectedDay.value, initialToday))

    private fun totals(day: LocalDate): Flow<Totals> = combine(
        container.waterRepository.totalMl(day..day),
        container.foodRepository.kcal(day..day),
        container.exerciseRepository.kcal(day..day),
        container.exerciseRepository.minutes(day..day)
    ) { water, consumed, burned, minutes -> Totals(water, consumed, burned, minutes) }

    private fun details(day: LocalDate): Flow<DayDetails> = combine(
        container.waterRepository.entries(day),
        container.exerciseRepository.entries(day),
        container.exerciseRepository.types
    ) { water, workouts, types ->
        val names = types.associate { it.id to it.name }
        DayDetails(
            waterEntries = water.size,
            lastWaterAt = water.maxOfOrNull { it.timestamp },
            workoutNames = workouts.sortedBy { it.timestamp }.mapNotNull { names[it.typeId] }.distinct()
        )
    }

    fun selectDay(day: LocalDate) {
        if (canOpen(day, uiState.value.today)) selectedDay.value = day
    }

    fun saveGoal(type: GoalType, goal: Goal, unit: ExerciseUnit) {
        viewModelScope.launch { goalsStore.saveDaily(type, goal, unit) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                DashboardViewModel((this[APPLICATION_KEY] as WellioApp).container, createSavedStateHandle())
            }
        }
    }
}

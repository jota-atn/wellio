package com.projeto.wellio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.projeto.wellio.AppContainer
import com.projeto.wellio.WellioApp
import com.projeto.wellio.data.prefs.GoalsStore
import com.projeto.wellio.data.repository.ExerciseRepository
import com.projeto.wellio.data.repository.FoodRepository
import com.projeto.wellio.data.repository.WaterRepository
import com.projeto.wellio.data.time.todayFlow
import com.projeto.wellio.domain.model.Goal
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.domain.model.Goals
import com.projeto.wellio.domain.model.Totals
import com.projeto.wellio.domain.time.weekOf
import com.projeto.wellio.domain.goals.DayScore
import com.projeto.wellio.domain.home.DASHBOARD_KEY
import com.projeto.wellio.domain.home.DefaultHomeLayout
import com.projeto.wellio.domain.home.HomeCard
import com.projeto.wellio.domain.home.decodeLayout
import com.projeto.wellio.domain.home.encodeLayout
import kotlinx.coroutines.flow.map
import com.projeto.wellio.domain.goals.dayScore
import com.projeto.wellio.domain.goals.lastDays
import com.projeto.wellio.ui.module.Modules
import com.projeto.wellio.ui.module.Summary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val goals: Goals = Goals.Default,
    val today: Totals = Totals(),
    val week: Totals = Totals(),
    /** Resumo de hoje de cada módulo, na ordem de [Modules.all]. */
    val summaries: List<Summary> = emptyList(),
    /** % do dia nos últimos 7 dias, do mais antigo para hoje. */
    val lastWeek: List<DayScore> = emptyList(),
    /** Cards da Home, na ordem do usuário. */
    val layout: List<HomeCard> = DefaultHomeLayout,
    /** Mostrar "+ Adicionar card" nas linhas vazias (até ser dispensada). */
    val showSuggestion: Boolean = true
)

/** Cards que a Home sabe desenhar: o Dashboard e o de cada módulo. */
val knownCardKeys: Set<String> = setOf(DASHBOARD_KEY) + Modules.all.map { it.id.key }

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(container: AppContainer) : ViewModel() {

    private val waterRepository: WaterRepository = container.waterRepository
    private val foodRepository: FoodRepository = container.foodRepository
    private val exerciseRepository: ExerciseRepository = container.exerciseRepository
    private val goalsStore: GoalsStore = container.goalsStore
    private val layoutStore = container.homeLayoutStore

    private val layout: Flow<List<HomeCard>> = layoutStore.layout.map { saved ->
        saved?.let { decodeLayout(it, knownCardKeys) } ?: DefaultHomeLayout
    }

    private val today = todayFlow(container.clock)

    private val summaries: Flow<List<Summary>> =
        combine(Modules.all.map { it.summary(container) }) { it.toList() }

    private val lastWeek: Flow<List<DayScore>> = today.flatMapLatest { day ->
        val days = lastDays(day)
        val range = days.first()..day
        combine(
            waterRepository.totalsByDay(range),
            foodRepository.kcalByDay(range),
            exerciseRepository.kcalByDay(range),
            goalsStore.goals
        ) { water, food, exercise, goals ->
            days.map { date ->
                DayScore(date, dayScore(Totals(water[date] ?: 0, food[date] ?: 0, exercise[date] ?: 0), goals))
            }
        }
    }


    val uiState: StateFlow<HomeUiState> = combine(
        goalsStore.goals,
        today.flatMapLatest { day -> totals(day..day) },
        today.flatMapLatest { day -> totals(weekOf(day)) },
        combine(summaries, lastWeek, ::Pair),
        combine(layout, layoutStore.suggestionDismissed, ::Pair)
    ) { goals, todayTotals, weekTotals, (summaries, lastWeek), (layout, dismissed) ->
        HomeUiState(goals, todayTotals, weekTotals, summaries, lastWeek, layout, showSuggestion = !dismissed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun totals(days: ClosedRange<LocalDate>): Flow<Totals> = combine(
        waterRepository.totalMl(days),
        foodRepository.kcal(days),
        exerciseRepository.kcal(days)
    ) { water, consumed, burned -> Totals(water, consumed, burned) }

    fun saveLayout(cards: List<HomeCard>) {
        viewModelScope.launch { layoutStore.save(encodeLayout(cards)) }
    }

    fun dismissSuggestion() {
        viewModelScope.launch { layoutStore.dismissSuggestion() }
    }

    fun updateGoal(type: GoalType, daily: Int, weekly: Int) {
        // Semanal igual a diária × 7 continua automática; qualquer outro valor vira meta própria
        val goal = Goal(daily = daily, weeklyOverride = weekly.takeIf { it != daily * 7 })
        viewModelScope.launch { goalsStore.setGoal(type, goal) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                HomeViewModel((this[APPLICATION_KEY] as WellioApp).container)
            }
        }
    }
}

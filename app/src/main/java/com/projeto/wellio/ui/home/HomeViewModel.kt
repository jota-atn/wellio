package com.projeto.wellio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.projeto.wellio.WellioApp
import com.projeto.wellio.data.prefs.GoalsStore
import com.projeto.wellio.data.repository.ExerciseRepository
import com.projeto.wellio.data.repository.FoodRepository
import com.projeto.wellio.data.repository.WaterRepository
import com.projeto.wellio.data.time.todayFlow
import com.projeto.wellio.domain.model.ExerciseType
import com.projeto.wellio.domain.model.Food
import com.projeto.wellio.domain.model.Goal
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.domain.model.Goals
import com.projeto.wellio.domain.model.Totals
import com.projeto.wellio.domain.model.WaterContainer
import com.projeto.wellio.domain.model.WaterEntry
import com.projeto.wellio.domain.water.lastEntry
import com.projeto.wellio.domain.water.quickButtons
import com.projeto.wellio.domain.time.weekOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class HomeUiState(
    val goals: Goals = Goals.Default,
    val today: Totals = Totals(),
    val week: Totals = Totals(),
    val foods: List<Food> = emptyList(),
    val exerciseTypes: List<ExerciseType> = emptyList(),
    val waterQuickButtons: List<WaterContainer> = emptyList(),
    val lastWaterToday: WaterEntry? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val waterRepository: WaterRepository,
    private val foodRepository: FoodRepository,
    private val exerciseRepository: ExerciseRepository,
    private val goalsStore: GoalsStore,
    clock: Clock
) : ViewModel() {

    private val today = todayFlow(clock)

    private val water = combine(
        waterRepository.containers,
        today.flatMapLatest { waterRepository.entries(it) }
    ) { containers, entries -> quickButtons(containers) to lastEntry(entries) }

    val uiState: StateFlow<HomeUiState> = combine(
        goalsStore.goals,
        today.flatMapLatest { day -> totals(day..day) },
        today.flatMapLatest { day -> totals(weekOf(day)) },
        combine(foodRepository.foods, exerciseRepository.types, ::Pair),
        water
    ) { goals, todayTotals, weekTotals, (foods, types), (quick, last) ->
        HomeUiState(goals, todayTotals, weekTotals, foods, types, quick, last)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun totals(days: ClosedRange<LocalDate>): Flow<Totals> = combine(
        waterRepository.totalMl(days),
        foodRepository.kcal(days),
        exerciseRepository.kcal(days)
    ) { water, consumed, burned -> Totals(water, consumed, burned) }

    fun addWater(container: WaterContainer) {
        viewModelScope.launch { waterRepository.add(container.amountMl, container.name) }
    }

    fun undoLastWater() {
        val last = uiState.value.lastWaterToday ?: return
        viewModelScope.launch { waterRepository.delete(last.id) }
    }

    fun addFood(food: Food, portions: Double) {
        viewModelScope.launch { foodRepository.add(food, portions) }
    }

    fun addExercise(type: ExerciseType, minutes: Int) {
        viewModelScope.launch { exerciseRepository.add(type, minutes) }
    }

    fun updateGoal(type: GoalType, daily: Int, weekly: Int) {
        // Semanal igual a diária × 7 continua automática; qualquer outro valor vira meta própria
        val goal = Goal(daily = daily, weeklyOverride = weekly.takeIf { it != daily * 7 })
        viewModelScope.launch { goalsStore.setGoal(type, goal) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WellioApp).container
                HomeViewModel(
                    container.waterRepository,
                    container.foodRepository,
                    container.exerciseRepository,
                    container.goalsStore,
                    container.clock
                )
            }
        }
    }
}

package com.projeto.wellio.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.projeto.wellio.WellioApp
import com.projeto.wellio.data.prefs.GoalsStore
import com.projeto.wellio.data.repository.ExerciseRepository
import com.projeto.wellio.data.repository.WeightRepository
import com.projeto.wellio.data.time.todayFlow
import com.projeto.wellio.domain.exercise.ExerciseCategory
import com.projeto.wellio.domain.exercise.ExerciseFamily
import com.projeto.wellio.domain.exercise.QuickExercise
import com.projeto.wellio.domain.model.ExerciseUnit
import com.projeto.wellio.domain.model.Goal
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.domain.exercise.Intensity
import com.projeto.wellio.domain.model.ExerciseEntry
import com.projeto.wellio.domain.model.ExerciseType
import com.projeto.wellio.domain.model.ExerciseUse
import com.projeto.wellio.domain.model.Goals
import com.projeto.wellio.domain.time.canOpen
import com.projeto.wellio.domain.time.instantAt
import com.projeto.wellio.domain.weight.DEFAULT_WEIGHT_KG
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** O que o registro devolve: a atividade, quanto tempo, as kcal digitadas (se houver) e o horário. */
data class ExerciseDraft(
    val type: ExerciseType,
    val minutes: Int,
    val manualKcal: Int? = null,
    val time: LocalTime
)

data class ExerciseUiState(
    val day: LocalDate,
    val today: LocalDate,
    val types: List<ExerciseType> = emptyList(),
    val uses: List<ExerciseUse> = emptyList(),
    val entries: List<ExerciseEntry> = emptyList(),
    val goals: Goals = Goals.Default,
    /** Peso atual, para estimar as kcal. */
    val weightKg: Double = DEFAULT_WEIGHT_KG
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseViewModel(
    private val repository: ExerciseRepository,
    private val goalsStore: GoalsStore,
    weightRepository: WeightRepository,
    private val clock: Clock
) : ViewModel() {

    private val initialDay = LocalDate.now(clock)
    private val selectedDay = MutableStateFlow(initialDay)
    val zone: ZoneId get() = clock.zone

    val uiState: StateFlow<ExerciseUiState> = combine(
        combine(selectedDay, todayFlow(clock), ::Pair),
        combine(repository.types, repository.lastUses, ::Pair),
        selectedDay.flatMapLatest { repository.entries(it) },
        goalsStore.goals,
        weightRepository.currentWeightKg
    ) { (day, today), (types, uses), entries, goals, weightKg ->
        ExerciseUiState(day, today, types, uses, entries, goals, weightKg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseUiState(initialDay, initialDay))

    fun selectDay(day: LocalDate) {
        if (canOpen(day, uiState.value.today)) selectedDay.value = day
    }

    /** Registra no dia aberto (hoje, fora da tela de Exercício). */
    fun add(draft: ExerciseDraft) {
        val day = selectedDay.value
        viewModelScope.launch {
            repository.add(draft.type, draft.minutes, draft.manualKcal, day, instantAt(day, draft.time, clock.zone))
        }
    }

    /** Último treino feito pelo toque num favorito, para o "Desfazer". */
    private var lastQuickAddId: String? = null

    /** Toque num favorito: registra na hora, no dia aberto, com o nível e a duração dele. */
    fun quickAdd(quick: QuickExercise) {
        val day = selectedDay.value
        val time = LocalTime.now(clock)
        viewModelScope.launch {
            lastQuickAddId = repository.add(quick.type, quick.minutes, null, day, instantAt(day, time, clock.zone))
        }
    }

    fun undoQuickAdd() {
        val id = lastQuickAddId ?: return
        lastQuickAddId = null
        viewModelScope.launch { repository.delete(id) }
    }

    /** Meta diária de exercício em [unit]; trocar de unidade grava a unidade antes. */
    fun setGoal(unit: ExerciseUnit, daily: Int) {
        viewModelScope.launch {
            goalsStore.setExerciseUnit(unit)
            goalsStore.setGoal(GoalType.EXERCISE, Goal(daily = daily))
        }
    }

    fun update(entry: ExerciseEntry, draft: ExerciseDraft) {
        viewModelScope.launch {
            val kcal = draft.manualKcal ?: repository.estimateKcal(draft.type, draft.minutes)
            repository.update(
                entry.copy(
                    typeId = draft.type.id,
                    minutes = draft.minutes,
                    kcal = kcal,
                    kcalIsManual = draft.manualKcal != null,
                    timestamp = instantAt(entry.date, draft.time, clock.zone)
                )
            )
        }
    }

    fun delete(entry: ExerciseEntry) {
        viewModelScope.launch { repository.delete(entry.id) }
    }

    fun restore(entry: ExerciseEntry) {
        viewModelScope.launch { repository.restore(entry.id) }
    }

    /** Favorito vale para a atividade inteira (todos os níveis). */
    fun toggleFavorite(family: ExerciseFamily) {
        viewModelScope.launch { repository.setFavorite(family.variants.map { it.id }, !family.isFavorite) }
    }

    /** Cria uma atividade própria e devolve ela em [onCreated] (já selecionada no registro). */
    fun createCustom(
        name: String,
        category: ExerciseCategory,
        intensity: Intensity,
        kcalPerMinute: Double?,
        onCreated: (ExerciseType) -> Unit
    ) {
        viewModelScope.launch {
            onCreated(repository.createCustom(name, category.label, category.iconKey, intensity.mets, kcalPerMinute))
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WellioApp).container
                ExerciseViewModel(container.exerciseRepository, container.goalsStore, container.weightRepository, container.clock)
            }
        }
    }
}

package com.projeto.wellio.ui.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.projeto.wellio.WellioApp
import com.projeto.wellio.data.prefs.GoalsStore
import com.projeto.wellio.data.repository.WeightRepository
import com.projeto.wellio.domain.weight.DEFAULT_WEIGHT_KG
import com.projeto.wellio.domain.weight.WeightGoal
import com.projeto.wellio.data.time.todayFlow
import com.projeto.wellio.domain.model.WeightEntry
import com.projeto.wellio.domain.weight.withChanges
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class WeightUiState(
    val today: LocalDate,
    /** Pesagens da mais recente para a mais antiga, com a variação em relação à anterior. */
    val entries: List<Pair<WeightEntry, Double?>> = emptyList(),
    val goal: WeightGoal? = null
) {
    /** Peso que vale agora: a pesagem mais recente, ou o padrão. */
    val currentKg: Double get() = entries.firstOrNull()?.first?.weightKg ?: DEFAULT_WEIGHT_KG
}

class WeightViewModel(
    private val repository: WeightRepository,
    private val goalsStore: GoalsStore,
    clock: Clock
) : ViewModel() {

    private val initialDay = LocalDate.now(clock)

    val uiState: StateFlow<WeightUiState> = combine(
        todayFlow(clock),
        repository.entries,
        goalsStore.weightGoal
    ) { today, entries, goal ->
        WeightUiState(today, withChanges(entries), goal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightUiState(initialDay))

    fun save(day: LocalDate, weightKg: Double) {
        viewModelScope.launch { repository.save(day, weightKg) }
    }

    fun update(entry: WeightEntry, day: LocalDate, weightKg: Double) {
        viewModelScope.launch { repository.update(entry, day, weightKg) }
    }

    /** Define o alvo; um alvo novo começa a barra de progresso no peso atual. */
    fun setTarget(targetKg: Double) {
        val startKg = uiState.value.goal?.startKg ?: uiState.value.currentKg
        viewModelScope.launch { goalsStore.setWeightGoal(WeightGoal(targetKg, startKg)) }
    }

    fun clearTarget() {
        viewModelScope.launch { goalsStore.setWeightGoal(null) }
    }

    fun delete(entry: WeightEntry) {
        viewModelScope.launch { repository.delete(entry.id) }
    }

    fun restore(entry: WeightEntry) {
        viewModelScope.launch { repository.restore(entry.id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WellioApp).container
                WeightViewModel(container.weightRepository, container.goalsStore, container.clock)
            }
        }
    }
}

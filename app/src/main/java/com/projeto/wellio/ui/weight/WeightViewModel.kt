package com.projeto.wellio.ui.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.projeto.wellio.WellioApp
import com.projeto.wellio.data.repository.WeightRepository
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
    val entries: List<Pair<WeightEntry, Double?>> = emptyList()
)

class WeightViewModel(private val repository: WeightRepository, clock: Clock) : ViewModel() {

    private val initialDay = LocalDate.now(clock)

    val uiState: StateFlow<WeightUiState> = combine(todayFlow(clock), repository.entries) { today, entries ->
        WeightUiState(today, withChanges(entries))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightUiState(initialDay))

    fun save(day: LocalDate, weightKg: Double) {
        viewModelScope.launch { repository.save(day, weightKg) }
    }

    fun update(entry: WeightEntry, day: LocalDate, weightKg: Double) {
        viewModelScope.launch { repository.update(entry, day, weightKg) }
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
                WeightViewModel(container.weightRepository, container.clock)
            }
        }
    }
}

package com.projeto.wellio.ui.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.projeto.wellio.WellioApp
import com.projeto.wellio.data.prefs.GoalsStore
import com.projeto.wellio.data.repository.WaterRepository
import com.projeto.wellio.data.time.todayFlow
import com.projeto.wellio.domain.model.WaterContainer
import com.projeto.wellio.domain.model.WaterEntry
import com.projeto.wellio.domain.time.canOpen
import com.projeto.wellio.domain.time.instantAt
import com.projeto.wellio.domain.water.totalMl
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
import java.util.UUID

data class WaterUiState(
    val day: LocalDate,
    val today: LocalDate,
    val goalMl: Int = 0,
    val entries: List<WaterEntry> = emptyList(),
    val containers: List<WaterContainer> = emptyList()
) {
    val totalMl: Int get() = totalMl(entries)
}

@OptIn(ExperimentalCoroutinesApi::class)
class WaterViewModel(
    private val repository: WaterRepository,
    goalsStore: GoalsStore,
    private val clock: Clock
) : ViewModel() {

    private val initialDay = LocalDate.now(clock)
    private val selectedDay = MutableStateFlow(initialDay)
    val zone: ZoneId get() = clock.zone

    val uiState: StateFlow<WaterUiState> = combine(
        selectedDay,
        todayFlow(clock),
        goalsStore.goals,
        selectedDay.flatMapLatest { repository.entries(it) },
        repository.containers
    ) { day, today, goals, entries, containers ->
        WaterUiState(day, today, goals.water.daily, entries, containers)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WaterUiState(initialDay, initialDay))

    fun selectDay(day: LocalDate) {
        if (canOpen(day, uiState.value.today)) selectedDay.value = day
    }

    fun add(amountMl: Int, containerName: String?, time: LocalTime) {
        val day = selectedDay.value
        viewModelScope.launch {
            repository.add(amountMl, containerName, day, instantAt(day, time, clock.zone))
        }
    }

    /** Último registro feito pelo toque num recipiente, para o "Desfazer". */
    private var lastQuickAddId: String? = null

    /** Toque num recipiente: registra na hora, no dia aberto. */
    fun quickAdd(container: WaterContainer) {
        val day = selectedDay.value
        val time = LocalTime.now(clock)
        viewModelScope.launch {
            lastQuickAddId = repository.add(container.amountMl, container.name, day, instantAt(day, time, clock.zone))
        }
    }

    fun undoQuickAdd() {
        val id = lastQuickAddId ?: return
        lastQuickAddId = null
        viewModelScope.launch { repository.delete(id) }
    }

    fun update(entry: WaterEntry, amountMl: Int, containerName: String?, time: LocalTime) {
        viewModelScope.launch {
            repository.update(
                entry.copy(
                    amountMl = amountMl,
                    containerName = containerName,
                    timestamp = instantAt(entry.date, time, clock.zone)
                )
            )
        }
    }

    fun delete(entry: WaterEntry) {
        viewModelScope.launch { repository.delete(entry.id) }
    }

    fun restore(entry: WaterEntry) {
        viewModelScope.launch { repository.restore(entry.id) }
    }

    fun saveContainer(id: String?, name: String, amountMl: Int, isQuick: Boolean, iconKey: String) {
        val containers = uiState.value.containers
        val existing = containers.find { it.id == id }
        val container = existing?.copy(name = name, amountMl = amountMl, isQuick = isQuick, iconKey = iconKey)
            ?: WaterContainer(
                id = UUID.randomUUID().toString(),
                name = name,
                amountMl = amountMl,
                isQuick = isQuick,
                position = (containers.maxOfOrNull { it.position } ?: -1) + 1,
                iconKey = iconKey
            )
        viewModelScope.launch { repository.saveContainers(listOf(container)) }
    }

    fun deleteContainer(container: WaterContainer) {
        viewModelScope.launch { repository.deleteContainer(container.id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WellioApp).container
                WaterViewModel(container.waterRepository, container.goalsStore, container.clock)
            }
        }
    }
}

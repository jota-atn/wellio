package com.projeto.wellio.ui.exercise

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.projeto.wellio.AppContainer
import com.projeto.wellio.WellioApp
import com.projeto.wellio.domain.model.ExerciseType
import com.projeto.wellio.ui.components.ExerciseDialog
import com.projeto.wellio.ui.components.GoalCard
import com.projeto.wellio.domain.home.CardSize
import com.projeto.wellio.ui.module.ExerciseModule
import com.projeto.wellio.ui.module.Summary
import com.projeto.wellio.ui.module.exerciseSummary
import com.projeto.wellio.ui.theme.WellioTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExerciseCardViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<Pair<Summary, List<ExerciseType>>> = combine(
        ExerciseModule.summary(container),
        container.exerciseRepository.types,
        ::Pair
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), exerciseSummary(0, 0) to emptyList())

    fun add(type: ExerciseType, minutes: Int) {
        viewModelScope.launch { container.exerciseRepository.add(type, minutes) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ExerciseCardViewModel((this[APPLICATION_KEY] as WellioApp).container) }
        }
    }
}

/** Card de Exercício na Home; registra pelo dialog antigo até a tela de Exercício chegar. */
@Composable
fun ExerciseHomeCard(
    size: CardSize,
    modifier: Modifier = Modifier,
    viewModel: ExerciseCardViewModel = viewModel(factory = ExerciseCardViewModel.Factory)
) {
    val summary = viewModel.state.collectAsStateWithLifecycle().value.first
    var registering by remember { mutableStateOf(false) }
    GoalCard(
        title = ExerciseModule.title,
        icon = ExerciseModule.icon,
        colors = WellioTheme.colors.exercise,
        summary = summary,
        size = size,
        onRegister = { registering = true },
        modifier = modifier
    )
    if (registering) ExerciseRegister(onClose = { registering = false }, viewModel = viewModel)
}

/** Registrar exercício (dialog antigo até a tela de Exercício); usado pelo card e pelo FAB. */
@Composable
fun ExerciseRegister(
    onClose: () -> Unit,
    viewModel: ExerciseCardViewModel = viewModel(factory = ExerciseCardViewModel.Factory)
) {
    val types = viewModel.state.collectAsStateWithLifecycle().value.second
    ExerciseDialog(
        exercises = types,
        onDismiss = onClose,
        onConfirm = { type, minutes ->
            viewModel.add(type, minutes)
            onClose()
        }
    )
}

package com.projeto.wellio.ui.food

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
import com.projeto.wellio.domain.model.Food
import com.projeto.wellio.ui.components.FoodDialog
import com.projeto.wellio.ui.components.GoalCard
import com.projeto.wellio.ui.module.CardSize
import com.projeto.wellio.ui.module.FoodModule
import com.projeto.wellio.ui.module.Summary
import com.projeto.wellio.ui.module.foodSummary
import com.projeto.wellio.ui.theme.WellioTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FoodCardViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<Pair<Summary, List<Food>>> = combine(
        FoodModule.summary(container),
        container.foodRepository.foods,
        ::Pair
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), foodSummary(0, 0) to emptyList())

    fun add(food: Food, portions: Double) {
        viewModelScope.launch { container.foodRepository.add(food, portions) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { FoodCardViewModel((this[APPLICATION_KEY] as WellioApp).container) }
        }
    }
}

/** Card de Alimentação na Home; registra pelo dialog antigo até o M1b. */
@Composable
fun FoodHomeCard(
    size: CardSize,
    modifier: Modifier = Modifier,
    viewModel: FoodCardViewModel = viewModel(factory = FoodCardViewModel.Factory)
) {
    val (summary, foods) = viewModel.state.collectAsStateWithLifecycle().value
    var registering by remember { mutableStateOf(false) }
    GoalCard(
        title = FoodModule.title,
        icon = FoodModule.icon,
        colors = WellioTheme.colors.food,
        summary = summary,
        size = size,
        onRegister = { registering = true },
        modifier = modifier
    )
    if (registering) {
        FoodDialog(
            foods = foods,
            onDismiss = { registering = false },
            onConfirm = { food, portions ->
                viewModel.add(food, portions)
                registering = false
            }
        )
    }
}

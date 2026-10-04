package com.projeto.wellio.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.ui.components.DashboardCard
import com.projeto.wellio.ui.components.DashboardDetailsDialog
import com.projeto.wellio.ui.components.GoalSettingDialog
import com.projeto.wellio.ui.module.CardSize
import com.projeto.wellio.ui.module.ExerciseModule
import com.projeto.wellio.ui.module.FoodModule
import com.projeto.wellio.ui.module.WaterModule
import com.projeto.wellio.ui.module.WeightModule
import com.projeto.wellio.ui.module.WellioModule

@Composable
fun HomeRoute(
    onMenuClick: () -> Unit,
    onOpenModule: (WellioModule) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onMenuClick = onMenuClick,
        onUpdateGoal = viewModel::updateGoal
    ) { module, size, modifier ->
        module.HomeCard(size, onOpen = { onOpenModule(module) }, modifier = modifier)
    }
}

/** Altura provisória de uma linha da Home, até o grid calcular por aparelho (etapa 4 do M1.5). */
private val RowHeight = 136.dp

/**
 * Home com o layout padrão (SPEC §5.1). [moduleCard] desenha o card de um módulo; separado
 * para a tela poder ser testada sem os ViewModels dos cards.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onMenuClick: () -> Unit,
    onUpdateGoal: (GoalType, Int, Int) -> Unit,
    moduleCard: @Composable (WellioModule, CardSize, Modifier) -> Unit
) {
    var goalBeingEdited by remember { mutableStateOf<GoalType?>(null) }
    var showDetails by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Abrir menu")
            }

            DashboardCard(
                summaries = state.summaries,
                lastWeek = state.lastWeek,
                onClick = { showDetails = true },
                onGoalClick = { goalBeingEdited = it },
                modifier = Modifier.height(RowHeight)
            )
            moduleCard(WaterModule, CardSize.Full, Modifier.fillMaxWidth().height(RowHeight))
            Row(Modifier.height(RowHeight), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                moduleCard(FoodModule, CardSize.Half, Modifier.weight(1f).fillMaxHeight())
                moduleCard(ExerciseModule, CardSize.Half, Modifier.weight(1f).fillMaxHeight())
            }
            Row(Modifier.height(RowHeight), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                moduleCard(WeightModule, CardSize.Half, Modifier.weight(1f).fillMaxHeight())
                Spacer(Modifier.weight(1f))
            }
        }
    }

    goalBeingEdited?.let { type ->
        val goal = state.goals[type]
        GoalSettingDialog(
            type = type,
            currentDaily = goal.daily,
            currentWeekly = goal.weekly,
            onDismiss = { goalBeingEdited = null },
            onConfirm = { newDaily, newWeekly ->
                onUpdateGoal(type, newDaily, newWeekly)
                goalBeingEdited = null
            }
        )
    }

    if (showDetails) {
        DashboardDetailsDialog(
            onDismiss = { showDetails = false },
            getDataForType = { type ->
                val goal = state.goals[type]
                Triple(state.today[type], goal.daily, goal.weekly)
            },
            getWeekTotal = { type -> state.week[type] }
        )
    }
}

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.projeto.wellio.domain.home.CardSize
import com.projeto.wellio.domain.home.DASHBOARD_KEY
import com.projeto.wellio.domain.home.HomeCard
import com.projeto.wellio.domain.home.homeRows
import com.projeto.wellio.domain.home.rowLayout
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.ui.components.DashboardCard
import com.projeto.wellio.ui.components.DashboardDetailsDialog
import com.projeto.wellio.ui.components.DashboardHalfCard
import com.projeto.wellio.ui.components.GoalSettingDialog
import com.projeto.wellio.ui.module.Modules
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

private val Gutter = 12.dp
private val EdgePadding = 16.dp

/**
 * Home (SPEC §5.1): os cards do layout do usuário em linhas que preenchem a tela. O número
 * de linhas vem da altura disponível e da escala da fonte; com mais linhas que isso, rola.
 * [moduleCard] desenha o card de um módulo; separado para a tela poder ser testada sem os
 * ViewModels dos cards.
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
    val fontScale = LocalDensity.current.fontScale

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize()) {
            IconButton(onClick = onMenuClick, modifier = Modifier.padding(start = 4.dp)) {
                Icon(Icons.Default.Menu, contentDescription = "Abrir menu")
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val rows = rowLayout(
                    availableHeightDp = (maxHeight - EdgePadding).value,
                    fontScale = fontScale,
                    spacingDp = Gutter.value
                )
                val rowHeight = rows.rowHeightDp.dp
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = EdgePadding, end = EdgePadding, bottom = EdgePadding),
                    verticalArrangement = Arrangement.spacedBy(Gutter)
                ) {
                    homeRows(state.layout).forEach { row ->
                        Row(Modifier.fillMaxWidth().height(rowHeight), horizontalArrangement = Arrangement.spacedBy(Gutter)) {
                            row.forEach { card ->
                                HomeCardSlot(
                                    card = card,
                                    state = state,
                                    moduleCard = moduleCard,
                                    onDashboardClick = { showDetails = true },
                                    onGoalClick = { goalBeingEdited = it },
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                            // Metade sem vizinho: o espaço à direita fica vazio (SPEC §5.1)
                            if (row.size == 1 && row[0].size == CardSize.Half) Spacer(Modifier.weight(1f))
                        }
                    }
                }
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

/** Desenha um card do layout: o Dashboard ou o card do módulo. */
@Composable
private fun HomeCardSlot(
    card: HomeCard,
    state: HomeUiState,
    moduleCard: @Composable (WellioModule, CardSize, Modifier) -> Unit,
    onDashboardClick: () -> Unit,
    onGoalClick: (GoalType) -> Unit,
    modifier: Modifier
) {
    if (card.key == DASHBOARD_KEY) {
        when (card.size) {
            CardSize.Full -> DashboardCard(state.summaries, state.lastWeek, onDashboardClick, onGoalClick, modifier)
            CardSize.Half -> DashboardHalfCard(state.summaries, state.lastWeek.lastOrNull()?.score ?: 0f, onDashboardClick, modifier)
        }
    } else {
        Modules.byKey(card.key)?.let { moduleCard(it, card.size, modifier) }
    }
}

package com.projeto.wellio.ui.home

import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import com.projeto.wellio.domain.home.addCard
import com.projeto.wellio.domain.home.freeRows
import com.projeto.wellio.domain.home.hideCard
import com.projeto.wellio.domain.home.toggleSize
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
    onEditingChange: (Boolean) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onMenuClick = onMenuClick,
        onUpdateGoal = viewModel::updateGoal,
        onSaveLayout = viewModel::saveLayout,
        onDismissSuggestion = viewModel::dismissSuggestion,
        onEditingChange = onEditingChange
    ) { module, size, modifier ->
        module.HomeCard(size, onOpen = { onOpenModule(module) }, modifier = modifier)
    }
}

private val Gutter = 12.dp
private val EdgePadding = 16.dp

/** Altura das prévias no catálogo (perto da altura real das linhas). */
private val PreviewRowHeight = 136.dp

/** Cards que podem ir para a Home: o Dashboard e o de cada módulo. */
private val catalog: List<CatalogEntry> =
    listOf(CatalogEntry(DASHBOARD_KEY, "Dashboard")) + Modules.all.map { CatalogEntry(it.id.key, it.title) }

private fun titleOf(card: HomeCard) = catalog.firstOrNull { it.key == card.key }?.title ?: card.key

/**
 * Home (SPEC §5.1 e §5.2): os cards do layout do usuário em linhas que preenchem a tela.
 * Segurar um card entra no modo de edição, que mexe num rascunho salvo ao concluir.
 * [moduleCard] desenha o card de um módulo; separado para a tela poder ser testada sem os
 * ViewModels dos cards.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onMenuClick: () -> Unit,
    onUpdateGoal: (GoalType, Int, Int) -> Unit,
    onSaveLayout: (List<HomeCard>) -> Unit,
    onDismissSuggestion: () -> Unit,
    onEditingChange: (Boolean) -> Unit = {},
    moduleCard: @Composable (WellioModule, CardSize, Modifier) -> Unit
) {
    var goalBeingEdited by remember { mutableStateOf<GoalType?>(null) }
    var showDetails by remember { mutableStateOf(false) }
    // Modo de edição: o layout sendo mexido (null = fora da edição)
    var draft by remember { mutableStateOf<List<HomeCard>?>(null) }
    var showCatalog by remember { mutableStateOf(false) }
    val fontScale = LocalDensity.current.fontScale
    val cards = draft ?: state.layout
    val editing = draft != null

    fun finishEditing() {
        draft?.let(onSaveLayout)
        draft = null
    }
    BackHandler(enabled = editing) { finishEditing() }
    // A sidebar não abre pelo gesto enquanto os cards estão sendo mexidos (edição ou editor de um card)
    var cardEditorOpen by remember { mutableStateOf(false) }
    LaunchedEffect(editing, cardEditorOpen) { onEditingChange(editing || cardEditorOpen) }

    val slot: @Composable (HomeCard, Modifier) -> Unit = { card, modifier ->
        HomeCardSlot(card, state, moduleCard, onDashboardClick = { showDetails = true }, onGoalClick = { goalBeingEdited = it }, modifier)
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize()) {
            Box(Modifier.height(56.dp), contentAlignment = Alignment.CenterStart) {
                if (editing) {
                    EditBar(onAddCard = { showCatalog = true }, onDone = ::finishEditing)
                } else {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "Abrir menu")
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { draft = state.layout }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar Home")
                        }
                    }
                }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val rows = rowLayout(
                    availableHeightDp = (maxHeight - EdgePadding - 8.dp).value,
                    fontScale = fontScale,
                    spacingDp = Gutter.value
                )
                val rowHeight = rows.rowHeightDp.dp
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        // Respiro no alto para os selos da edição não serem cortados
                        .padding(start = EdgePadding, end = EdgePadding, top = 8.dp, bottom = EdgePadding)
                        // Tocar fora dos cards sai da edição (SPEC §5.2)
                        .pointerInput(editing) { if (editing) detectTapGestures { finishEditing() } },
                    verticalArrangement = Arrangement.spacedBy(Gutter)
                ) {
                    HomeGrid(
                        cards = cards,
                        editing = editing,
                        rowHeight = rowHeight,
                        gutter = Gutter,
                        onStartEditing = { if (draft == null) draft = state.layout },
                        onChange = { draft = it },
                        onQuickAction = { key, action ->
                            val index = state.layout.indexOfFirst { it.key == key }
                            when (action) {
                                QuickAction.Hide -> onSaveLayout(hideCard(state.layout, index))
                                QuickAction.ToggleSize -> onSaveLayout(toggleSize(state.layout, index))
                            }
                        },
                        onCardEditorChange = { cardEditorOpen = it },
                        cardTitle = ::titleOf,
                        modifier = Modifier.fillMaxWidth(),
                        card = slot
                    )
                    if (!editing && state.showSuggestion && freeRows(cards, rows.count) > 0 && catalog.any { entry -> cards.none { it.key == entry.key } }) {
                        AddCardSuggestion(rowHeight, onClick = { showCatalog = true }, onDismiss = onDismissSuggestion)
                    }
                }
            }
        }
    }

    if (showCatalog) {
        CatalogSheet(
            entries = catalog,
            onHome = cards.map { it.key }.toSet(),
            rowHeight = PreviewRowHeight,
            gutter = Gutter,
            onDismiss = { showCatalog = false },
            onAdd = { card ->
                showCatalog = false
                if (editing) draft = addCard(cards, card) else onSaveLayout(addCard(cards, card))
            },
            card = slot
        )
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

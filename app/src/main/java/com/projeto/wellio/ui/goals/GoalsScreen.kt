package com.projeto.wellio.ui.goals

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.projeto.wellio.R
import com.projeto.wellio.domain.goals.PeriodGoal
import com.projeto.wellio.domain.goals.PeriodGoalStatus
import com.projeto.wellio.domain.goals.PeriodMetric
import com.projeto.wellio.domain.goals.PeriodKind
import com.projeto.wellio.domain.goals.Rings
import com.projeto.wellio.domain.model.ExerciseUnit
import com.projeto.wellio.domain.model.Goal
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.ui.components.RecordsHeader
import com.projeto.wellio.ui.exercise.Campfire
import com.projeto.wellio.ui.format.formatInt
import com.projeto.wellio.ui.format.formatKg
import com.projeto.wellio.ui.format.formatLiters
import com.projeto.wellio.ui.profile.OptionSheet
import com.projeto.wellio.ui.theme.DarkWellioColors
import com.projeto.wellio.ui.theme.WellioTheme
import com.projeto.wellio.ui.water.WaterDrop
import com.projeto.wellio.ui.weight.WeightPicker
import com.projeto.wellio.ui.weight.WeightPickerState
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun GoalsRoute(
    onBack: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    viewModel: GoalsViewModel = viewModel(factory = GoalsViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GoalsScreen(
        state = state,
        onBack = onBack,
        onOpenDay = onOpenDay,
        onSaveDaily = viewModel::saveDaily,
        onWeekStart = viewModel::setWeekStart,
        suggest = viewModel::suggestTarget,
        onCreate = viewModel::create,
        onCreateWeight = viewModel::setWeightPeriod,
        onUpdate = viewModel::update,
        onDelete = viewModel::delete,
        onRestore = viewModel::restore,
        onRemoveWeight = viewModel::removeWeightPeriod,
        onCelebrated = viewModel::celebrated,
        onWeightCelebrated = viewModel::weightCelebrated
    )
}

private sealed interface GoalsEditing {
    data class Daily(val type: GoalType) : GoalsEditing
    data object WeekStart : GoalsEditing
    data object New : GoalsEditing
    data class Period(val goal: PeriodGoal) : GoalsEditing
    data object WeightPeriod : GoalsEditing
}

private val RingOrder = listOf(GoalType.WATER, GoalType.FOOD, GoalType.EXERCISE)

/**
 * Tela de Metas (SPEC §9.2): a semana em anéis no topo; as metas diárias com a semanal e o
 * início da semana; as metas de período com ritmo e projeção; e o histórico.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    state: GoalsUiState,
    onBack: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onSaveDaily: (GoalType, Goal, ExerciseUnit) -> Unit,
    onWeekStart: (DayOfWeek) -> Unit,
    suggest: suspend (PeriodMetric, PeriodKind, ClosedRange<LocalDate>) -> Double,
    onCreate: (PeriodGoal) -> Unit,
    onCreateWeight: (Double, LocalDate) -> Unit,
    onUpdate: (PeriodGoal) -> Unit,
    onDelete: (PeriodGoal) -> Unit,
    onRestore: (PeriodGoal) -> Unit,
    onRemoveWeight: () -> Unit,
    onCelebrated: (PeriodGoal) -> Unit,
    onWeightCelebrated: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<GoalsEditing?>(null) }
    var showingHistory by rememberSaveable { mutableStateOf(false) }
    // Confete por cima da tela enquanto alguma meta comemora
    var confetti by remember { mutableIntStateOf(0) }

    fun deleteWithUndo(goal: PeriodGoal) {
        onDelete(goal)
        editing = null
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar("Meta apagada", actionLabel = "Desfazer", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) onRestore(goal)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Metas") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "semana") { WeekRings(state, onOpenDay) }
                item(key = "diarias-titulo") { SectionTitle("Metas diárias") }
                item(key = "diarias") {
                    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(vertical = 8.dp)) {
                            RingOrder.forEach { type -> DailyRow(type, state) { editing = GoalsEditing.Daily(type) } }
                            HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                            SettingRow("A semana começa na", weekdayName(state.weekStart)) { editing = GoalsEditing.WeekStart }
                        }
                    }
                }
                item(key = "periodo-titulo") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("Metas de período", Modifier.weight(1f))
                        TextButton(onClick = { editing = GoalsEditing.New }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Nova")
                        }
                    }
                }
                state.weight?.let { weight ->
                    item(key = "periodo-peso") {
                        WeightPeriodCard(weight, onClick = { editing = GoalsEditing.WeightPeriod }, onCelebrate = {
                            confetti++
                            onWeightCelebrated()
                        })
                    }
                }
                items(state.active, key = { it.goal.id }) { view ->
                    PeriodGoalCard(view, state.today, onClick = { editing = GoalsEditing.Period(view.goal) }, onCelebrate = {
                        confetti++
                        onCelebrated(view.goal)
                    }, modifier = Modifier.animateItem())
                }
                if (state.active.isEmpty() && state.weight == null) {
                    item(key = "periodo-vazio") {
                        Text(
                            "Crie uma meta para a semana, o mês ou o ano: beber 60 L em outubro, 12 treinos no mês…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
                if (state.finished.isNotEmpty()) {
                    item(key = "historico-titulo") {
                        RecordsHeader(
                            title = "Histórico",
                            count = state.finished.size,
                            expanded = showingHistory,
                            onToggle = { showingHistory = !showingHistory }
                        )
                    }
                    if (showingHistory) {
                        items(state.finished, key = { "h-" + it.id }) { goal -> FinishedRow(goal, Modifier.animateItem()) }
                    }
                }
            }
        }
        if (confetti > 0) {
            Confetti(seed = confetti, modifier = Modifier.fillMaxSize())
        }
    }

    when (val current = editing) {
        is GoalsEditing.Daily -> DailyGoalSheet(
            type = current.type,
            goals = state.goals,
            suggestion = state.suggestions[current.type],
            onDismiss = { editing = null },
            onSave = { goal, unit ->
                onSaveDaily(current.type, goal, unit)
                editing = null
            }
        )
        GoalsEditing.WeekStart -> OptionSheet(
            title = "A semana começa na",
            options = DayOfWeek.entries,
            selected = state.weekStart,
            label = { weekdayName(it).replaceFirstChar(Char::uppercase) },
            onDismiss = { editing = null },
            onPick = {
                onWeekStart(it)
                editing = null
            },
            onClear = null
        )
        GoalsEditing.New -> CreatePeriodGoalSheet(
            today = state.today,
            weekStart = state.weekStart,
            currentKg = state.currentKg,
            hasWeightGoal = state.weight != null,
            suggest = suggest,
            onDismiss = { editing = null },
            onCreate = {
                onCreate(it)
                editing = null
            },
            onCreateWeight = { kg, deadline ->
                onCreateWeight(kg, deadline)
                editing = null
            }
        )
        is GoalsEditing.Period -> EditPeriodGoalSheet(
            goal = current.goal,
            onDismiss = { editing = null },
            onSave = {
                onUpdate(it)
                editing = null
            },
            onDelete = { deleteWithUndo(current.goal) }
        )
        GoalsEditing.WeightPeriod -> state.weight?.let { weight ->
            WeightPeriodSheet(
                weight = weight,
                today = state.today,
                onDismiss = { editing = null },
                onSave = { kg, deadline ->
                    onCreateWeight(kg, deadline)
                    editing = null
                },
                onRemove = {
                    onRemoveWeight()
                    editing = null
                }
            )
        }
        null -> Unit
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(start = 8.dp, top = 8.dp)
    )
}

/** A semana em anéis (SPEC §9.2): os 7 dias com os anéis pequenos; tocar abre o Dashboard naquele dia. */
@Composable
private fun WeekRings(state: GoalsUiState, onOpenDay: (LocalDate) -> Unit) {
    val colors = WellioTheme.colors.base
    Surface(shape = RoundedCornerShape(28.dp), color = colors.container, contentColor = colors.onContainer, modifier = Modifier.padding(top = 4.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth()) {
                state.week.forEach { day ->
                    val isToday = day.date == state.today
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .then(if (day.future) Modifier else Modifier.clickable(onClickLabel = "Abrir o dia") { onOpenDay(day.date) })
                            .padding(vertical = 6.dp)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "${day.date.dayOfMonth}: " +
                                    if (day.future) "ainda não chegou" else "${(day.rings.score * 100).toInt()} por cento"
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            weekdayInitial(day.date),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = colors.onContainer.copy(alpha = if (isToday) 1f else 0.7f)
                        )
                        Spacer(Modifier.height(6.dp))
                        GoalRings(if (day.future) Rings(0f, 0f, 0f) else day.rings, size = 38.dp, stroke = 4.dp)
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.size(4.dp).clip(CircleShape).background(if (isToday) colors.accent else Color.Transparent))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "${state.closedDays} de 7 dias com os anéis fechados",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onContainer.copy(alpha = 0.85f)
            )
        }
    }
}

private fun ringColor(type: GoalType) = when (type) {
    GoalType.WATER -> DarkWellioColors.water.accent
    GoalType.FOOD -> DarkWellioColors.food.accent
    GoalType.EXERCISE -> DarkWellioColors.exercise.accent
}

/** "2.000 ml/dia · 14 L/semana (auto)". */
private fun dailyText(type: GoalType, goal: Goal, unit: ExerciseUnit): String {
    val label = goalUnit(type, unit)
    val weekly = if (type == GoalType.WATER) formatLiters(goal.weekly) else "${formatInt(goal.weekly)} $label"
    return "${formatInt(goal.daily)} $label/dia · $weekly/semana" + if (goal.weeklyOverride == null) " (auto)" else ""
}

@Composable
private fun DailyRow(type: GoalType, state: GoalsUiState, onClick: () -> Unit) {
    val text = dailyText(type, state.goals[type], state.goals.exerciseUnit)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Mudar meta", onClick = onClick)
            .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
            .semantics(mergeDescendants = true) { contentDescription = "${goalTitle(type)}: $text" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(ringColor(type)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(goalTitle(type), style = MaterialTheme.typography.bodyLarge)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Módulo da métrica, para o elemento do cartão. */
private enum class MetricModule { Water, Food, Exercise, Weight }

private fun moduleOf(metric: PeriodMetric) = when (metric) {
    PeriodMetric.WaterTotal, PeriodMetric.WaterDays -> MetricModule.Water
    PeriodMetric.KcalTotal, PeriodMetric.KcalDays -> MetricModule.Food
    PeriodMetric.ExerciseKcalTotal, PeriodMetric.ExerciseMinutesTotal, PeriodMetric.ExerciseDays, PeriodMetric.Workouts -> MetricModule.Exercise
    PeriodMetric.Weight -> MetricModule.Weight
}

/**
 * Elemento do módulo no cartão. [celebrating] passa a valer quando a meta conclui: a gota
 * transborda, a fogueira vira labareda.
 */
@Composable
private fun MetricElement(module: MetricModule, celebrating: Boolean) {
    val additions = if (celebrating) 1 else 0
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        when (module) {
            MetricModule.Water -> WaterDrop(totalMl = 1, goalMl = 1, additions = additions, modifier = Modifier.size(width = 40.dp, height = 48.dp))
            MetricModule.Exercise -> Campfire(total = 1, goal = 1, additions = additions, modifier = Modifier.size(width = 44.dp, height = 48.dp))
            MetricModule.Food -> Box(
                Modifier.size(44.dp).clip(CircleShape).background(WellioTheme.colors.food.container),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_module_food), contentDescription = null, tint = WellioTheme.colors.food.accent, modifier = Modifier.size(24.dp))
            }
            MetricModule.Weight -> Box(
                Modifier.size(44.dp).clip(CircleShape).background(WellioTheme.colors.base.container),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_module_weight), contentDescription = null, tint = WellioTheme.colors.base.onContainer, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/** Barra com a marca de "onde deveria estar hoje" (SPEC §9.2). */
@Composable
private fun PaceBar(fraction: Float, expected: Float, color: Color) {
    val shown by animateFloatAsState(fraction, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow), label = "progresso")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val mark = MaterialTheme.colorScheme.onSurface
    BoxWithConstraints(Modifier.fillMaxWidth().height(14.dp), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(track)) {
            Box(Modifier.fillMaxWidth(shown).fillMaxHeight().background(color))
        }
        if (expected in 0.001f..0.999f) {
            Box(Modifier.offset(x = maxWidth * expected - 1.dp).width(2.dp).height(14.dp).background(mark.copy(alpha = 0.7f)))
        }
    }
}

private fun moduleColor(module: MetricModule) = when (module) {
    MetricModule.Water -> DarkWellioColors.water.accent
    MetricModule.Food -> DarkWellioColors.food.accent
    MetricModule.Exercise -> DarkWellioColors.exercise.accent
    MetricModule.Weight -> DarkWellioColors.base.accent
}

/** Cartão com ritmo (SPEC §9.2): barra com a marca de hoje, valor, dias restantes e projeção. */
@Composable
private fun PeriodGoalCard(view: PeriodGoalView, today: LocalDate, onClick: () -> Unit, onCelebrate: () -> Unit, modifier: Modifier = Modifier) {
    val goal = view.goal
    val progress = view.progress
    val module = moduleOf(goal.metric)
    val done = progress.reached || goal.status == PeriodGoalStatus.Completed
    // Concluiu e ainda não comemorou: comemora uma vez ao aparecer
    var celebrating by remember(goal.id) { mutableStateOf(false) }
    LaunchedEffect(goal.id, progress.reached, goal.celebrated) {
        if (progress.reached && !goal.celebrated) {
            celebrating = true
            onCelebrate()
        }
    }
    GoalCardFrame(onClick, modifier) {
        MetricElement(module, celebrating)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(goal.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            PaceBar(progress.fraction, progress.expected, moduleColor(module))
            Spacer(Modifier.height(6.dp))
            Text(
                "${formatMetric(goal.metric, progress.value)} de ${formatMetric(goal.metric, progress.target)} · " +
                    if (done) "concluída!" else daysLeftText(progress.daysLeft),
                style = MaterialTheme.typography.bodyMedium
            )
            if (!done) {
                progress.projection?.let {
                    Text(
                        "No ritmo atual: ~${formatMetric(goal.metric, it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun daysLeftText(days: Int) = when (days) {
    0 -> "último dia"
    1 -> "falta 1 dia"
    else -> "faltam $days dias"
}

@Composable
private fun GoalCardFrame(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

private val shortDay = DateTimeFormatter.ofPattern("dd/MM")

/** O peso alvo com prazo, no mesmo formato dos outros cartões. */
@Composable
private fun WeightPeriodCard(weight: WeightPeriodView, onClick: () -> Unit, onCelebrate: () -> Unit) {
    val progress = weight.progress
    LaunchedEffect(weight.reached, weight.celebrated) {
        if (weight.reached && !weight.celebrated) onCelebrate()
    }
    GoalCardFrame(onClick) {
        MetricElement(MetricModule.Weight, celebrating = false)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Chegar a ${formatKg(weight.goal.targetKg)} até ${weight.goal.deadline?.format(shortDay)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            PaceBar(progress.fraction, progress.expected, moduleColor(MetricModule.Weight))
            Spacer(Modifier.height(6.dp))
            Text(
                "${formatKg(weight.currentKg)} agora · " + if (weight.reached) "chegou lá!" else daysLeftText(progress.daysLeft),
                style = MaterialTheme.typography.bodyMedium
            )
            if (!weight.reached) {
                progress.projection?.let { projected ->
                    Text(
                        if (projected >= progress.target) "No ritmo atual: chega lá no prazo"
                        else "No ritmo atual: ~${formatKg(projected)} dos ${formatKg(progress.target)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Mudar o alvo ou o prazo da meta de peso, ou tirar o prazo (o alvo fica na tela de Peso). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightPeriodSheet(
    weight: WeightPeriodView,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Double, LocalDate) -> Unit,
    onRemove: () -> Unit
) {
    val colors = WellioTheme.colors.base
    val picker = remember { WeightPickerState(weight.goal.targetKg) }
    var deadline by remember { mutableStateOf(weight.goal.deadline ?: today.plusDays(30)) }
    var picking by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text("Meta de peso", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(8.dp))
            WeightPicker(picker)
            OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text("Até ${deadline.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onRemove) { Text("Tirar o prazo") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { picker.weight?.let { onSave(it, deadline) } },
                    enabled = picker.weight != null,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.container, contentColor = colors.onContainer)
                ) { Text("Salvar") }
            }
        }
    }
    if (picking) {
        FutureDatePickerDialog(deadline, from = today.plusDays(1), onDismiss = { picking = false }, onPick = {
            deadline = it
            picking = false
        })
    }
}

/** Meta terminada no histórico: nome, resultado e selo. */
@Composable
private fun FinishedRow(goal: PeriodGoal, modifier: Modifier = Modifier) {
    val completed = goal.status == PeriodGoalStatus.Completed
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(goal.title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${formatMetric(goal.metric, goal.finalValue ?: 0.0)} de ${formatMetric(goal.metric, goal.target)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = if (completed) WellioTheme.colors.food.container else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (completed) WellioTheme.colors.food.onContainer else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    if (completed) "Concluída" else "Não concluída",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

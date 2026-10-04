package com.projeto.wellio.ui.weight

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.projeto.wellio.domain.model.WeightEntry
import com.projeto.wellio.domain.weight.DEFAULT_WEIGHT_KG
import com.projeto.wellio.domain.weight.WeightPeriod
import com.projeto.wellio.domain.weight.goalProgress
import com.projeto.wellio.domain.weight.projectedDate
import com.projeto.wellio.domain.weight.remainingKg
import com.projeto.wellio.domain.weight.summarize
import com.projeto.wellio.domain.weight.trendChange
import com.projeto.wellio.domain.weight.weeklyRate
import com.projeto.wellio.domain.weight.weightTrend
import com.projeto.wellio.ui.components.RecordsHeader
import com.projeto.wellio.ui.format.dayLabel
import com.projeto.wellio.ui.format.formatKg
import com.projeto.wellio.ui.format.formatKgChange
import com.projeto.wellio.ui.format.shortDate
import com.projeto.wellio.ui.theme.WellioTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun WeightRoute(
    onBack: () -> Unit,
    viewModel: WeightViewModel = viewModel(factory = WeightViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WeightScreen(
        state = state,
        onBack = onBack,
        onSave = viewModel::save,
        onUpdate = viewModel::update,
        onDelete = viewModel::delete,
        onRestore = viewModel::restore,
        onSetTarget = viewModel::setTarget,
        onClearTarget = viewModel::clearTarget
    )
}

private sealed interface WeightEditing {
    data object New : WeightEditing
    data class Existing(val entry: WeightEntry) : WeightEditing
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    state: WeightUiState,
    onBack: () -> Unit,
    onSave: (LocalDate, Double) -> Unit,
    onUpdate: (WeightEntry, LocalDate, Double) -> Unit,
    onDelete: (WeightEntry) -> Unit,
    onRestore: (WeightEntry) -> Unit,
    onSetTarget: (Double) -> Unit,
    onClearTarget: () -> Unit
) {
    val colors = WellioTheme.colors.base
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<WeightEditing?>(null) }
    var editingTarget by remember { mutableStateOf(false) }
    var showingHistory by rememberSaveable { mutableStateOf(false) }

    fun deleteWithUndo(entry: WeightEntry) {
        onDelete(entry)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = "Pesagem de ${dayLabel(entry.date, state.today).lowercase()} apagada",
                actionLabel = "Desfazer",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onRestore(entry)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Peso") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = WeightEditing.New },
                containerColor = colors.container,
                contentColor = colors.onContainer,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Registrar peso") }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val latest = state.entries.firstOrNull()
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "balanca") {
                BathroomScale(
                    currentKg = latest?.first?.weightKg,
                    subtitle = latest?.let { "Pesagem de ${dayLabel(it.first.date, state.today).lowercase()}" }
                        ?: "Sem pesagens: o cálculo usa ${formatKg(DEFAULT_WEIGHT_KG)}",
                    detail = latest?.second?.let { "${formatKgChange(it)} desde a anterior" },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            item(key = "alvo") {
                TargetCard(state, onClick = { editingTarget = true })
            }
            if (state.entries.isNotEmpty()) {
                item(key = "evolucao") { EvolutionCard(state) }
            }
            if (state.entries.isEmpty()) {
                item(key = "vazio") {
                    Text(
                        "Registre seu peso para o cálculo de calorias dos exercícios ficar certo.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            } else {
                item(key = "titulo") {
                    RecordsHeader(
                        title = "Histórico",
                        count = state.entries.size,
                        expanded = showingHistory,
                        onToggle = { showingHistory = !showingHistory }
                    )
                }
                if (showingHistory) {
                    items(state.entries, key = { it.first.id }) { (entry, change) ->
                        WeightRow(
                            entry = entry,
                            change = change,
                            today = state.today,
                            onClick = { editing = WeightEditing.Existing(entry) },
                            onDelete = { deleteWithUndo(entry) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }

    editing?.let { current ->
        val existing = (current as? WeightEditing.Existing)?.entry
        WeightSheet(
            entry = existing,
            suggestedKg = state.currentKg,
            today = state.today,
            onDismiss = { editing = null },
            onSave = { day, kg ->
                if (existing == null) onSave(day, kg) else onUpdate(existing, day, kg)
                editing = null
            },
            onDelete = existing?.let { entry ->
                {
                    editing = null
                    deleteWithUndo(entry)
                }
            }
        )
    }

    if (editingTarget) {
        TargetSheet(
            targetKg = state.goal?.targetKg,
            currentKg = state.currentKg,
            startKg = state.goal?.startKg ?: state.currentKg,
            onDismiss = { editingTarget = false },
            onSave = {
                onSetTarget(it)
                editingTarget = false
            },
            onRemove = state.goal?.let {
                {
                    onClearTarget()
                    editingTarget = false
                }
            }
        )
    }
}

/** Peso alvo: progresso, quanto falta e a data estimada; sem alvo, um convite para definir. */
@Composable
private fun TargetCard(state: WeightUiState, onClick: () -> Unit) {
    val colors = WellioTheme.colors.base
    val goal = state.goal
    val currentKg = state.currentKg
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (goal == null) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Peso alvo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Defina um alvo para acompanhar o progresso",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            return@Surface
        }

        val remaining = remainingKg(goal, currentKg)
        val progress = goalProgress(goal, currentKg)
        val rate = weeklyRate(weightTrend(state.entries.map { it.first }, state.today, WeightPeriod.Month))
        val eta = projectedDate(goal, currentKg, rate, state.today)
        Column(Modifier.padding(20.dp).semantics(mergeDescendants = true) {}) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Alvo ${formatKg(goal.targetKg)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(
                    if (remaining == 0.0) "Chegou lá!" else "faltam ${formatKg(remaining)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(colors.container))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                listOfNotNull(
                    "Começou em ${formatKg(goal.startKg)}",
                    eta?.let { "chega lá por volta de ${shortDate(it, state.today)}" }
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Curva por período, com resumo (menor, maior, média) e ritmo. */
@Composable
private fun EvolutionCard(state: WeightUiState) {
    var period by rememberSaveable { mutableStateOf(WeightPeriod.Month) }
    val trend = remember(state.entries, state.today, period) {
        weightTrend(state.entries.map { it.first }, state.today, period)
    }
    val change = trendChange(trend)
    val summary = summarize(trend)
    val rate = weeklyRate(trend)
    val scheme = MaterialTheme.colorScheme

    Surface(shape = RoundedCornerShape(24.dp), color = scheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 20.dp)) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Evolução", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                change?.let {
                    Text(
                        "${formatKgChange(it)} em ${period.label.lowercase()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.primary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (trend.size >= 2) {
                WeightChart(
                    trend = trend,
                    targetKg = state.goal?.targetKg,
                    today = state.today,
                    colors = ChartColors(line = scheme.primary, bubble = scheme.primary, onBubble = scheme.onPrimary, guide = scheme.onSurfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 8.dp)
                        .semantics {
                            contentDescription = "Curva de peso: de ${formatKg(trend.first().weightKg)} " +
                                "a ${formatKg(trend.last().weightKg)} em ${period.label.lowercase()}"
                        }
                )
                Text(
                    "Segure e arraste para ver cada pesagem",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            } else {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text("Registre mais pesagens para ver a curva", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeightPeriod.entries.forEach { option ->
                    val selected = option == period
                    Surface(
                        onClick = { period = option },
                        shape = RoundedCornerShape(50),
                        color = if (selected) scheme.primary else Color.Transparent,
                        contentColor = if (selected) scheme.onPrimary else scheme.onSurface,
                        border = if (selected) null else BorderStroke(1.dp, scheme.outlineVariant),
                        modifier = Modifier.semantics { this.selected = selected }
                    ) {
                        Text(
                            option.label,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            summary?.let {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stat("Menor", formatKg(it.minKg), Modifier.weight(1f))
                    Stat("Maior", formatKg(it.maxKg), Modifier.weight(1f))
                    Stat("Média", formatKg(it.averageKg), Modifier.weight(1f))
                    Stat("Por semana", rate?.let(::formatKgChange) ?: "—", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.semantics(mergeDescendants = true) {}
    ) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightRow(
    entry: WeightEntry,
    change: Double?,
    today: LocalDate,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState()
    val day = dayLabel(entry.date, today)
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        onDismiss = { onDelete() },
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = "Editar") { onClick() }
                .semantics(mergeDescendants = true) {
                    contentDescription = listOfNotNull(day, formatKg(entry.weightKg), change?.let(::formatKgChange))
                        .joinToString(", ")
                }
        ) {
            Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(day, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                change?.let {
                    Text(
                        formatKgChange(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Text(formatKg(entry.weightKg), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Apagar pesagem de ${day.lowercase()}")
                }
            }
        }
    }
}

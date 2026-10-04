package com.projeto.wellio.ui.weight

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.projeto.wellio.domain.weight.WeightPeriod
import com.projeto.wellio.domain.weight.trendChange
import com.projeto.wellio.domain.weight.weightTrend
import com.projeto.wellio.ui.icons.WellioIcons
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.projeto.wellio.domain.model.WeightEntry
import com.projeto.wellio.domain.weight.DEFAULT_WEIGHT_KG
import com.projeto.wellio.domain.weight.MAX_WEIGHT_KG
import com.projeto.wellio.domain.weight.MIN_WEIGHT_KG
import com.projeto.wellio.domain.weight.parseWeight
import com.projeto.wellio.ui.components.DayPickerDialog
import com.projeto.wellio.ui.format.dayLabel
import com.projeto.wellio.ui.format.formatKg
import com.projeto.wellio.ui.format.formatKgChange
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
        onRestore = viewModel::restore
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
    onRestore: (WeightEntry) -> Unit
) {
    val colors = WellioTheme.colors.base
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<WeightEditing?>(null) }

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
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item(key = "topo") {
                WeightHero(state, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            if (state.entries.isEmpty()) {
                item(key = "vazio") {
                    Text(
                        "Registre seu peso para o cálculo de calorias dos exercícios ficar certo.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                }
            } else {
                item(key = "titulo") {
                    Text(
                        "Histórico",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                    )
                }
                items(state.entries, key = { it.first.id }) { (entry, change) ->
                    WeightRow(
                        entry = entry,
                        change = change,
                        today = state.today,
                        onClick = { editing = WeightEditing.Existing(entry) },
                        onDelete = { deleteWithUndo(entry) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).animateItem()
                    )
                }
            }
        }
    }

    editing?.let { current ->
        val existing = (current as? WeightEditing.Existing)?.entry
        WeightSheet(
            entry = existing,
            suggestedKg = state.entries.firstOrNull()?.first?.weightKg ?: DEFAULT_WEIGHT_KG,
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
}

/** Topo da tela: peso atual, variação no período e a curva de evolução. */
@Composable
private fun WeightHero(state: WeightUiState, modifier: Modifier = Modifier) {
    val colors = WellioTheme.colors.base
    val current = state.entries.firstOrNull()?.first
    var period by rememberSaveable { mutableStateOf(WeightPeriod.Month) }
    val trend = remember(state.entries, state.today, period) {
        weightTrend(state.entries.map { it.first }, state.today, period)
    }
    val change = trendChange(trend)

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = colors.container,
        contentColor = colors.onContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                    Text("Peso atual", style = MaterialTheme.typography.labelLarge)
                    Text(
                        current?.let { formatKg(it.weightKg) } ?: "—",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        current?.let { "Pesagem de ${dayLabel(it.date, state.today).lowercase()}" }
                            ?: "Sem pesagens: o cálculo usa ${formatKg(DEFAULT_WEIGHT_KG)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                change?.let {
                    Surface(shape = RoundedCornerShape(50), color = colors.accent, contentColor = colors.onAccent) {
                        Text(
                            "${formatKgChange(it)} em ${period.label.lowercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (state.entries.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                if (trend.size >= 2) {
                    WeightChart(
                        trend,
                        line = colors.accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .semantics {
                                contentDescription = "Curva de peso: de ${formatKg(trend.first().weightKg)} " +
                                    "a ${formatKg(trend.last().weightKg)} em ${period.label.lowercase()}"
                            }
                    )
                } else {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Registre mais pesagens para ver a curva",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onContainer.copy(alpha = 0.75f)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeightPeriod.entries.forEach { option ->
                        val selected = option == period
                        Surface(
                            onClick = { period = option },
                            shape = RoundedCornerShape(50),
                            color = if (selected) colors.onContainer else Color.Transparent,
                            contentColor = if (selected) colors.container else colors.onContainer,
                            border = if (selected) null else BorderStroke(1.dp, colors.onContainer.copy(alpha = 0.4f)),
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
            }
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

/** "72,4" para os campos de texto. */
private fun kgText(kg: Double): String = formatKg(kg).removeSuffix(" kg")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightSheet(
    entry: WeightEntry?,
    suggestedKg: Double,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (LocalDate, Double) -> Unit,
    onDelete: (() -> Unit)?
) {
    val colors = WellioTheme.colors.base
    var weightText by remember { mutableStateOf(kgText(entry?.weightKg ?: suggestedKg)) }
    var lastValid by remember { mutableDoubleStateOf(entry?.weightKg ?: suggestedKg) }
    var typing by remember { mutableStateOf(false) }
    var day by remember { mutableStateOf(entry?.date ?: today) }
    var pickingDay by remember { mutableStateOf(false) }
    val weight = parseWeight(weightText)
    LaunchedEffect(weight) { weight?.let { lastValid = it } }

    fun step(delta: Double) {
        val next = (((parseWeight(weightText) ?: lastValid) + delta) * 10).roundToInt() / 10.0
        if (next in MIN_WEIGHT_KG..MAX_WEIGHT_KG) weightText = kgText(next)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                if (entry == null) "Registrar peso" else "Editar pesagem",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                FilledTonalIconButton(onClick = { step(-0.1) }) {
                    Icon(WellioIcons.Minus, contentDescription = "Diminuir 0,1 kg")
                }
                Text(
                    formatKg(weight ?: lastValid),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(onClick = { step(0.1) }) {
                    Icon(Icons.Default.Add, contentDescription = "Aumentar 0,1 kg")
                }
            }

            WeightRuler(
                value = weight ?: lastValid,
                onValueChange = { weightText = kgText(it) },
                tick = MaterialTheme.colorScheme.onSurfaceVariant,
                indicator = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            Column(Modifier.padding(horizontal = 24.dp)) {
                if (typing) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { text -> weightText = text.filter { it.isDigit() || it == ',' || it == '.' }.take(6) },
                        label = { Text("Peso (kg)") },
                        isError = weight == null,
                        supportingText = {
                            if (weight == null) Text("Entre ${MIN_WEIGHT_KG.toInt()} e ${MAX_WEIGHT_KG.toInt()} kg")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    TextButton(onClick = { typing = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Digitar o peso")
                    }
                }
                OutlinedButton(onClick = { pickingDay = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Dia: ${dayLabel(day, today)}")
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Apagar")
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { weight?.let { onSave(day, it) } },
                        enabled = weight != null,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.container, contentColor = colors.onContainer)
                    ) { Text("Salvar") }
                }
            }
        }
    }

    if (pickingDay) {
        DayPickerDialog(day, today, onDismiss = { pickingDay = false }, onPick = {
            day = it
            pickingDay = false
        })
    }
}

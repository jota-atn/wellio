package com.projeto.wellio.ui.weight

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
        val current = state.entries.firstOrNull()
        Column(Modifier.padding(innerPadding).fillMaxSize()) {
            CurrentWeight(current?.first, state.today, Modifier.padding(16.dp))

            if (state.entries.isEmpty()) {
                Text(
                    "Registre seu peso para o cálculo de calorias dos exercícios ficar certo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.entries, key = { it.first.id }) { (entry, change) ->
                        WeightRow(
                            entry = entry,
                            change = change,
                            today = state.today,
                            onClick = { editing = WeightEditing.Existing(entry) },
                            onDelete = { deleteWithUndo(entry) }
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

@Composable
private fun CurrentWeight(entry: WeightEntry?, today: LocalDate, modifier: Modifier = Modifier) {
    val colors = WellioTheme.colors.base
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.container, contentColor = colors.onContainer)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Peso atual", style = MaterialTheme.typography.labelLarge)
            Text(
                entry?.let { formatKg(it.weightKg) } ?: "—",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                entry?.let { "Pesagem de ${dayLabel(it.date, today).lowercase()}" }
                    ?: "Sem pesagens: o cálculo usa ${formatKg(DEFAULT_WEIGHT_KG)}",
                style = MaterialTheme.typography.bodyMedium
            )
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
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()
    val day = dayLabel(entry.date, today)
    SwipeToDismissBox(
        state = dismissState,
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
            Row(Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightSheet(
    entry: WeightEntry?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (LocalDate, Double) -> Unit,
    onDelete: (() -> Unit)?
) {
    val colors = WellioTheme.colors.base
    var weightText by remember { mutableStateOf(entry?.weightKg?.let { formatKg(it).removeSuffix(" kg") }.orEmpty()) }
    var day by remember { mutableStateOf(entry?.date ?: today) }
    var pickingDay by remember { mutableStateOf(false) }
    val weight = parseWeight(weightText)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text(if (entry == null) "Registrar peso" else "Editar pesagem", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = weightText,
                onValueChange = { text -> weightText = text.filter { it.isDigit() || it == ',' || it == '.' }.take(6) },
                label = { Text("Peso (kg)") },
                isError = weightText.isNotEmpty() && weight == null,
                supportingText = {
                    if (weightText.isNotEmpty() && weight == null) {
                        Text("Entre ${MIN_WEIGHT_KG.toInt()} e ${MAX_WEIGHT_KG.toInt()} kg")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
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

    if (pickingDay) {
        DayPickerDialog(day, today, onDismiss = { pickingDay = false }, onPick = {
            day = it
            pickingDay = false
        })
    }
}

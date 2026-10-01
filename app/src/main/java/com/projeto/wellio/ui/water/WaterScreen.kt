package com.projeto.wellio.ui.water

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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.projeto.wellio.domain.goals.progress
import com.projeto.wellio.domain.model.WaterContainer
import com.projeto.wellio.domain.model.WaterEntry
import com.projeto.wellio.ui.components.DayNavigator
import com.projeto.wellio.ui.components.swipeBetweenDays
import com.projeto.wellio.ui.format.formatMl
import com.projeto.wellio.ui.format.formatTime
import com.projeto.wellio.ui.theme.WellioTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun WaterRoute(
    onBack: () -> Unit,
    viewModel: WaterViewModel = viewModel(factory = WaterViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WaterScreen(
        state = state,
        zone = viewModel.zone,
        onBack = onBack,
        onDayChange = viewModel::selectDay,
        onAdd = viewModel::add,
        onUpdate = viewModel::update,
        onDelete = viewModel::delete,
        onRestore = viewModel::restore,
        onSaveContainer = viewModel::saveContainer,
        onDeleteContainer = viewModel::deleteContainer
    )
}

/** O que a folha de registro está editando: um registro novo ou um existente. */
private sealed interface EntryEditing {
    data object New : EntryEditing
    data class Existing(val entry: WaterEntry) : EntryEditing
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterScreen(
    state: WaterUiState,
    zone: ZoneId,
    onBack: () -> Unit,
    onDayChange: (LocalDate) -> Unit,
    onAdd: (Int, String?, LocalTime) -> Unit,
    onUpdate: (WaterEntry, Int, String?, LocalTime) -> Unit,
    onDelete: (WaterEntry) -> Unit,
    onRestore: (WaterEntry) -> Unit,
    onSaveContainer: (String?, String, Int, Boolean) -> Unit,
    onDeleteContainer: (WaterContainer) -> Unit
) {
    val colors = WellioTheme.colors.water
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<EntryEditing?>(null) }
    var managingContainers by remember { mutableStateOf(false) }

    fun deleteWithUndo(entry: WaterEntry) {
        onDelete(entry)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = "${formatMl(entry.amountMl)} apagado",
                actionLabel = "Desfazer",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onRestore(entry)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Água") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { managingContainers = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Recipientes")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = EntryEditing.New },
                containerColor = colors.accent,
                contentColor = colors.onAccent,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Adicionar") }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .swipeBetweenDays(state.day, state.today, onDayChange)
        ) {
            DayNavigator(state.day, state.today, onDayChange, Modifier.padding(horizontal = 8.dp))
            WaterSummary(state.totalMl, state.goalMl, Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(16.dp))

            if (state.entries.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Nenhum registro neste dia",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.entries.sortedByDescending { it.timestamp }, key = { it.id }) { entry ->
                        WaterEntryRow(
                            entry = entry,
                            zone = zone,
                            onClick = { editing = EntryEditing.Existing(entry) },
                            onDelete = { deleteWithUndo(entry) }
                        )
                    }
                }
            }
        }
    }

    editing?.let { current ->
        val existing = (current as? EntryEditing.Existing)?.entry
        WaterEntrySheet(
            entry = existing,
            containers = state.containers,
            zone = zone,
            onDismiss = { editing = null },
            onSave = { amount, name, time ->
                if (existing == null) onAdd(amount, name, time) else onUpdate(existing, amount, name, time)
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

    if (managingContainers) {
        ContainersSheet(
            containers = state.containers,
            onDismiss = { managingContainers = false },
            onSave = onSaveContainer,
            onDelete = onDeleteContainer
        )
    }
}

@Composable
private fun WaterSummary(totalMl: Int, goalMl: Int, modifier: Modifier = Modifier) {
    val colors = WellioTheme.colors.water
    val fraction = progress(totalMl, goalMl)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Água: ${formatMl(totalMl)} de ${formatMl(goalMl)}, ${(fraction * 100).toInt()} por cento"
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.container, contentColor = colors.onContainer)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(formatMl(totalMl), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("de ${formatMl(goalMl)}", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Text("${(fraction * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.track)
            ) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(colors.accent))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaterEntryRow(
    entry: WaterEntry,
    zone: ZoneId,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()
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
        val time = formatTime(entry.timestamp, zone)
        val label = entry.containerName ?: "Água"
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = "Editar") { onClick() }
                .semantics(mergeDescendants = true) {
                    contentDescription = "$time, $label, ${formatMl(entry.amountMl)}"
                }
        ) {
            Row(
                Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(time, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(formatMl(entry.amountMl), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Apagar registro das $time")
                }
            }
        }
    }
}

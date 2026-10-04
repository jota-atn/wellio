package com.projeto.wellio.ui.water

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.projeto.wellio.domain.goals.progress
import com.projeto.wellio.domain.model.WaterContainer
import com.projeto.wellio.domain.model.WaterEntry
import com.projeto.wellio.domain.water.ContainerIcon
import com.projeto.wellio.ui.components.DayNavigator
import com.projeto.wellio.ui.components.swipeBetweenDays
import com.projeto.wellio.ui.format.formatMl
import com.projeto.wellio.ui.format.formatTime
import com.projeto.wellio.ui.icons.containerIconRes
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
        onQuickAdd = viewModel::quickAdd,
        onUndoQuickAdd = viewModel::undoQuickAdd,
        onUpdate = viewModel::update,
        onDelete = viewModel::delete,
        onRestore = viewModel::restore,
        onSaveContainer = viewModel::saveContainer,
        onDeleteContainer = viewModel::deleteContainer
    )
}

/** O que uma folha está editando: um item novo ou um existente. */
private sealed interface Editing<out T> {
    data object New : Editing<Nothing>
    data class Existing<T>(val item: T) : Editing<T>
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterScreen(
    state: WaterUiState,
    zone: ZoneId,
    onBack: () -> Unit,
    onDayChange: (LocalDate) -> Unit,
    onAdd: (Int, String?, LocalTime) -> Unit,
    onQuickAdd: (WaterContainer) -> Unit,
    onUndoQuickAdd: () -> Unit,
    onUpdate: (WaterEntry, Int, String?, LocalTime) -> Unit,
    onDelete: (WaterEntry) -> Unit,
    onRestore: (WaterEntry) -> Unit,
    onSaveContainer: (id: String?, name: String, amountMl: Int, isQuick: Boolean, iconKey: String) -> Unit,
    onDeleteContainer: (WaterContainer) -> Unit
) {
    val colors = WellioTheme.colors.water
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editingEntry by remember { mutableStateOf<Editing<WaterEntry>?>(null) }
    var editingContainer by remember { mutableStateOf<Editing<WaterContainer>?>(null) }
    var managingContainers by rememberSaveable { mutableStateOf(false) }

    fun showUndo(message: String, onUndo: () -> Unit) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(message, actionLabel = "Desfazer", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }

    fun deleteWithUndo(entry: WaterEntry) {
        onDelete(entry)
        showUndo("${formatMl(entry.amountMl)} apagado") { onRestore(entry) }
    }

    fun drink(container: WaterContainer) {
        onQuickAdd(container)
        showUndo("${container.name} ${formatMl(container.amountMl)} registrado", onUndoQuickAdd)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Água") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingEntry = Editing.New },
                containerColor = colors.accent,
                contentColor = colors.onAccent,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Adicionar") }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .swipeBetweenDays(state.day, state.today, onDayChange),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item(key = "dia") {
                DayNavigator(state.day, state.today, onDayChange, Modifier.padding(horizontal = 8.dp))
            }
            item(key = "gota") {
                WaterHero(state.totalMl, state.goalMl, Modifier.padding(horizontal = 16.dp))
            }
            item(key = "recipientes") {
                ContainersSection(
                    containers = state.containers,
                    editing = managingContainers,
                    onEditingChange = { managingContainers = it },
                    onDrink = ::drink,
                    onEdit = { editingContainer = Editing.Existing(it) },
                    onNew = { editingContainer = Editing.New },
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
            item(key = "titulo-registros") {
                Text(
                    "Registros",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp)
                )
            }
            if (state.entries.isEmpty()) {
                item(key = "vazio") {
                    Text(
                        "Nenhum registro neste dia",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(state.entries.sortedByDescending { it.timestamp }, key = { it.id }) { entry ->
                    WaterEntryRow(
                        entry = entry,
                        iconKey = state.containers.find { it.name == entry.containerName }?.iconKey,
                        zone = zone,
                        onClick = { editingEntry = Editing.Existing(entry) },
                        onDelete = { deleteWithUndo(entry) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).animateItem()
                    )
                }
            }
        }
    }

    editingEntry?.let { current ->
        val existing = (current as? Editing.Existing)?.item
        WaterEntrySheet(
            entry = existing,
            containers = state.containers,
            zone = zone,
            onDismiss = { editingEntry = null },
            onSave = { amount, name, time ->
                if (existing == null) onAdd(amount, name, time) else onUpdate(existing, amount, name, time)
                editingEntry = null
            },
            onDelete = existing?.let { entry ->
                {
                    editingEntry = null
                    deleteWithUndo(entry)
                }
            }
        )
    }

    editingContainer?.let { current ->
        val existing = (current as? Editing.Existing)?.item
        ContainerSheet(
            container = existing,
            onDismiss = { editingContainer = null },
            onSave = { name, amount, isQuick, iconKey ->
                onSaveContainer(existing?.id, name, amount, isQuick, iconKey)
                editingContainer = null
            },
            onDelete = existing?.let { container ->
                {
                    editingContainer = null
                    onDeleteContainer(container)
                }
            }
        )
    }
}

/** Topo da tela: a gota enchendo e os números do dia. */
@Composable
private fun WaterHero(totalMl: Int, goalMl: Int, modifier: Modifier = Modifier) {
    val colors = WellioTheme.colors.water
    val fraction = progress(totalMl, goalMl)
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = colors.container,
        contentColor = colors.onContainer,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Água: ${formatMl(totalMl)} de ${formatMl(goalMl)}, ${(fraction * 100).toInt()} por cento"
            }
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            WaterDrop(fraction, Modifier.size(width = 120.dp, height = 156.dp))
            Spacer(Modifier.width(24.dp))
            Column {
                Text(
                    formatMl(totalMl),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text("de ${formatMl(goalMl)}", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(50), color = colors.accent, contentColor = colors.onAccent) {
                    Text(
                        "${(fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                if (goalMl in 1..<totalMl) {
                    Spacer(Modifier.height(8.dp))
                    Text("+${formatMl(totalMl - goalMl)} além da meta", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaterEntryRow(
    entry: WaterEntry,
    iconKey: String?,
    zone: ZoneId,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WellioTheme.colors.water
    val dismissState = rememberSwipeToDismissBoxState()
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
                Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = colors.container, contentColor = colors.onContainer, modifier = Modifier.size(40.dp)) {
                    Icon(
                        painterResource(containerIconRes(iconKey ?: ContainerIcon.Copo.key)),
                        contentDescription = null,
                        modifier = Modifier.padding(9.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                    Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(formatMl(entry.amountMl), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Apagar registro das $time")
                }
            }
        }
    }
}

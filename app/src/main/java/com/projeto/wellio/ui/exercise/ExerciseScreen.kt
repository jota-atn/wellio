package com.projeto.wellio.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.projeto.wellio.domain.exercise.ExerciseCategory
import com.projeto.wellio.domain.exercise.ExerciseFamily
import com.projeto.wellio.domain.exercise.Intensity
import com.projeto.wellio.domain.exercise.QuickExercise
import com.projeto.wellio.domain.exercise.exerciseTotals
import com.projeto.wellio.domain.exercise.quickExercises
import com.projeto.wellio.domain.goals.progress
import com.projeto.wellio.domain.model.ExerciseEntry
import com.projeto.wellio.domain.model.ExerciseType
import com.projeto.wellio.domain.model.ExerciseUnit
import com.projeto.wellio.ui.components.DayNavigator
import com.projeto.wellio.ui.components.RecordsHeader
import com.projeto.wellio.ui.components.swipeBetweenDays
import com.projeto.wellio.ui.format.formatInt
import com.projeto.wellio.ui.format.formatTime
import com.projeto.wellio.ui.icons.WellioIcons
import com.projeto.wellio.ui.icons.exerciseIconRes
import com.projeto.wellio.ui.navigation.sharedContainer
import com.projeto.wellio.ui.navigation.sharedHero
import com.projeto.wellio.ui.goals.CelebrateWhenGoalReached
import com.projeto.wellio.ui.goals.CelebrationOverlay
import com.projeto.wellio.ui.goals.rememberCelebration
import com.projeto.wellio.ui.theme.DarkWellioColors
import com.projeto.wellio.ui.theme.WellioTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun ExerciseRoute(
    onBack: () -> Unit,
    fromCard: Boolean = false,
    viewModel: ExerciseViewModel = viewModel(factory = ExerciseViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExerciseScreen(
        state = state,
        zone = viewModel.zone,
        fromCard = fromCard,
        onBack = onBack,
        onDayChange = viewModel::selectDay,
        onAdd = viewModel::add,
        onQuickAdd = viewModel::quickAdd,
        onUndoQuickAdd = viewModel::undoQuickAdd,
        onUpdate = viewModel::update,
        onDelete = viewModel::delete,
        onRestore = viewModel::restore,
        onSetGoal = viewModel::setGoal,
        onToggleFavorite = viewModel::toggleFavorite,
        onCreateCustom = viewModel::createCustom,
        onClaimCelebration = viewModel::claimCelebration
    )
}

/** O que o registro está fazendo: novo (vazio ou de um favorito) ou editando um treino. */
private sealed interface Registering {
    data class New(val prefill: ExerciseDraft? = null) : Registering
    data class Edit(val entry: ExerciseEntry) : Registering
}

/**
 * Tela de Exercício (SPEC §8.0): fogueira com o progresso do dia, fileira de favoritos
 * (tocar registra, segurar abre preenchido), treinos do dia recolhidos e navegação entre
 * dias. Aberta pelo card ([fromCard]), o card cresce e vira esta tela.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreen(
    state: ExerciseUiState,
    zone: ZoneId,
    onBack: () -> Unit,
    onDayChange: (LocalDate) -> Unit,
    onAdd: (ExerciseDraft) -> Unit,
    onQuickAdd: (QuickExercise) -> Unit,
    onUndoQuickAdd: () -> Unit,
    onUpdate: (ExerciseEntry, ExerciseDraft) -> Unit,
    onDelete: (ExerciseEntry) -> Unit,
    onRestore: (ExerciseEntry) -> Unit,
    onSetGoal: (ExerciseUnit, Int) -> Unit,
    onToggleFavorite: (ExerciseFamily) -> Unit,
    onCreateCustom: (String, ExerciseCategory, Intensity, Double?, (ExerciseType) -> Unit) -> Unit,
    fromCard: Boolean = false,
    onClaimCelebration: (onCelebrate: () -> Unit) -> Unit = {}
) {
    val colors = WellioTheme.colors.exercise
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var registering by remember { mutableStateOf<Registering?>(null) }
    var editingGoal by remember { mutableStateOf(false) }
    var showingEntries by rememberSaveable { mutableStateOf(false) }
    // Registros feitos aqui: só eles avivam a fogueira (e não abrir a tela ou trocar de dia)
    var additions by rememberSaveable { mutableIntStateOf(0) }
    val typesById = remember(state.types) { state.types.associateBy { it.id } }
    val quick = remember(state.types, state.uses) { quickExercises(state.types, state.uses) }

    fun showUndo(message: String, onUndo: () -> Unit) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(message, actionLabel = "Desfazer", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }

    fun deleteWithUndo(entry: ExerciseEntry) {
        onDelete(entry)
        showUndo("${typesById[entry.typeId]?.name ?: "Treino"} apagado") { onRestore(entry) }
    }

    fun train(item: QuickExercise) {
        additions++
        onQuickAdd(item)
        showUndo("${item.type.name} · ${formatDuration(item.minutes)} registrado", onUndoQuickAdd)
    }

    // Bater a meta registrando aqui: a fogueira vira labareda e a festa vem por cima (SPEC §9.2)
    val celebration = rememberCelebration()
    val dayTotals = exerciseTotals(state.entries)
    CelebrateWhenGoalReached(
        total = if (state.goals.exerciseUnit == ExerciseUnit.Minutes) dayTotals.minutes else dayTotals.kcal,
        goal = state.goals.exercise.daily,
        additions = additions,
        isToday = state.day == state.today
    ) {
        onClaimCelebration { celebration.celebrate("Meta de exercício batida!", DarkWellioColors.exercise.accent) }
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.sharedContainer("card-exercise", RoundedCornerShape(28.dp), enabled = fromCard, screen = true),
        topBar = {
            TopAppBar(
                title = { Text("Exercício") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { registering = Registering.New() },
                containerColor = colors.accent,
                contentColor = colors.onAccent,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Registrar") }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val totals = exerciseTotals(state.entries)
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
            item(key = "fogueira") {
                ExerciseHero(
                    kcal = totals.kcal,
                    minutes = totals.minutes,
                    unit = state.goals.exerciseUnit,
                    goal = state.goals.exercise.daily,
                    additions = additions,
                    fromCard = fromCard,
                    onEditGoal = { editingGoal = true },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item(key = "favoritos") {
                QuickRow(
                    items = quick,
                    canUndo = state.entries.isNotEmpty(),
                    onTrain = ::train,
                    onPrefill = { registering = Registering.New(ExerciseDraft(it.type, it.minutes, null, LocalTime.now(zone))) },
                    onNew = { registering = Registering.New() },
                    onUndoLast = { state.entries.maxByOrNull { it.timestamp }?.let(::deleteWithUndo) },
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
            item(key = "titulo-treinos") {
                RecordsHeader(
                    title = "Treinos",
                    count = state.entries.size,
                    expanded = showingEntries,
                    onToggle = { showingEntries = !showingEntries },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            if (showingEntries && state.entries.isEmpty()) {
                item(key = "vazio") {
                    Text(
                        "Nenhum treino neste dia",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    )
                }
            } else if (showingEntries) {
                items(state.entries.sortedByDescending { it.timestamp }, key = { it.id }) { entry ->
                    EntryRow(
                        entry = entry,
                        type = typesById[entry.typeId],
                        zone = zone,
                        onClick = { registering = Registering.Edit(entry) },
                        onDelete = { deleteWithUndo(entry) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).animateItem()
                    )
                }
            }
        }
    }
    CelebrationOverlay(celebration)
    }

    registering?.let { current ->
        val editingEntry = (current as? Registering.Edit)?.entry
        val initial = when (current) {
            is Registering.New -> current.prefill
            is Registering.Edit -> typesById[current.entry.typeId]?.let { type ->
                ExerciseDraft(
                    type = type,
                    minutes = current.entry.minutes,
                    manualKcal = current.entry.kcal.takeIf { current.entry.kcalIsManual },
                    time = current.entry.timestamp.atZone(zone).toLocalTime()
                )
            }
        }
        ExerciseSheet(
            types = state.types,
            uses = state.uses,
            weightKg = state.weightKg,
            initial = initial,
            editing = editingEntry != null,
            defaultTime = LocalTime.now(zone),
            onDismiss = { registering = null },
            onSave = { draft ->
                if (editingEntry == null) {
                    additions++
                    onAdd(draft)
                } else {
                    onUpdate(editingEntry, draft)
                }
                registering = null
            },
            onDelete = editingEntry?.let { entry ->
                {
                    registering = null
                    deleteWithUndo(entry)
                }
            },
            onToggleFavorite = onToggleFavorite,
            onCreateCustom = onCreateCustom
        )
    }

    if (editingGoal) {
        ExerciseGoalSheet(
            unit = state.goals.exerciseUnit,
            daily = state.goals.exercise.daily,
            onDismiss = { editingGoal = false },
            onSave = { unit, daily ->
                onSetGoal(unit, daily)
                editingGoal = false
            }
        )
    }
}

/** Topo: fogueira e o progresso na unidade da meta, com o outro valor menor. */
@Composable
private fun ExerciseHero(
    kcal: Int,
    minutes: Int,
    unit: ExerciseUnit,
    goal: Int,
    additions: Int,
    fromCard: Boolean,
    onEditGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WellioTheme.colors.exercise
    val value = if (unit == ExerciseUnit.Minutes) minutes else kcal
    val fraction = progress(value, goal)
    val main = if (unit == ExerciseUnit.Minutes) formatDuration(minutes) else "${formatInt(kcal)} kcal"
    val other = if (unit == ExerciseUnit.Minutes) "${formatInt(kcal)} kcal" else formatDuration(minutes)
    val goalText = if (unit == ExerciseUnit.Minutes) formatDuration(goal) else "${formatInt(goal)} kcal"
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = colors.container,
        contentColor = colors.onContainer,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Exercício: $main de $goalText, ${(fraction * 100).toInt()} por cento. $other"
            }
    ) {
        Row(Modifier.padding(start = 8.dp, end = 20.dp, top = 12.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Campfire(value, goal, additions, Modifier.sharedHero("hero-exercise", enabled = fromCard).size(width = 136.dp, height = 168.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(main, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Row(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClickLabel = "Mudar a meta", onClick = onEditGoal),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("de $goalText", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .width(140.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.track)
                ) {
                    Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(colors.accent))
                }
                Spacer(Modifier.height(8.dp))
                Text(other, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Fileira de favoritos e recentes: tocar registra, segurar abre preenchido, + abre vazio. */
@Composable
private fun QuickRow(
    items: List<QuickExercise>,
    canUndo: Boolean,
    onTrain: (QuickExercise) -> Unit,
    onPrefill: (QuickExercise) -> Unit,
    onNew: () -> Unit,
    onUndoLast: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WellioTheme.colors.exercise
    // A tela abre com a fileira vazia (só o +) e os treinos chegam depois: sem isto, a fileira
    // seguiria o + até o fim. Volta ao começo quando chegam e quando outro treino vai para a frente.
    val rowState = rememberLazyListState()
    LaunchedEffect(items.firstOrNull()?.type?.id) { rowState.scrollToItem(0) }
    Column(modifier) {
        Row(Modifier.padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Treinar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = onUndoLast, enabled = canUndo) {
                Icon(WellioIcons.Undo, contentDescription = "Desfazer último", tint = if (canUndo) colors.accent else LocalContentColor.current.copy(alpha = 0.38f))
            }
        }
        LazyRow(state = rowState, contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.type.id }) { item ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.container,
                    contentColor = colors.onContainer,
                    modifier = Modifier
                        .size(width = 104.dp, height = 112.dp)
                        .combinedClickable(
                            onClickLabel = "Registrar",
                            onLongClickLabel = "Ajustar antes de registrar",
                            onClick = { onTrain(item) },
                            onLongClick = { onPrefill(item) }
                        )
                        .semantics(mergeDescendants = true) {
                            contentDescription = "${item.type.name}, ${formatDuration(item.minutes)}"
                        }
                ) {
                    Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(painterResource(exerciseIconRes(item.type)), contentDescription = null, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(item.type.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatDuration(item.minutes), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            item(key = "novo") {
                OutlinedCard(
                    onClick = onNew,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .size(width = 64.dp, height = 112.dp)
                        .semantics { contentDescription = "Registrar outro exercício" }
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = colors.accent)
                    }
                }
            }
        }
        if (items.isEmpty()) {
            Text(
                "Seus favoritos (★) e os treinos que você mais faz aparecem aqui.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryRow(
    entry: ExerciseEntry,
    type: ExerciseType?,
    zone: ZoneId,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WellioTheme.colors.exercise
    val dismissState = rememberSwipeToDismissBoxState()
    val time = formatTime(entry.timestamp, zone)
    val name = type?.name ?: "Treino"
    val level = type?.level?.label?.lowercase()
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
                    contentDescription = listOfNotNull(time, name, level, formatDuration(entry.minutes), "${entry.kcal} quilocalorias")
                        .joinToString(", ")
                }
        ) {
            Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = colors.container, contentColor = colors.onContainer, modifier = Modifier.size(40.dp)) {
                    type?.let { Icon(painterResource(exerciseIconRes(it)), contentDescription = null, modifier = Modifier.padding(9.dp)) }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(listOfNotNull(name, level).joinToString(" · "), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "$time · ${formatDuration(entry.minutes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${formatInt(entry.kcal)} kcal", style = MaterialTheme.typography.titleSmall)
                    if (entry.kcalIsManual) {
                        Text("do relógio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Apagar treino das $time")
                }
            }
        }
    }
}

/** Meta diária de exercício: kcal ou minutos (SPEC §8.0). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ExerciseGoalSheet(unit: ExerciseUnit, daily: Int, onDismiss: () -> Unit, onSave: (ExerciseUnit, Int) -> Unit) {
    val colors = WellioTheme.colors.exercise
    var chosen by remember { mutableStateOf(unit) }
    var text by remember { mutableStateOf(daily.toString()) }
    val presets = if (chosen == ExerciseUnit.Minutes) listOf(20, 30, 45, 60) else listOf(300, 400, 500, 700)
    val value = text.toIntOrNull()?.takeIf { it in 1..if (chosen == ExerciseUnit.Minutes) 300 else 5000 }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("Meta diária de exercício", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ExerciseUnit.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = chosen == option,
                        onClick = {
                            if (chosen != option) text = option.defaultDaily.toString()
                            chosen = option
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, ExerciseUnit.entries.size),
                        label = { Text(if (option == ExerciseUnit.Kcal) "kcal queimadas" else "minutos ativos") }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(4) },
                label = { Text(if (chosen == ExerciseUnit.Minutes) "Meta (min)" else "Meta (kcal)") },
                isError = value == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { preset ->
                    SuggestionChip(onClick = { text = preset.toString() }, label = { Text(if (chosen == ExerciseUnit.Minutes) "$preset min" else "$preset kcal") })
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { value?.let { onSave(chosen, it) } },
                enabled = value != null,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                modifier = Modifier.align(Alignment.End)
            ) { Text("Salvar") }
        }
    }
}

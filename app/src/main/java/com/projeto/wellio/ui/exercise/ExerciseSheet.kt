package com.projeto.wellio.ui.exercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.projeto.wellio.domain.calories.exerciseKcal
import com.projeto.wellio.domain.exercise.CustomExerciseValidation
import com.projeto.wellio.domain.exercise.DEFAULT_EXERCISE_MINUTES
import com.projeto.wellio.domain.exercise.ExerciseCategory
import com.projeto.wellio.domain.exercise.Intensity
import com.projeto.wellio.domain.exercise.parseKcal
import com.projeto.wellio.domain.exercise.recentExercises
import com.projeto.wellio.domain.exercise.searchExercises
import com.projeto.wellio.domain.exercise.validateCustomExercise
import com.projeto.wellio.domain.model.ExerciseType
import com.projeto.wellio.domain.model.ExerciseUse
import com.projeto.wellio.ui.components.TimePickerDialog
import com.projeto.wellio.ui.format.formatInt
import com.projeto.wellio.ui.icons.exerciseCategoryIconRes
import com.projeto.wellio.ui.icons.exerciseIconRes
import com.projeto.wellio.ui.theme.WellioTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Registro de exercício (SPEC §8.0), em tela cheia. Sem [initial], começa escolhendo a
 * atividade (busca, recentes, categorias, ★ favorito, criar atividade); com [initial]
 * (editar um treino ou um favorito segurado), já abre nos detalhes: mostrador de duração,
 * kcal ao vivo (tocar permite digitar a do relógio) e horário.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSheet(
    types: List<ExerciseType>,
    uses: List<ExerciseUse>,
    weightKg: Double,
    initial: ExerciseDraft?,
    editing: Boolean,
    defaultTime: LocalTime,
    onDismiss: () -> Unit,
    onSave: (ExerciseDraft) -> Unit,
    onDelete: (() -> Unit)?,
    onToggleFavorite: (ExerciseType) -> Unit,
    onCreateCustom: (String, ExerciseCategory, Intensity, Double?, (ExerciseType) -> Unit) -> Unit
) {
    var selected by remember { mutableStateOf(initial?.type) }
    var minutes by remember { mutableIntStateOf(initial?.minutes ?: DEFAULT_EXERCISE_MINUTES) }
    var manualKcal by remember { mutableStateOf(initial?.manualKcal) }
    var time by remember { mutableStateOf(initial?.time ?: defaultTime) }
    var creating by remember { mutableStateOf(false) }
    // A atividade pode ter mudado no banco (ex.: virou favorita): usa a versão atual
    val current = selected?.let { s -> types.firstOrNull { it.id == s.id } ?: s }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(0.94f)) {
            if (current == null) {
                ActivityPicker(
                    types = types,
                    uses = uses,
                    onPick = { type ->
                        selected = type
                        minutes = uses.firstOrNull { it.typeId == type.id }?.minutes ?: DEFAULT_EXERCISE_MINUTES
                        manualKcal = null
                    },
                    onToggleFavorite = onToggleFavorite,
                    onCreate = { creating = true }
                )
            } else {
                ExerciseDetails(
                    type = current,
                    minutes = minutes,
                    onMinutesChange = { minutes = it },
                    estimatedKcal = exerciseKcal(current, weightKg, minutes),
                    manualKcal = manualKcal,
                    onManualKcalChange = { manualKcal = it },
                    time = time,
                    onTimeChange = { time = it },
                    title = if (editing) "Editar treino" else "Registrar exercício",
                    onChangeActivity = { selected = null },
                    onToggleFavorite = { onToggleFavorite(current) },
                    onSave = { onSave(ExerciseDraft(current, minutes, manualKcal, time)) },
                    onDelete = onDelete
                )
            }
        }
    }

    if (creating) {
        CreateExerciseDialog(
            onDismiss = { creating = false },
            onCreate = { name, category, intensity, kcalPerMinute ->
                creating = false
                onCreateCustom(name, category, intensity, kcalPerMinute) { created ->
                    selected = created
                    minutes = DEFAULT_EXERCISE_MINUTES
                    manualKcal = null
                }
            }
        )
    }
}

/** Passo 1: escolher a atividade. */
@Composable
private fun ActivityPicker(
    types: List<ExerciseType>,
    uses: List<ExerciseUse>,
    onPick: (ExerciseType) -> Unit,
    onToggleFavorite: (ExerciseType) -> Unit,
    onCreate: () -> Unit
) {
    val colors = WellioTheme.colors.exercise
    var query by rememberSaveable { mutableStateOf("") }
    var category by remember { mutableStateOf<ExerciseCategory?>(null) }
    val results = remember(types, query, category) { searchExercises(types, query, category) }
    val recents = remember(types, uses) { recentExercises(types, uses) }

    Text("Registrar exercício", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp))
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text("Buscar atividade") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    )
    if (query.isBlank() && recents.isNotEmpty()) {
        Text("Recentes", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 6.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(recents, key = { it.id }) { type ->
                AssistChip(
                    onClick = { onPick(type) },
                    label = { Text(type.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { Icon(painterResource(exerciseIconRes(type)), contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.widthIn(max = 220.dp)
                )
            }
        }
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(selected = category == null, onClick = { category = null }, label = { Text("Todas") })
        }
        items(ExerciseCategory.entries) { option ->
            FilterChip(
                selected = category == option,
                onClick = { category = if (category == option) null else option },
                label = { Text(option.label) },
                leadingIcon = { Icon(painterResource(exerciseCategoryIconRes(option)), contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.container, selectedLabelColor = colors.onContainer, selectedLeadingIconColor = colors.onContainer)
            )
        }
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        items(results, key = { it.id }) { type ->
            ActivityRow(type, onPick = { onPick(type) }, onToggleFavorite = { onToggleFavorite(type) })
        }
        item(key = "criar") {
            TextButton(onClick = onCreate, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (results.isEmpty()) "Não achou? Criar atividade" else "Criar atividade")
            }
        }
    }
}

@Composable
private fun ActivityRow(type: ExerciseType, onPick: () -> Unit, onToggleFavorite: () -> Unit) {
    val colors = WellioTheme.colors.exercise
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onPick)
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = colors.container, contentColor = colors.onContainer, modifier = Modifier.size(40.dp)) {
            Icon(painterResource(exerciseIconRes(type)), contentDescription = null, modifier = Modifier.padding(9.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(type.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                type.kcalPerMinute?.let { "${formatInt(it.toInt())} kcal/min · própria" }
                    ?: ("${type.mets.toString().replace('.', ',')} MET" + if (type.isCustom) " · própria" else ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                Icons.Default.Star,
                contentDescription = if (type.isFavorite) "Tirar ${type.name} dos favoritos" else "Favoritar ${type.name}",
                tint = if (type.isFavorite) colors.accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}

/** Passo 2: duração no mostrador, kcal e horário. */
@Composable
private fun ExerciseDetails(
    type: ExerciseType,
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    estimatedKcal: Int,
    manualKcal: Int?,
    onManualKcalChange: (Int?) -> Unit,
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    title: String,
    onChangeActivity: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val colors = WellioTheme.colors.exercise
    var typingKcal by remember { mutableStateOf(false) }
    var kcalText by remember { mutableStateOf("") }
    var pickingTime by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Surface(shape = CircleShape, color = colors.accent, contentColor = colors.onAccent, modifier = Modifier.size(44.dp)) {
                Icon(painterResource(exerciseIconRes(type)), contentDescription = null, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(type.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = if (type.isFavorite) "Tirar dos favoritos" else "Favoritar",
                    tint = if (type.isFavorite) colors.accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
            TextButton(onClick = onChangeActivity) { Text("Trocar") }
        }
        Spacer(Modifier.height(8.dp))
        ExerciseDial(
            minutes = minutes,
            onMinutesChange = onMinutesChange,
            accent = colors.accent,
            track = colors.track,
            modifier = Modifier.size(260.dp)
        )
        Spacer(Modifier.height(16.dp))

        // kcal: estimativa ao vivo; tocar permite digitar a do relógio
        if (typingKcal) {
            val parsed = parseKcal(kcalText)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = kcalText,
                    onValueChange = { kcalText = it.filter(Char::isDigit).take(4) },
                    label = { Text("kcal do relógio") },
                    isError = kcalText.isNotEmpty() && parsed == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.width(180.dp)
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    onManualKcalChange(parsed)
                    typingKcal = false
                }, enabled = kcalText.isEmpty() || parsed != null) { Text("OK") }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = {
                        kcalText = (manualKcal ?: estimatedKcal).toString()
                        typingKcal = true
                    },
                    shape = RoundedCornerShape(50),
                    color = colors.container,
                    contentColor = colors.onContainer,
                    modifier = Modifier.semantics {
                        contentDescription = if (manualKcal != null) "$manualKcal quilocalorias do relógio. Tocar para mudar"
                        else "Cerca de $estimatedKcal quilocalorias. Tocar para digitar as do relógio"
                    }
                ) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.Bottom) {
                        Text(
                            (if (manualKcal == null) "≈ " else "") + formatInt(manualKcal ?: estimatedKcal),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (manualKcal == null) "kcal" else "kcal · do relógio", style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (manualKcal != null) {
                    IconButton(onClick = { onManualKcalChange(null) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Voltar para a estimativa")
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { pickingTime = true }) {
            Text("Horário: ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}")
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onDelete != null) {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Apagar")
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onSave,
                enabled = minutes > 0,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent)
            ) { Text("Salvar") }
        }
    }

    if (pickingTime) {
        TimePickerDialog(time, onDismiss = { pickingTime = false }, onConfirm = {
            onTimeChange(it)
            pickingTime = false
        })
    }
}

/** Criar atividade própria: nome, categoria, intensidade e, se quiser, kcal por minuto. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CreateExerciseDialog(
    onDismiss: () -> Unit,
    onCreate: (String, ExerciseCategory, Intensity, Double?) -> Unit
) {
    val colors = WellioTheme.colors.exercise
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExerciseCategory.Other) }
    var intensity by remember { mutableStateOf(Intensity.Moderate) }
    var kcalPerMinute by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Criar atividade") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, label = { Text("Nome") }, singleLine = true)
                Text("Categoria", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExerciseCategory.entries.forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text(option.label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.container, selectedLabelColor = colors.onContainer)
                        )
                    }
                }
                Text("Intensidade", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow {
                    Intensity.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = intensity == option,
                            onClick = { intensity = option },
                            shape = SegmentedButtonDefaults.itemShape(index, Intensity.entries.size),
                            label = { Text(option.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = kcalPerMinute,
                    onValueChange = { kcalPerMinute = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(5) },
                    label = { Text("kcal por minuto (opcional)") },
                    supportingText = { Text("Se souber, vale no lugar da intensidade") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (val result = validateCustomExercise(name, kcalPerMinute)) {
                    is CustomExerciseValidation.Valid -> onCreate(result.name, category, intensity, result.kcalPerMinute)
                    CustomExerciseValidation.BlankName -> error = "Dê um nome à atividade"
                    CustomExerciseValidation.InvalidKcalPerMinute -> error = "kcal por minuto entre 0 e 30"
                }
            }) { Text("Criar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

package com.projeto.wellio.ui.water

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.projeto.wellio.domain.model.WaterContainer
import com.projeto.wellio.domain.model.WaterEntry
import com.projeto.wellio.domain.water.ContainerIcon
import com.projeto.wellio.domain.water.ContainerValidation
import com.projeto.wellio.domain.water.MAX_QUICK_BUTTONS
import com.projeto.wellio.domain.water.MAX_WATER_AMOUNT_ML
import com.projeto.wellio.domain.water.MAX_WATER_GOAL_ML
import com.projeto.wellio.domain.water.MIN_WATER_GOAL_ML
import com.projeto.wellio.domain.water.parseWaterGoal
import com.projeto.wellio.domain.water.suggestedWaterGoal
import com.projeto.wellio.ui.format.formatKg
import com.projeto.wellio.domain.water.parseWaterAmount
import com.projeto.wellio.domain.water.suggestedIcon
import com.projeto.wellio.domain.water.validateContainer
import com.projeto.wellio.ui.format.formatMl
import com.projeto.wellio.ui.icons.containerIconRes
import com.projeto.wellio.ui.theme.WellioTheme
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Registrar água num dia ([entry] = null) ou editar um registro existente. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterEntrySheet(
    entry: WaterEntry?,
    containers: List<WaterContainer>,
    zone: ZoneId,
    onDismiss: () -> Unit,
    onSave: (Int, String?, LocalTime) -> Unit,
    onDelete: (() -> Unit)?
) {
    val colors = WellioTheme.colors.water
    var amountText by remember { mutableStateOf(entry?.amountMl?.toString().orEmpty()) }
    var containerName by remember { mutableStateOf(entry?.containerName) }
    var time by remember {
        mutableStateOf(entry?.timestamp?.atZone(zone)?.toLocalTime() ?: LocalTime.now(zone))
    }
    var pickingTime by remember { mutableStateOf(false) }
    val amount = parseWaterAmount(amountText)

    // Abre inteiro e rola: com o teclado aberto, o Salvar continua alcançável
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text(
                if (entry == null) "Registrar água" else "Editar registro",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(containers, key = { it.id }) { container ->
                    FilterChip(
                        selected = containerName == container.name && amount == container.amountMl,
                        onClick = {
                            containerName = container.name
                            amountText = container.amountMl.toString()
                        },
                        label = { Text("${container.name} ${formatMl(container.amountMl)}") },
                        leadingIcon = {
                            Icon(
                                painterResource(containerIconRes(container.iconKey)),
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.container,
                            selectedLabelColor = colors.onContainer
                        )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { text ->
                    amountText = text.filter(Char::isDigit).take(5)
                    // Valor digitado à mão deixa de ser de um recipiente
                    if (containers.none { it.name == containerName && it.amountMl.toString() == amountText }) {
                        containerName = null
                    }
                },
                label = { Text("Quantidade (ml)") },
                isError = amountText.isNotEmpty() && amount == null,
                supportingText = {
                    if (amountText.isNotEmpty() && amount == null) Text("Entre 1 e ${formatMl(MAX_WATER_AMOUNT_ML)}")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(onClick = { pickingTime = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Horário: ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}")
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
                    onClick = { amount?.let { onSave(it, containerName, time) } },
                    enabled = amount != null,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent)
                ) { Text("Salvar") }
            }
        }
    }

    if (pickingTime) {
        TimePickerDialog(
            initial = time,
            onDismiss = { pickingTime = false },
            onConfirm = {
                time = it
                pickingTime = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        text = { TimePicker(state = state) }
    )
}

/** Criar ([container] = null) ou editar um recipiente: ícone, nome, volume e se vira botão no card. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerSheet(
    container: WaterContainer?,
    onDismiss: () -> Unit,
    onSave: (name: String, amountMl: Int, isQuick: Boolean, iconKey: String) -> Unit,
    onDelete: (() -> Unit)?
) {
    val colors = WellioTheme.colors.water
    var name by remember { mutableStateOf(container?.name.orEmpty()) }
    var amountText by remember { mutableStateOf(container?.amountMl?.toString().orEmpty()) }
    var isQuick by remember { mutableStateOf(container?.isQuick ?: false) }
    // Recipiente novo segue o ícone sugerido pelo volume até o usuário escolher um
    var pickedIcon by remember { mutableStateOf(container?.let { ContainerIcon.fromKey(it.iconKey) }) }
    val icon = pickedIcon ?: suggestedIcon(parseWaterAmount(amountText) ?: 0)
    var error by remember { mutableStateOf<String?>(null) }

    // Abre inteiro e rola: com o teclado aberto, o Salvar continua alcançável
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text(
                if (container == null) "Novo recipiente" else "Editar recipiente",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ContainerIcon.entries) { option ->
                    val selected = option == icon
                    Surface(
                        onClick = { pickedIcon = option },
                        shape = CircleShape,
                        color = if (selected) colors.accent else colors.container,
                        contentColor = if (selected) colors.onAccent else colors.onContainer,
                        modifier = Modifier
                            .size(52.dp)
                            .semantics {
                                contentDescription = "Ícone ${option.label}"
                                this.selected = selected
                            }
                    ) {
                        Icon(
                            painterResource(containerIconRes(option.key)),
                            contentDescription = null,
                            modifier = Modifier.padding(13.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(20) },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.weight(1.4f)
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter(Char::isDigit).take(4) },
                    label = { Text("ml") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Botão no card da Home", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Aparecem até $MAX_QUICK_BUTTONS",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isQuick,
                    onCheckedChange = { isQuick = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = colors.accent, checkedThumbColor = colors.onAccent)
                )
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
                    onClick = {
                        when (val result = validateContainer(name, amountText)) {
                            is ContainerValidation.Valid -> onSave(result.name, result.amountMl, isQuick, icon.key)
                            ContainerValidation.BlankName -> error = "Dê um nome ao recipiente"
                            ContainerValidation.InvalidAmount -> error = "Quantidade entre 1 e ${formatMl(MAX_WATER_AMOUNT_ML)}"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent)
                ) { Text("Salvar") }
            }
        }
    }
}

/** Meta diária de água, com sugestão de 35 ml por kg do peso atual. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WaterGoalSheet(
    currentMl: Int,
    weightKg: Double,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    val colors = WellioTheme.colors.water
    val suggested = suggestedWaterGoal(weightKg)
    var text by remember { mutableStateOf(currentMl.toString()) }
    val goal = parseWaterGoal(text)

    // Abre inteiro e rola: com o teclado aberto, o Salvar continua alcançável
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("Meta diária de água", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(4) },
                label = { Text("Meta (ml)") },
                isError = goal == null,
                supportingText = {
                    if (goal == null) Text("Entre ${formatMl(MIN_WATER_GOAL_ML)} e ${formatMl(MAX_WATER_GOAL_ML)}")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(
                    onClick = { text = suggested.toString() },
                    label = { Text("Sugestão para ${formatKg(weightKg)}: ${formatMl(suggested)}") },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = colors.container,
                        labelColor = colors.onContainer
                    )
                )
                listOf(1500, 2000, 2500, 3000).filter { it != suggested }.forEach { ml ->
                    SuggestionChip(onClick = { text = ml.toString() }, label = { Text(formatMl(ml)) })
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { goal?.let(onSave) },
                enabled = goal != null,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                modifier = Modifier.align(Alignment.End)
            ) { Text("Salvar") }
        }
    }
}

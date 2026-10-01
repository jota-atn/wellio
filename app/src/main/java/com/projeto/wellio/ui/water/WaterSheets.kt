package com.projeto.wellio.ui.water

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.projeto.wellio.domain.model.WaterContainer
import com.projeto.wellio.domain.model.WaterEntry
import com.projeto.wellio.domain.water.ContainerValidation
import com.projeto.wellio.domain.water.MAX_WATER_AMOUNT_ML
import com.projeto.wellio.domain.water.parseWaterAmount
import com.projeto.wellio.domain.water.validateContainer
import com.projeto.wellio.ui.format.formatMl
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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
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

/** Criar, editar e remover recipientes, e escolher quais viram botões rápidos no card. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainersSheet(
    containers: List<WaterContainer>,
    onDismiss: () -> Unit,
    onSave: (id: String?, name: String, amountMl: Int, isQuick: Boolean) -> Unit,
    onDelete: (WaterContainer) -> Unit
) {
    val colors = WellioTheme.colors.water
    var editingId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun clearForm() {
        editingId = null
        name = ""
        amountText = ""
        error = null
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("Recipientes", style = MaterialTheme.typography.titleLarge)
            Text(
                "Os marcados como rápidos viram botões no card de Água (até 3).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            containers.forEach { container ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(container.name, style = MaterialTheme.typography.bodyLarge)
                        Text(formatMl(container.amountMl), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Rápido", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(4.dp))
                    Switch(
                        checked = container.isQuick,
                        onCheckedChange = { onSave(container.id, container.name, container.amountMl, it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.accent, checkedThumbColor = colors.onAccent)
                    )
                    IconButton(onClick = {
                        editingId = container.id
                        name = container.name
                        amountText = container.amountMl.toString()
                        error = null
                    }) { Icon(Icons.Default.Edit, contentDescription = "Editar ${container.name}") }
                    IconButton(onClick = { onDelete(container) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remover ${container.name}")
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text(
                if (editingId == null) "Novo recipiente" else "Editar recipiente",
                style = MaterialTheme.typography.titleMedium
            )
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
            Row {
                if (editingId != null) TextButton(onClick = ::clearForm) { Text("Cancelar") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        when (val result = validateContainer(name, amountText)) {
                            is ContainerValidation.Valid -> {
                                val isQuick = containers.find { it.id == editingId }?.isQuick ?: false
                                onSave(editingId, result.name, result.amountMl, isQuick)
                                clearForm()
                            }
                            ContainerValidation.BlankName -> error = "Dê um nome ao recipiente"
                            ContainerValidation.InvalidAmount -> error = "Quantidade entre 1 e ${formatMl(MAX_WATER_AMOUNT_ML)}"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent)
                ) { Text(if (editingId == null) "Adicionar" else "Salvar") }
            }
        }
    }
}

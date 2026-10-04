package com.projeto.wellio.ui.weight

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.projeto.wellio.domain.model.WeightEntry
import com.projeto.wellio.domain.weight.MAX_WEIGHT_KG
import com.projeto.wellio.domain.weight.MIN_WEIGHT_KG
import com.projeto.wellio.domain.weight.parseWeight
import com.projeto.wellio.ui.components.DayPickerDialog
import com.projeto.wellio.ui.format.dayLabel
import com.projeto.wellio.ui.format.formatKg
import com.projeto.wellio.ui.icons.WellioIcons
import com.projeto.wellio.ui.theme.WellioTheme
import java.time.LocalDate
import kotlin.math.roundToInt

/** "72,4" para os campos de texto. */
private fun kgText(kg: Double): String = formatKg(kg).removeSuffix(" kg")

/** Estado de um peso sendo escolhido: o texto é a fonte, a régua e os botões escrevem nele. */
@Stable
class WeightPickerState(initialKg: Double) {
    var text by mutableStateOf(kgText(initialKg))
    var lastValid by mutableDoubleStateOf(initialKg)
        private set

    /** Peso válido do texto, ou null se estiver fora da faixa. */
    val weight: Double? get() = parseWeight(text)

    /** O que a régua e o número grande mostram: o peso válido, ou o último que foi válido. */
    val shown: Double get() = weight ?: lastValid

    fun set(kg: Double) {
        text = kgText(kg)
        lastValid = kg
    }

    fun type(newText: String) {
        text = newText.filter { it.isDigit() || it == ',' || it == '.' }.take(6)
        weight?.let { lastValid = it }
    }

    fun step(deltaKg: Double) {
        val next = ((shown + deltaKg) * 10).roundToInt() / 10.0
        if (next in MIN_WEIGHT_KG..MAX_WEIGHT_KG) set(next)
    }
}

/** Número grande com −/+, régua de balança e a opção de digitar. */
@Composable
fun WeightPicker(state: WeightPickerState, modifier: Modifier = Modifier) {
    var typing by remember { mutableStateOf(false) }
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(onClick = { state.step(-0.1) }) {
                Icon(WellioIcons.Minus, contentDescription = "Diminuir 0,1 kg")
            }
            Text(
                formatKg(state.shown),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            FilledTonalIconButton(onClick = { state.step(0.1) }) {
                Icon(Icons.Default.Add, contentDescription = "Aumentar 0,1 kg")
            }
        }
        WeightRuler(
            value = state.shown,
            onValueChange = state::set,
            tick = MaterialTheme.colorScheme.onSurfaceVariant,
            indicator = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        if (typing) {
            OutlinedTextField(
                value = state.text,
                onValueChange = state::type,
                label = { Text("Peso (kg)") },
                isError = state.weight == null,
                supportingText = {
                    if (state.weight == null) Text("Entre ${MIN_WEIGHT_KG.toInt()} e ${MAX_WEIGHT_KG.toInt()} kg")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
        } else {
            TextButton(onClick = { typing = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Digitar o peso")
            }
        }
    }
}

/** Registrar uma pesagem ([entry] = null) ou editar uma existente. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightSheet(
    entry: WeightEntry?,
    suggestedKg: Double,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (LocalDate, Double) -> Unit,
    onDelete: (() -> Unit)?
) {
    val colors = WellioTheme.colors.base
    val picker = remember { WeightPickerState(entry?.weightKg ?: suggestedKg) }
    var day by remember { mutableStateOf(entry?.date ?: today) }
    var pickingDay by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text(
                if (entry == null) "Registrar peso" else "Editar pesagem",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(8.dp))
            WeightPicker(picker)
            Column(Modifier.padding(horizontal = 24.dp)) {
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
                        onClick = { picker.weight?.let { onSave(day, it) } },
                        enabled = picker.weight != null,
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

/** Definir, mudar ou remover o peso alvo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetSheet(
    targetKg: Double?,
    currentKg: Double,
    startKg: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onRemove: (() -> Unit)?
) {
    val colors = WellioTheme.colors.base
    val picker = remember { WeightPickerState(targetKg ?: currentKg) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text("Peso alvo", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp))
            Text(
                if (targetKg == null) "A barra de progresso começa no seu peso de agora (${formatKg(currentKg)})."
                else "A barra de progresso continua contando desde ${formatKg(startKg)}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(8.dp))
            WeightPicker(picker)
            Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onRemove != null) {
                    TextButton(onClick = onRemove) { Text("Remover alvo") }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { picker.weight?.let(onSave) },
                    enabled = picker.weight != null,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.container, contentColor = colors.onContainer)
                ) { Text("Salvar") }
            }
        }
    }
}

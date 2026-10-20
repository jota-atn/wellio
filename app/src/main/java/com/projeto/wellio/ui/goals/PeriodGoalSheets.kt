package com.projeto.wellio.ui.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.projeto.wellio.domain.goals.PeriodGoal
import com.projeto.wellio.domain.goals.PeriodKind
import com.projeto.wellio.domain.goals.PeriodMetric
import com.projeto.wellio.domain.goals.daysIn
import com.projeto.wellio.domain.goals.periodContaining
import com.projeto.wellio.domain.weight.DEFAULT_WEIGHT_KG
import com.projeto.wellio.ui.theme.WellioTheme
import com.projeto.wellio.ui.weight.WeightPicker
import com.projeto.wellio.ui.weight.WeightPickerState
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val dayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Passos da criação (SPEC §9.2). */
private enum class CreateStep { Metric, Period, Value, Finish }

/** Valor como aparece no campo: ml viram litros, o resto fica inteiro. */
private fun fieldValue(metric: PeriodMetric, value: Double): String =
    if (metric == PeriodMetric.WaterTotal) formatFieldLiters(value) else value.roundToInt().toString()

private fun formatFieldLiters(ml: Double): String {
    val liters = ml / 1000
    return if (liters == liters.roundToInt().toDouble()) liters.roundToInt().toString() else "%.1f".format(liters).replace('.', ',')
}

/** Unidade do campo de valor (água em litros). */
private fun fieldUnit(metric: PeriodMetric): String = when (metric) {
    PeriodMetric.WaterTotal -> "L"
    PeriodMetric.KcalTotal, PeriodMetric.ExerciseKcalTotal -> "kcal"
    PeriodMetric.ExerciseMinutesTotal -> "min"
    PeriodMetric.WaterDays, PeriodMetric.KcalDays, PeriodMetric.ExerciseDays -> "dias"
    PeriodMetric.Workouts -> "treinos"
    PeriodMetric.Weight -> "kg"
}

/** Texto do campo → valor na unidade da métrica (água em ml); null se inválido. */
private fun parseField(metric: PeriodMetric, text: String, days: Int): Double? {
    val number = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 } ?: return null
    return when (metric) {
        PeriodMetric.WaterTotal -> (number * 1000).roundToInt().toDouble()
        PeriodMetric.WaterDays, PeriodMetric.KcalDays, PeriodMetric.ExerciseDays -> number.roundToInt().toDouble().takeIf { it <= days }
        else -> number.roundToInt().toDouble()
    }
}

/**
 * Nova meta de período, em passos (SPEC §9.2): métrica → período → valor (com a
 * sugestão) → recorrência e nome. A de peso pula o período: escolhe alvo e prazo.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePeriodGoalSheet(
    today: LocalDate,
    weekStart: DayOfWeek,
    currentKg: Double?,
    hasWeightGoal: Boolean,
    suggest: suspend (PeriodMetric, PeriodKind, ClosedRange<LocalDate>) -> Double,
    onDismiss: () -> Unit,
    onCreate: (PeriodGoal) -> Unit,
    onCreateWeight: (targetKg: Double, deadline: LocalDate) -> Unit
) {
    val colors = WellioTheme.colors.base
    var step by remember { mutableStateOf(CreateStep.Metric) }
    var metric by remember { mutableStateOf(PeriodMetric.WaterTotal) }
    var kind by remember { mutableStateOf(PeriodKind.Month) }
    var customEnd by remember { mutableStateOf(today.plusDays(29)) }
    var pickingEnd by remember { mutableStateOf(false) }
    var valueText by remember { mutableStateOf("") }
    var recurring by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var titleEdited by remember { mutableStateOf(false) }
    var confirmReplace by remember { mutableStateOf(false) }
    val weightPicker = remember { WeightPickerState(currentKg ?: DEFAULT_WEIGHT_KG) }

    val range = if (kind == PeriodKind.Custom) today..customEnd else periodContaining(kind, today, weekStart)
    val days = daysIn(range)
    val isWeight = metric == PeriodMetric.Weight
    val target = if (isWeight) weightPicker.weight else parseField(metric, valueText, days)
    val canRecur = kind != PeriodKind.Custom && !isWeight
    val generated = target?.let { periodGoalTitle(metric, it, kind, range.start, range.endInclusive, recurring && canRecur, today) }
    // O nome acompanha as escolhas até o usuário mexer nele
    val shownTitle = if (titleEdited) title else generated.orEmpty()

    // Chegando no valor: preenche com a sugestão
    LaunchedEffect(step, metric, kind, customEnd) {
        if (step == CreateStep.Value && !isWeight) valueText = fieldValue(metric, suggest(metric, kind, range))
    }

    fun back() {
        step = when (step) {
            CreateStep.Metric -> return onDismiss()
            CreateStep.Period -> CreateStep.Metric
            CreateStep.Value -> if (isWeight) CreateStep.Metric else CreateStep.Period
            CreateStep.Finish -> CreateStep.Value
        }
    }

    fun create() {
        val value = target ?: return
        if (isWeight) {
            onCreateWeight(value, customEnd)
        } else {
            onCreate(
                PeriodGoal(
                    id = "",
                    title = shownTitle.trim().ifEmpty { generated.orEmpty() },
                    metric = metric,
                    target = value,
                    kind = kind,
                    start = range.start,
                    end = range.endInclusive,
                    recurring = recurring && canRecur
                )
            )
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (step != CreateStep.Metric) {
                    IconButton(onClick = ::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                } else {
                    Spacer(Modifier.width(16.dp))
                }
                Text(
                    when (step) {
                        CreateStep.Metric -> "O que medir?"
                        CreateStep.Period -> "Em quanto tempo?"
                        CreateStep.Value -> if (isWeight) "Alvo e prazo" else "Quanto?"
                        CreateStep.Finish -> "Últimos detalhes"
                    },
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(Modifier.height(8.dp))
            when (step) {
                CreateStep.Metric -> PeriodMetric.entries.forEach { option ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                metric = option
                                titleEdited = false
                                if (option == PeriodMetric.Weight) kind = PeriodKind.Custom
                                step = if (option == PeriodMetric.Weight) CreateStep.Value else CreateStep.Period
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(metricLabel(option), style = MaterialTheme.typography.bodyLarge)
                        Text(metricExample(option), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                CreateStep.Period -> Column(Modifier.padding(horizontal = 24.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(PeriodKind.Week to "Esta semana", PeriodKind.Month to "Este mês", PeriodKind.Year to "Este ano", PeriodKind.Custom to "Datas")
                            .forEach { (option, label) ->
                                FilterChip(selected = kind == option, onClick = { kind = option }, label = { Text(label) })
                            }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (kind == PeriodKind.Custom) {
                        OutlinedButton(onClick = { pickingEnd = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("De hoje até ${customEnd.format(dayFormatter)}")
                        }
                    } else {
                        Text(
                            "${range.start.format(dayFormatter)} a ${range.endInclusive.format(dayFormatter)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { step = CreateStep.Value },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.container, contentColor = colors.onContainer),
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Continuar") }
                }
                CreateStep.Value -> Column {
                    if (isWeight) {
                        WeightPicker(weightPicker)
                        OutlinedButton(onClick = { pickingEnd = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                            Text("Até ${customEnd.format(dayFormatter)}")
                        }
                        if (hasWeightGoal) {
                            Text(
                                "Vira o peso alvo da tela de Peso, no lugar do atual.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = valueText,
                            onValueChange = { valueText = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(7) },
                            label = { Text("Meta (${fieldUnit(metric)})") },
                            isError = target == null,
                            supportingText = {
                                if (target == null && metric.countsDays) Text("Até $days dias") else Text("Sugestão pelo seu histórico")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // Peso não tem nome nem recorrência: cria daqui
                            when {
                                !isWeight -> step = CreateStep.Finish
                                hasWeightGoal -> confirmReplace = true
                                else -> create()
                            }
                        },
                        enabled = target != null,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.container, contentColor = colors.onContainer),
                        modifier = Modifier.align(Alignment.End).padding(horizontal = 24.dp)
                    ) { Text(if (isWeight) "Criar meta" else "Continuar") }
                }
                CreateStep.Finish -> Column(Modifier.padding(horizontal = 24.dp)) {
                    OutlinedTextField(
                        value = shownTitle,
                        onValueChange = {
                            title = it.take(60)
                            titleEdited = true
                        },
                        label = { Text("Nome") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (canRecur) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Repetir", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Quando acabar, começa outra igual",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = recurring, onCheckedChange = { recurring = it })
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = ::create,
                        enabled = target != null,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.container, contentColor = colors.onContainer),
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Criar meta") }
                }
            }
        }
    }

    if (pickingEnd) {
        FutureDatePickerDialog(customEnd, from = today.plusDays(1), onDismiss = { pickingEnd = false }, onPick = {
            customEnd = it
            pickingEnd = false
        })
    }
    if (confirmReplace) {
        AlertDialog(
            onDismissRequest = { confirmReplace = false },
            title = { Text("Trocar o peso alvo?") },
            text = { Text("Só existe um alvo de peso por vez. Esta meta substitui o atual, também na tela de Peso.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReplace = false
                    create()
                }) { Text("Trocar") }
            },
            dismissButton = { TextButton(onClick = { confirmReplace = false }) { Text("Cancelar") } }
        )
    }
}

/** Editar uma meta de período (SPEC §9.2): valor, nome e recorrência; métrica e período não mudam. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPeriodGoalSheet(goal: PeriodGoal, onDismiss: () -> Unit, onSave: (PeriodGoal) -> Unit, onDelete: () -> Unit) {
    val colors = WellioTheme.colors.base
    var title by remember { mutableStateOf(goal.title) }
    var valueText by remember { mutableStateOf(fieldValue(goal.metric, goal.target)) }
    var recurring by remember { mutableStateOf(goal.recurring) }
    val target = parseField(goal.metric, valueText, daysIn(goal.start..goal.end))

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("Meta de período", style = MaterialTheme.typography.titleLarge)
            Text(
                "${metricLabel(goal.metric)} · ${goal.start.format(dayFormatter)} a ${goal.end.format(dayFormatter)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(60) },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = valueText,
                onValueChange = { valueText = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(7) },
                label = { Text("Meta (${fieldUnit(goal.metric)})") },
                isError = target == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (goal.kind != PeriodKind.Custom) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Repetir", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = recurring, onCheckedChange = { recurring = it })
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDelete) { Text("Apagar", color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { target?.let { onSave(goal.copy(title = title.trim().ifEmpty { goal.title }, target = it, recurring = recurring)) } },
                    enabled = target != null,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.container, contentColor = colors.onContainer)
                ) { Text("Salvar") }
            }
        }
    }
}

/** Calendário para datas a partir de [from] (prazos de metas). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutureDatePickerDialog(day: LocalDate, from: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    // O DatePicker trabalha com meia-noite UTC
    fun LocalDate.utcMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val fromMillis = from.utcMillis()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = maxOf(day, from).utcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= fromMillis
            override fun isSelectableYear(year: Int) = year >= from.year
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis
                    ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    ?.let(onPick) ?: onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    ) {
        DatePicker(state = state)
    }
}

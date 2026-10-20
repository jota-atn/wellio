package com.projeto.wellio.ui.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.projeto.wellio.domain.model.ExerciseUnit
import com.projeto.wellio.domain.model.Goal
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.domain.model.Goals
import com.projeto.wellio.domain.water.MAX_WATER_GOAL_ML
import com.projeto.wellio.domain.water.MIN_WATER_GOAL_ML
import com.projeto.wellio.ui.theme.WellioTheme

/** Faixa aceita da meta diária de cada tipo (exercício na unidade escolhida). */
fun dailyGoalRange(type: GoalType, unit: ExerciseUnit): IntRange = when (type) {
    GoalType.WATER -> MIN_WATER_GOAL_ML..MAX_WATER_GOAL_ML
    GoalType.FOOD -> 1000..6000
    GoalType.EXERCISE -> if (unit == ExerciseUnit.Minutes) 5..300 else 50..5000
}

/** "ml", "kcal" ou "min". */
fun goalUnit(type: GoalType, unit: ExerciseUnit): String = when (type) {
    GoalType.WATER -> "ml"
    GoalType.FOOD -> "kcal"
    GoalType.EXERCISE -> unit.label
}

fun goalTitle(type: GoalType): String = when (type) {
    GoalType.WATER -> "Água"
    GoalType.FOOD -> "Calorias"
    GoalType.EXERCISE -> "Exercício"
}

/**
 * Meta diária e semanal de uma meta (SPEC §9.2): a diária (com a [suggestion], se houver),
 * a unidade do exercício e a semanal automática (diária × 7) ou própria. É o mesmo sheet
 * no Dashboard, no card da Home e na tela de Metas.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DailyGoalSheet(
    type: GoalType,
    goals: Goals,
    suggestion: Int?,
    onDismiss: () -> Unit,
    onSave: (Goal, ExerciseUnit) -> Unit
) {
    val colors = WellioTheme.colors.base
    val current = goals[type]
    var unit by remember { mutableStateOf(goals.exerciseUnit) }
    var dailyText by remember { mutableStateOf(current.daily.toString()) }
    var autoWeekly by remember { mutableStateOf(current.weeklyOverride == null) }
    var weeklyText by remember { mutableStateOf(current.weekly.toString()) }
    val range = dailyGoalRange(type, unit)
    val daily = dailyText.toIntOrNull()?.takeIf { it in range }
    val weekly = if (autoWeekly) daily?.times(7) else weeklyText.toIntOrNull()?.takeIf { it > 0 }
    val unitLabel = goalUnit(type, unit)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("Meta de ${goalTitle(type).lowercase()}", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            if (type == GoalType.EXERCISE) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ExerciseUnit.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = unit == option,
                            onClick = {
                                if (unit != option) {
                                    dailyText = option.defaultDaily.toString()
                                    weeklyText = (option.defaultDaily * 7).toString()
                                }
                                unit = option
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, ExerciseUnit.entries.size),
                            label = { Text(if (option == ExerciseUnit.Kcal) "kcal queimadas" else "minutos ativos") }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            OutlinedTextField(
                value = dailyText,
                onValueChange = { dailyText = it.filter(Char::isDigit).take(4) },
                label = { Text("Por dia ($unitLabel)") },
                isError = daily == null,
                supportingText = { if (daily == null) Text("Entre ${range.first} e ${range.last} $unitLabel") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (suggestion != null && (type != GoalType.EXERCISE || unit == goals.exerciseUnit)) {
                SuggestionChip(
                    onClick = { dailyText = suggestion.toString() },
                    label = { Text("Sugestão: $suggestion $unitLabel") }
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Semanal automática", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (autoWeekly) "A diária × 7${daily?.let { " = ${it * 7} $unitLabel" } ?: ""}" else "Valor próprio para a semana",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = autoWeekly, onCheckedChange = {
                    autoWeekly = it
                    if (!it) weeklyText = ((daily ?: current.daily) * 7).toString()
                })
            }
            if (!autoWeekly) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = weeklyText,
                    onValueChange = { weeklyText = it.filter(Char::isDigit).take(6) },
                    label = { Text("Por semana ($unitLabel)") },
                    isError = weekly == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (daily != null && weekly != null) {
                        onSave(Goal(daily, weeklyOverride = weekly.takeIf { !autoWeekly && it != daily * 7 }), unit)
                    }
                },
                enabled = daily != null && weekly != null,
                colors = ButtonDefaults.buttonColors(containerColor = colors.container, contentColor = colors.onContainer),
                modifier = Modifier.align(Alignment.End)
            ) { Text("Salvar") }
        }
    }
}

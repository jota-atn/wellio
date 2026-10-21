package com.projeto.wellio.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.projeto.wellio.domain.model.ExerciseUnit
import com.projeto.wellio.domain.model.Goal
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.ui.components.DayNavigator
import com.projeto.wellio.ui.components.swipeBetweenDays
import com.projeto.wellio.ui.format.dayLabel
import com.projeto.wellio.ui.format.formatInt
import com.projeto.wellio.ui.navigation.sharedContainer
import com.projeto.wellio.ui.theme.DarkWellioColors
import com.projeto.wellio.ui.theme.WellioTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun DashboardRoute(
    onBack: () -> Unit,
    fromCard: Boolean = false,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(state, viewModel.zone, onBack, viewModel::selectDay, viewModel::saveGoal, fromCard)
}

/** Ordem das linhas, a mesma dos anéis (de fora para dentro). */
private val RingOrder = listOf(GoalType.WATER, GoalType.FOOD, GoalType.EXERCISE)

private fun percent(score: Float) = (score * 100).toInt()

/**
 * Tela do Dashboard (SPEC §5.4): os três anéis do dia com o % no centro e, abaixo, as
 * linhas das metas (tocar muda a meta). Setas, deslizar e calendário trocam o dia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    zone: ZoneId,
    onBack: () -> Unit,
    onDayChange: (java.time.LocalDate) -> Unit,
    onSaveGoal: (GoalType, Goal, ExerciseUnit) -> Unit,
    fromCard: Boolean = false
) {
    val colors = WellioTheme.colors.base
    var editing by remember { mutableStateOf<GoalType?>(null) }
    // Anel sendo segurado (SPEC §5.4)
    var held by remember { mutableStateOf<GoalType?>(null) }
    // O balão fica do lado oposto ao dedo, para não ser coberto por ele
    var fingerOnTop by remember { mutableStateOf(true) }
    val rings = state.rings

    Scaffold(
        modifier = Modifier.sharedContainer("card-dashboard", RoundedCornerShape(28.dp), enabled = fromCard, screen = true),
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .swipeBetweenDays(state.day, state.today, onDayChange)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DayNavigator(state.day, state.today, onDayChange)
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = colors.container,
                contentColor = colors.onContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp)
                        .clearAndSetSemantics {
                            contentDescription = "${dayLabel(state.day, state.today)}: ${percent(rings.score)} por cento das metas"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Trocar de dia recomeça os anéis do zero
                    key(state.day) {
                        GoalRings(
                            rings,
                            size = RingsSize,
                            stroke = RingsStroke,
                            highlight = held?.let(RingOrder::indexOf),
                            modifier = Modifier.holdToInspect(onHold = { type, onTop ->
                                held = type
                                fingerOnTop = onTop
                            })
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val heldType = held
                        if (heldType == null) {
                            Text("${percent(rings.score)}%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                            Text(
                                if (rings.allClosed) "anéis fechados!" else dayLabel(state.day, state.today).lowercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.onContainer.copy(alpha = 0.8f)
                            )
                        } else {
                            val value = when (heldType) {
                                GoalType.WATER -> rings.water
                                GoalType.FOOD -> rings.food
                                GoalType.EXERCISE -> rings.exercise
                            }
                            Text("${percent(value)}%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = ringColor(heldType))
                            Text(goalTitle(heldType).lowercase(), style = MaterialTheme.typography.labelLarge, color = colors.onContainer.copy(alpha = 0.8f))
                        }
                    }
                    held?.let { type ->
                        RingBalloon(
                            ringSummary(type, state, zone),
                            modifier = if (fingerOnTop) Modifier.align(Alignment.BottomCenter).offset(y = 20.dp)
                            else Modifier.align(Alignment.TopCenter).offset(y = (-20).dp)
                        )
                    }
                }
            }
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    RingOrder.forEach { type ->
                        GoalLine(type, state, onClick = { editing = type })
                    }
                }
            }
        }
    }

    editing?.let { type ->
        DailyGoalSheet(
            type = type,
            goals = state.currentGoals,
            suggestion = state.suggestions[type],
            onDismiss = { editing = null },
            onSave = { goal, unit ->
                onSaveGoal(type, goal, unit)
                editing = null
            }
        )
    }
}

/** Cor do anel de cada meta (as vivas, iguais às dos anéis). */
private fun ringColor(type: GoalType) = when (type) {
    GoalType.WATER -> DarkWellioColors.water.accent
    GoalType.FOOD -> DarkWellioColors.food.accent
    GoalType.EXERCISE -> DarkWellioColors.exercise.accent
}

/** "● Água   1.400 / 2.000 ml ›", na meta que valia no dia. */
@Composable
private fun GoalLine(type: GoalType, state: DashboardUiState, onClick: () -> Unit) {
    val unit = goalUnit(type, state.goals.exerciseUnit)
    val value = state.totals.toward(type, state.goals)
    val goal = state.goals[type].daily
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Mudar meta", onClick = onClick)
            .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${goalTitle(type)}: ${formatInt(value)} de ${formatInt(goal)} $unit"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(ringColor(type)))
        Spacer(Modifier.width(12.dp))
        Text(goalTitle(type), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(formatInt(value), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            " / ${formatInt(goal)} $unit",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val RingsSize = 232.dp
private val RingsStroke = 18.dp

/**
 * Segurar um anel (SPEC §5.4): vibra, e [onHold] diz qual anel está sob o dedo (e se o dedo
 * está na metade de cima); arrastar para outro troca (vibrando de novo); soltar devolve null.
 */
@Composable
private fun Modifier.holdToInspect(onHold: (GoalType?, Boolean) -> Unit): Modifier {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    return pointerInput(Unit) {
        val stroke = with(density) { RingsStroke.toPx() }
        fun ringAt(point: Offset): GoalType? {
            val distance = (point - Offset(size.width / 2f, size.height / 2f)).getDistance()
            val index = RingOrder.indices.minByOrNull { i ->
                abs(distance - (size.width / 2f - with(density) { ringInset(RingsStroke, i).toPx() } - stroke / 2))
            } ?: return null
            val radius = size.width / 2f - with(density) { ringInset(RingsStroke, index).toPx() } - stroke / 2
            return RingOrder[index].takeIf { abs(distance - radius) <= stroke * 1.2f }
        }
        var current: GoalType? = null
        fun update(point: Offset) {
            val type = ringAt(point)
            if (type != null && type != current) haptics.performHapticFeedback(if (current == null) HapticFeedbackType.LongPress else HapticFeedbackType.SegmentTick)
            if (type != null) current = type
            onHold(current, point.y < size.height / 2f)
        }
        detectDragGesturesAfterLongPress(
            onDragStart = ::update,
            onDrag = { change, _ -> update(change.position) },
            onDragEnd = {
                current = null
                onHold(null, true)
            },
            onDragCancel = {
                current = null
                onHold(null, true)
            }
        )
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Resumo do dia de uma meta para o balão: "1.400 de 2.000 ml · faltam 600 ml · 6 registros, o último às 15h". */
private fun ringSummary(type: GoalType, state: DashboardUiState, zone: ZoneId): String {
    val unit = goalUnit(type, state.goals.exerciseUnit)
    val value = state.totals.toward(type, state.goals)
    val goal = state.goals[type].daily
    val left = goal - value
    val parts = mutableListOf("${formatInt(value)} de ${formatInt(goal)} $unit")
    parts += if (left > 0) "faltam ${formatInt(left)} $unit" else "meta batida!"
    val details = state.details
    when (type) {
        GoalType.WATER -> if (details.waterEntries > 0) {
            val last = details.lastWaterAt?.let { ", o último às ${timeFormatter.format(it.atZone(zone))}" } ?: ""
            parts += (if (details.waterEntries == 1) "1 registro" else "${details.waterEntries} registros") + last
        }
        GoalType.EXERCISE -> if (details.workoutNames.isNotEmpty()) parts += details.workoutNames.joinToString(", ")
        GoalType.FOOD -> Unit
    }
    return parts.joinToString(" · ")
}

/** Balão por cima dos anéis enquanto um é segurado. */
@Composable
private fun RingBalloon(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = 6.dp,
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

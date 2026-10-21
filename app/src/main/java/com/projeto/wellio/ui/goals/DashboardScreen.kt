package com.projeto.wellio.ui.goals

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.projeto.wellio.R
import com.projeto.wellio.domain.goals.progress
import com.projeto.wellio.ui.exercise.Campfire
import com.projeto.wellio.ui.water.WaterDrop
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp, bottom = 16.dp)
                        .clearAndSetSemantics {
                            contentDescription = "${dayLabel(state.day, state.today)}: ${percent(rings.score)} por cento das metas" +
                                (state.yesterdayScore?.let { ", ${comparedToYesterday(rings.score, it)}" } ?: "")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Três anéis fechados: o quadro brilha e solta partículas
                    if (rings.allClosed) ClosedGlow(Modifier.matchParentSize())
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
                    if (held == null) {
                        state.yesterdayScore?.let { yesterday ->
                            YesterdayChip(comparedToYesterday(rings.score, yesterday), up = rings.score >= yesterday, modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp))
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
                // Frase do dia (SPEC §5.4)
                Text(
                    dayPhrase(state.totals, state.goals, rings, isToday = state.day == state.today),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = colors.onContainer,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 22.dp)
                )
                }
            }
            RingOrder.forEach { type ->
                GoalTile(type, state, onClick = { editing = type })
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

/**
 * Peça de uma meta (SPEC §5.4): fundo na cor do módulo, o elemento dele em miniatura
 * enchendo pelo dia (gota, prato, fogueira) e valor/meta, na meta que valia no dia.
 */
@Composable
private fun GoalTile(type: GoalType, state: DashboardUiState, onClick: () -> Unit) {
    val palette = when (type) {
        GoalType.WATER -> WellioTheme.colors.water
        GoalType.FOOD -> WellioTheme.colors.food
        GoalType.EXERCISE -> WellioTheme.colors.exercise
    }
    val unit = goalUnit(type, state.goals.exerciseUnit)
    val value = state.totals.toward(type, state.goals)
    val goal = state.goals[type].daily
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = palette.container,
        contentColor = palette.onContainer,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "${goalTitle(type)}: ${formatInt(value)} de ${formatInt(goal)} $unit"
            }
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                when (type) {
                    GoalType.WATER -> WaterDrop(value, goal, additions = 0, modifier = Modifier.size(width = 46.dp, height = 56.dp))
                    GoalType.EXERCISE -> Campfire(value, goal, additions = 0, modifier = Modifier.size(width = 50.dp, height = 56.dp))
                    GoalType.FOOD -> Box(
                        Modifier.size(48.dp).clip(CircleShape).background(palette.accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(R.drawable.ic_module_food), contentDescription = null, tint = palette.accent, modifier = Modifier.size(28.dp))
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(goalTitle(type), style = MaterialTheme.typography.labelLarge, color = palette.onContainer.copy(alpha = 0.8f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(formatInt(value), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(" / ${formatInt(goal)} $unit", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 2.dp))
                }
            }
            Text(
                "${percent(progress(value, goal))}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.accent
            )
        }
    }
}

/** "+12% que ontem", no canto do quadro. */
@Composable
private fun YesterdayChip(text: String, up: Boolean, modifier: Modifier = Modifier) {
    val colors = WellioTheme.colors.base
    Surface(
        shape = RoundedCornerShape(50),
        color = colors.onContainer.copy(alpha = 0.12f),
        contentColor = if (up) DarkWellioColors.food.accent else colors.onContainer.copy(alpha = 0.85f),
        modifier = modifier
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

private class Spark(val x: Float, val speed: Float, val phase: Float, val size: Float, val color: Color)

/**
 * Brilho de quando os três anéis fecham (SPEC §5.4): um halo que pulsa atrás dos anéis e
 * partículas nas três cores subindo devagar, enquanto a tela estiver aberta.
 */
@Composable
private fun ClosedGlow(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "brilho")
    val pulse by transition.animateFloat(0.6f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "halo")
    val time by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "partículas")
    val colors = listOf(DarkWellioColors.water.accent, DarkWellioColors.food.accent, DarkWellioColors.exercise.accent, Color(0xFFFFD45C))
    val sparks = remember {
        val random = kotlin.random.Random(7)
        List(22) { Spark(random.nextFloat(), 0.6f + random.nextFloat() * 0.8f, random.nextFloat(), 2f + random.nextFloat() * 3f, colors[random.nextInt(colors.size)]) }
    }
    Canvas(modifier) {
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFFFFD45C).copy(alpha = 0.28f * pulse), Color.Transparent),
                center = center,
                radius = size.minDimension * 0.62f
            ),
            radius = size.minDimension * 0.62f,
            center = center
        )
        sparks.forEach { spark ->
            val t = (time * spark.speed + spark.phase) % 1f
            val y = size.height * (1f - t)
            val x = size.width * spark.x + kotlin.math.sin(t * 12f + spark.phase * 6f) * 10.dp.toPx()
            val alpha = if (t < 0.15f) t / 0.15f else if (t > 0.8f) (1f - t) / 0.2f else 1f
            drawCircle(spark.color.copy(alpha = 0.8f * alpha), radius = spark.size.dp.toPx() / 2, center = Offset(x, y))
        }
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

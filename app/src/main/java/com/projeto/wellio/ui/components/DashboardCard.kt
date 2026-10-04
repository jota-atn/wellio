package com.projeto.wellio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.projeto.wellio.domain.goals.DayScore
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.ui.module.Modules
import com.projeto.wellio.ui.module.Summary
import com.projeto.wellio.ui.theme.DarkWellioColors
import com.projeto.wellio.ui.theme.WellioTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Sobre o azul-marinho (igual nos dois temas), os módulos usam as cores vivas do tema escuro. */
private val OnNavy = DarkWellioColors

/** Resumos das metas diárias, na ordem dos anéis (de fora para dentro). */
private fun goalSummaries(summaries: List<Summary>) =
    summaries.filter { Modules.of(it.module).goalType != null }

private fun percent(score: Float) = (score * 100).toInt()

/**
 * Dashboard inteiro (SPEC §5.1): anéis das 3 metas com o % do dia, as metas com valor e
 * o placar dos últimos 7 dias (hoje em destaque). Tocar numa meta muda a meta.
 */
@Composable
fun DashboardCard(
    summaries: List<Summary>,
    lastWeek: List<DayScore>,
    onClick: () -> Unit,
    onGoalClick: (GoalType) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WellioTheme.colors.base
    val goals = goalSummaries(summaries)
    val todayScore = lastWeek.lastOrNull()?.score ?: 0f
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = colors.container,
        contentColor = colors.onContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "Hoje: ${percent(todayScore)} por cento das metas"
                },
                contentAlignment = Alignment.Center
            ) {
                GoalRings(goals, size = 96.dp, stroke = 7.dp)
                Text("${percent(todayScore)}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                goals.forEach { summary ->
                    val module = Modules.of(summary.module)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClickLabel = "Mudar meta") { module.goalType?.let(onGoalClick) }
                            .semantics(mergeDescendants = true) { contentDescription = summary.spoken },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(module.icon),
                            contentDescription = null,
                            tint = module.colors(OnNavy).accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(summary.value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        summary.secondary?.let {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onContainer.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            WeekBars(lastWeek, accent = colors.accent, ink = colors.onContainer, modifier = Modifier.width(80.dp).height(88.dp))
        }
    }
}

/** Dashboard metade (SPEC §5.1): os 3 anéis com o % do dia no centro. */
@Composable
fun DashboardHalfCard(
    summaries: List<Summary>,
    todayScore: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WellioTheme.colors.base
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = colors.container,
        contentColor = colors.onContainer,
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = "Dashboard: hoje ${percent(todayScore)} por cento das metas. " +
                    goalSummaries(summaries).joinToString(". ") { it.spoken }
            }
    ) {
        Box(Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
            GoalRings(goalSummaries(summaries), size = 124.dp, stroke = 8.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${percent(todayScore)}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("hoje", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Anéis concêntricos, um por meta (de fora para dentro), que enchem com mola ao aparecer. */
@Composable
private fun GoalRings(goals: List<Summary>, size: Dp, stroke: Dp) {
    Box(Modifier.size(size)) {
        goals.forEachIndexed { index, summary ->
            key(summary.module) {
                GoalRing(
                    progress = summary.progress ?: 0f,
                    color = Modules.of(summary.module).colors(OnNavy).accent,
                    inset = (stroke + stroke * 0.3f) * index,
                    stroke = stroke
                )
            }
        }
    }
}

@Composable
private fun GoalRing(progress: Float, color: Color, inset: Dp, stroke: Dp) {
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        sweep.animateTo(progress, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessVeryLow))
    }
    Canvas(Modifier.fillMaxSize().padding(inset)) {
        val width = stroke.toPx()
        val topLeft = Offset(width / 2, width / 2)
        val arcSize = Size(size.width - width, size.height - width)
        drawArc(color.copy(alpha = 0.22f), 0f, 360f, false, topLeft, arcSize, style = Stroke(width))
        if (sweep.value > 0f) {
            drawArc(color, -90f, 360f * sweep.value, false, topLeft, arcSize, style = Stroke(width, cap = StrokeCap.Round))
        }
    }
}

/** Barras do % do dia nos últimos 7 dias; crescem uma depois da outra ao aparecer. */
@Composable
private fun WeekBars(days: List<DayScore>, accent: Color, ink: Color, modifier: Modifier = Modifier) {
    val ptBR = remember { Locale.forLanguageTag("pt-BR") }
    Column(
        modifier.semantics(mergeDescendants = true) {
            contentDescription = "Últimos 7 dias: " + days.joinToString(", ") {
                "${it.date.dayOfWeek.getDisplayName(TextStyle.FULL, ptBR)} ${percent(it.score)} por cento"
            }
        }
    ) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEachIndexed { index, day ->
                val height = remember(day.date) { Animatable(0f) }
                LaunchedEffect(day.score) {
                    delay(index * 45L)
                    height.animateTo(day.score, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
                }
                val today = index == days.lastIndex
                Canvas(Modifier.width(9.dp).fillMaxHeight()) {
                    val radius = CornerRadius(size.width / 2)
                    drawRoundRect(ink.copy(alpha = 0.14f), cornerRadius = radius)
                    val filled = size.height * height.value.coerceIn(0f, 1f)
                    if (filled > 0f) {
                        drawRoundRect(
                            if (today) accent else ink.copy(alpha = 0.55f),
                            topLeft = Offset(0f, size.height - filled),
                            size = Size(size.width, filled),
                            cornerRadius = radius
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEachIndexed { index, day ->
                Text(
                    day.date.weekdayInitial(ptBR),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (index == days.lastIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (index == days.lastIndex) accent else ink.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    softWrap = false,
                    modifier = Modifier.width(9.dp).wrapContentWidth(unbounded = true)
                )
            }
        }
    }
}

private fun LocalDate.weekdayInitial(locale: Locale): String =
    dayOfWeek.getDisplayName(TextStyle.NARROW, locale).uppercase(locale)

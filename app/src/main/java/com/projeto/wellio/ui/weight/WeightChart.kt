package com.projeto.wellio.ui.weight

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projeto.wellio.domain.model.WeightEntry
import com.projeto.wellio.ui.format.formatKg
import com.projeto.wellio.ui.format.shortDate
import java.time.LocalDate
import kotlin.math.abs

/** Cores da curva: linha, fundo do balão e texto sobre ele. */
data class ChartColors(val line: Color, val bubble: Color, val onBubble: Color, val guide: Color)

/**
 * Curva suave do peso no período (SPEC §6.1): desenha da esquerda para a direita ao
 * aparecer e o ponto mais recente pulsa. Segurar o dedo e arrastar mostra o peso e o
 * dia de cada pesagem. [trend] vem da mais antiga para a mais recente; [targetKg]
 * desenha a linha tracejada do alvo.
 */
@Composable
fun WeightChart(
    trend: List<WeightEntry>,
    targetKg: Double?,
    today: LocalDate,
    colors: ChartColors,
    modifier: Modifier = Modifier
) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(trend) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }
    val pulse by rememberInfiniteTransition(label = "pulso").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1400), RepeatMode.Restart),
        label = "halo"
    )
    var selected by remember(trend) { mutableStateOf<Int?>(null) }
    val haptics = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer()

    Canvas(
        modifier.pointerInput(trend) {
            fun nearest(x: Float): Int {
                val inset = 12.dp.toPx()
                val firstDay = trend.first().date.toEpochDay()
                val spanDays = (trend.last().date.toEpochDay() - firstDay).coerceAtLeast(1)
                return trend.indices.minBy { i ->
                    abs(inset + (size.width - 2 * inset) * (trend[i].date.toEpochDay() - firstDay) / spanDays - x)
                }
            }
            fun select(index: Int) {
                if (index != selected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                selected = index
            }
            detectDragGesturesAfterLongPress(
                onDragStart = { select(nearest(it.x)) },
                onDrag = { change, _ -> select(nearest(change.position.x)) },
                onDragEnd = { selected = null },
                onDragCancel = { selected = null }
            )
        }
    ) {
        if (trend.size < 2) return@Canvas
        val strokeWidth = 3.dp.toPx()
        val inset = 12.dp.toPx()
        // Espaço no alto para o balão do ponto escolhido
        val top = 34.dp.toPx()
        val firstDay = trend.first().date.toEpochDay()
        val spanDays = (trend.last().date.toEpochDay() - firstDay).coerceAtLeast(1)
        val weights = trend.map { it.weightKg } + listOfNotNull(targetKg)
        val minKg = weights.min() - 0.3
        val maxKg = weights.max() + 0.3
        fun yOf(kg: Double) = top + (size.height - top - inset) * (1 - ((kg - minKg) / (maxKg - minKg)).toFloat())
        val points = trend.map {
            Offset(inset + (size.width - 2 * inset) * (it.date.toEpochDay() - firstDay) / spanDays, yOf(it.weightKg))
        }

        targetKg?.let { target ->
            val y = yOf(target)
            drawLine(
                colors.guide,
                Offset(0f, y),
                Offset(size.width, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
            )
            val label = measurer.measure("alvo ${formatKg(target)}", TextStyle(color = colors.guide, fontSize = 11.sp))
            drawText(label, topLeft = Offset(size.width - label.size.width, y - label.size.height - 2.dp.toPx()))
        }

        // Curva com controles no meio de cada trecho: suave, sem passar dos pontos na vertical
        val curve = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.zipWithNext { a, b ->
                val midX = (a.x + b.x) / 2
                cubicTo(midX, a.y, midX, b.y, b.x, b.y)
            }
        }
        val area = Path().apply {
            addPath(curve)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }

        clipRect(right = size.width * reveal.value) {
            drawPath(area, Brush.verticalGradient(listOf(colors.line.copy(alpha = 0.3f), Color.Transparent), startY = top))
            drawPath(curve, colors.line, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        val chosen = selected
        if (chosen != null) {
            val point = points[chosen]
            drawLine(colors.guide, Offset(point.x, top), Offset(point.x, size.height), strokeWidth = 1.dp.toPx())
            drawCircle(colors.line, radius = strokeWidth * 2.2f, center = point)
            drawCircle(colors.bubble, radius = strokeWidth * 1.1f, center = point)

            val entry = trend[chosen]
            val text = measurer.measure(
                "${formatKg(entry.weightKg)} · ${shortDate(entry.date, today)}",
                TextStyle(color = colors.onBubble, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            )
            val padding = 8.dp.toPx()
            val bubble = Size(text.size.width + 2 * padding, text.size.height + padding)
            val left = (point.x - bubble.width / 2).coerceIn(0f, size.width - bubble.width)
            drawRoundRect(colors.bubble, Offset(left, 0f), bubble, CornerRadius(bubble.height / 2))
            drawText(text, topLeft = Offset(left + padding, padding / 2))
        } else if (reveal.value == 1f) {
            val last = points.last()
            drawCircle(colors.line.copy(alpha = 0.4f * (1 - pulse)), radius = strokeWidth * (2 + 3 * pulse), center = last)
            drawCircle(colors.line, radius = strokeWidth * 1.6f, center = last)
        }
    }
}

package com.projeto.wellio.ui.weight

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.projeto.wellio.domain.model.WeightEntry

/**
 * Curva suave do peso no período (SPEC §2.9): desenha da esquerda para a direita ao
 * aparecer, e o ponto mais recente pulsa. [trend] vem da mais antiga para a mais recente.
 */
@Composable
fun WeightChart(trend: List<WeightEntry>, line: Color, modifier: Modifier = Modifier) {
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

    Canvas(modifier) {
        if (trend.size < 2) return@Canvas
        val strokeWidth = 3.dp.toPx()
        val inset = 10.dp.toPx()
        val firstDay = trend.first().date.toEpochDay()
        val spanDays = (trend.last().date.toEpochDay() - firstDay).coerceAtLeast(1)
        val minKg = trend.minOf { it.weightKg } - 0.3
        val maxKg = trend.maxOf { it.weightKg } + 0.3
        val points = trend.map {
            Offset(
                x = inset + (size.width - 2 * inset) * (it.date.toEpochDay() - firstDay) / spanDays,
                y = inset + (size.height - 2 * inset) * (1 - ((it.weightKg - minKg) / (maxKg - minKg)).toFloat())
            )
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
            drawPath(area, Brush.verticalGradient(listOf(line.copy(alpha = 0.35f), Color.Transparent)))
            drawPath(curve, line, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        if (reveal.value == 1f) {
            val last = points.last()
            drawCircle(line.copy(alpha = 0.4f * (1 - pulse)), radius = strokeWidth * (2 + 3 * pulse), center = last)
            drawCircle(line, radius = strokeWidth * 1.6f, center = last)
        }
    }
}

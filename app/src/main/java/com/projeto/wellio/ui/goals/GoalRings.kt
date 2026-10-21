package com.projeto.wellio.ui.goals

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.projeto.wellio.domain.goals.Rings
import com.projeto.wellio.ui.theme.DarkWellioColors
import kotlinx.coroutines.launch

/** Sobre o azul-marinho (igual nos dois temas), os anéis usam as cores vivas do tema escuro. */
private val OnNavy = DarkWellioColors

/**
 * Três anéis concêntricos (SPEC §5.4): água por fora, kcal no meio, exercício por dentro.
 * Enchem com mola ao aparecer e ao mudar; o anel que fecha brilha uma vez. [animate] = false
 * desenha parado (anéis pequenos da semana). [highlight] (0 = água, 1 = kcal, 2 = exercício)
 * engrossa aquele anel e apaga os outros, enquanto ele é segurado.
 */
@Composable
fun GoalRings(rings: Rings, size: Dp, stroke: Dp, modifier: Modifier = Modifier, animate: Boolean = true, highlight: Int? = null) {
    val colors = listOf(OnNavy.water.accent, OnNavy.food.accent, OnNavy.exercise.accent)
    val values = listOf(rings.water, rings.food, rings.exercise)
    Box(modifier.size(size)) {
        values.forEachIndexed { index, value ->
            GoalRing(
                value,
                colors[index],
                inset = ringInset(stroke, index),
                stroke = stroke,
                animate = animate,
                emphasis = when (highlight) {
                    null -> RingEmphasis.Normal
                    index -> RingEmphasis.Held
                    else -> RingEmphasis.Dimmed
                }
            )
        }
    }
}

/** Distância da borda até o anel [index] (de fora para dentro). */
fun ringInset(stroke: Dp, index: Int): Dp = (stroke + stroke * 0.3f) * index

private enum class RingEmphasis { Normal, Held, Dimmed }

@Composable
private fun GoalRing(progress: Float, color: Color, inset: Dp, stroke: Dp, animate: Boolean, emphasis: RingEmphasis) {
    val alpha by animateFloatAsState(if (emphasis == RingEmphasis.Dimmed) 0.25f else 1f, label = "anel apagado")
    val thickness by animateFloatAsState(if (emphasis == RingEmphasis.Held) 1.25f else 1f, label = "anel segurado")
    val sweep = remember { Animatable(if (animate) 0f else progress) }
    val glow = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        if (!animate) {
            sweep.snapTo(progress)
            return@LaunchedEffect
        }
        val wasClosed = sweep.value >= 1f
        sweep.animateTo(progress, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessVeryLow))
        // Fechou agora: brilha
        if (progress >= 1f && !wasClosed) {
            launch {
                glow.animateTo(1f, tween(220))
                glow.animateTo(0f, tween(700))
            }
        }
    }
    Canvas(Modifier.fillMaxSize().padding(inset).graphicsLayer { this.alpha = alpha }) {
        val width = stroke.toPx() * thickness
        val topLeft = Offset(width / 2, width / 2)
        val arcSize = Size(size.width - width, size.height - width)
        drawArc(color.copy(alpha = 0.22f), 0f, 360f, false, topLeft, arcSize, style = Stroke(width))
        if (glow.value > 0f) {
            drawArc(color.copy(alpha = 0.35f * glow.value), 0f, 360f, false, topLeft, arcSize, style = Stroke(width * (1f + glow.value)))
        }
        if (sweep.value > 0f) {
            drawArc(color, -90f, 360f * sweep.value.coerceAtMost(1f), false, topLeft, arcSize, style = Stroke(width, cap = StrokeCap.Round))
        }
    }
}

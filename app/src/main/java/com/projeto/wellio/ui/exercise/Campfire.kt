package com.projeto.wellio.ui.exercise

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.projeto.wellio.domain.exercise.fireLevel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private val Log = Color(0xFF6D3B22)
private val LogDark = Color(0xFF4A2414)
private val FlameOuter = Color(0xFFE8452C)
private val FlameMiddle = Color(0xFFFF8A3D)
private val FlameCore = Color(0xFFFFD45C)
private val Ember = Color(0xFFFF6A3D)

/**
 * Fogueira (SPEC §8.0): a chama cresce com o progresso do dia, de brasa (0) a labareda (1);
 * as línguas de fogo dançam sem parar e, passada a meta, sobem faíscas. Quando o total
 * aumenta por um registro feito aqui ([additions] muda), a chama "aviva" com mola.
 */
@Composable
fun Campfire(progress: Float, additions: Int, modifier: Modifier = Modifier) {
    val level = fireLevel(progress)
    val intensity = remember { Animatable(0f) }
    val boost = remember { Animatable(1f) }
    val handled = remember { mutableIntStateOf(additions) }

    LaunchedEffect(level.intensity, additions) {
        if (additions != handled.intValue) {
            handled.intValue = additions
            launch {
                boost.snapTo(1.25f)
                boost.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessLow))
            }
        }
        intensity.animateTo(level.intensity, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessVeryLow))
    }

    val time by rememberInfiniteTransition(label = "fogo").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2400, easing = LinearEasing)),
        label = "tempo"
    )

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val base = Offset(w / 2, h * 0.86f)
        val i = intensity.value

        // Brilho no chão, maior com a chama
        drawCircle(
            Brush.radialGradient(listOf(Ember.copy(alpha = 0.18f + 0.3f * i), Color.Transparent), center = base, radius = w * (0.35f + 0.3f * i)),
            radius = w * (0.35f + 0.3f * i),
            center = base
        )

        // Chamas: três línguas, cada uma com fase e altura próprias
        val flameHeight = h * (0.12f + 0.62f * i) * boost.value
        if (i > 0.02f) {
            listOf(-0.16f to 0.75f, 0.16f to 0.8f, 0f to 1f).forEachIndexed { index, (dx, rel) ->
                val phase = time * 2 * PI.toFloat() + index * 2.1f
                val sway = sin(phase) * w * 0.05f
                val tongueHeight = flameHeight * rel * (0.92f + 0.08f * sin(phase * 1.7f))
                val center = Offset(base.x + dx * w, base.y - h * 0.04f)
                drawFlame(center, w * 0.34f * (0.6f + 0.4f * i), tongueHeight, sway, FlameOuter)
                drawFlame(center, w * 0.24f * (0.6f + 0.4f * i), tongueHeight * 0.72f, sway * 0.8f, FlameMiddle)
                drawFlame(center, w * 0.12f * (0.6f + 0.4f * i), tongueHeight * 0.42f, sway * 0.6f, FlameCore)
            }
        }

        drawLogs(base, w)

        // Brasa: pontinhos acesos sobre a lenha, mais visíveis com a chama baixa
        val emberAlpha = (1f - i).coerceIn(0.25f, 1f)
        listOf(-0.12f, 0.02f, 0.13f).forEachIndexed { index, dx ->
            val pulse = 0.6f + 0.4f * sin(time * 2 * PI.toFloat() * 2 + index)
            drawCircle(Ember.copy(alpha = emberAlpha * pulse), w * 0.025f, Offset(base.x + dx * w, base.y - h * 0.03f))
        }

        // Faíscas subindo, passada a meta
        if (level.sparks) drawSparks(base, w, h, flameHeight, time)
    }
}

/** Uma língua de fogo: gota invertida com a ponta balançando [sway] para o lado. */
private fun DrawScope.drawFlame(bottom: Offset, width: Float, height: Float, sway: Float, color: Color) {
    if (height <= 0f) return
    val half = width / 2
    val tip = Offset(bottom.x + sway, bottom.y - height)
    val path = Path().apply {
        moveTo(bottom.x - half, bottom.y)
        cubicTo(bottom.x - half, bottom.y - height * 0.45f, tip.x - half * 0.35f, tip.y + height * 0.35f, tip.x, tip.y)
        cubicTo(tip.x + half * 0.35f, tip.y + height * 0.35f, bottom.x + half, bottom.y - height * 0.45f, bottom.x + half, bottom.y)
        // Fundo arredondado
        cubicTo(bottom.x + half, bottom.y + half * 0.5f, bottom.x - half, bottom.y + half * 0.5f, bottom.x - half, bottom.y)
        close()
    }
    drawPath(path, color)
}

/** Duas toras cruzadas. */
private fun DrawScope.drawLogs(base: Offset, w: Float) {
    val logSize = Size(w * 0.62f, w * 0.1f)
    listOf(-11f to Log, 11f to LogDark).forEach { (angle, color) ->
        rotate(angle, base) {
            drawRoundRect(
                color,
                topLeft = Offset(base.x - logSize.width / 2, base.y - logSize.height / 2),
                size = logSize,
                cornerRadius = CornerRadius(logSize.height / 2)
            )
        }
    }
}

/** Faíscas que sobem da chama e somem; cada uma em loop com atraso próprio. */
private fun DrawScope.drawSparks(base: Offset, w: Float, h: Float, flameHeight: Float, time: Float) {
    val random = Random(7)
    repeat(9) {
        val offset = random.nextFloat()
        val drift = (random.nextFloat() - 0.5f) * w * 0.5f
        val t = (time * 1.6f + offset) % 1f
        val y = base.y - flameHeight * 0.6f - t * h * 0.55f
        val x = base.x + drift * t + sin((t + offset) * 6f) * w * 0.03f
        drawCircle(FlameCore.copy(alpha = (1f - t) * 0.9f), w * (0.012f + 0.01f * (1 - t)), Offset(x, y))
    }
}

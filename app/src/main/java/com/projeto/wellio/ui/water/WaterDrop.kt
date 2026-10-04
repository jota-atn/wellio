package com.projeto.wellio.ui.water

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import com.projeto.wellio.ui.theme.WellioTheme
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Gota que enche até [fraction] (0 a 1) do dia (SPEC §2.9). A superfície ondula sem
 * parar, devagar; quando o total muda, o nível sobe com mola elástica e a onda
 * "chacoalha" antes de acalmar. Com "Remover animações", fica parada.
 */
@Composable
fun WaterDrop(fraction: Float, modifier: Modifier = Modifier) {
    val colors = WellioTheme.colors.water

    val level = remember { Animatable(0f) }
    val agitation = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        agitation.snapTo(1f)
        launch { agitation.animateTo(0f, tween(durationMillis = 1600)) }
        level.animateTo(fraction.coerceIn(0f, 1f), spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessVeryLow))
    }

    val waves = rememberInfiniteTransition(label = "ondas")
    val phase by waves.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = 3200, easing = LinearEasing)),
        label = "fase"
    )

    Canvas(modifier) {
        val drop = dropPath(size)
        val amplitude = size.height * (0.012f + 0.04f * agitation.value)
        // Nível 0 = fundo da gota; 1 = acima da ponta, para a onda não aparecer com a gota cheia
        val surfaceY = size.height * (1f - level.value * 1.04f)

        clipPath(drop) {
            drawRect(colors.track)
            drawPath(wavePath(size, surfaceY, amplitude, phase + PI.toFloat() * 0.8f), colors.accent.copy(alpha = 0.45f))
            drawPath(wavePath(size, surfaceY + amplitude, amplitude, phase), colors.accent)
            // Reflexo do vidro, à esquerda
            drawArc(
                color = Color.White.copy(alpha = 0.35f),
                startAngle = 160f,
                sweepAngle = 50f,
                useCenter = false,
                topLeft = Offset(size.width * 0.18f, size.height * 0.42f),
                size = Size(size.width * 0.64f, size.width * 0.64f),
                style = Stroke(width = size.width * 0.05f, cap = StrokeCap.Round)
            )
        }
        drawPath(drop, colors.accent, style = Stroke(width = size.width * 0.025f))
    }
}

/** Contorno da gota: ponta no alto e corpo redondo embaixo, ocupando todo o [size]. */
private fun dropPath(size: Size): Path {
    val w = size.width
    val h = size.height
    val radius = w / 2f
    val centerY = h - radius
    return Path().apply {
        moveTo(w / 2f, 0f)
        cubicTo(w * 0.62f, h * 0.18f, w, h * 0.36f, w, centerY)
        arcTo(Rect(0f, centerY - radius, w, h), startAngleDegrees = 0f, sweepAngleDegrees = 180f, forceMoveTo = false)
        cubicTo(0f, h * 0.36f, w * 0.38f, h * 0.18f, w / 2f, 0f)
        close()
    }
}

/** Água abaixo de uma superfície senoidal na altura [surfaceY]. */
private fun wavePath(size: Size, surfaceY: Float, amplitude: Float, phase: Float): Path = Path().apply {
    val steps = 32
    moveTo(0f, size.height)
    for (i in 0..steps) {
        val x = size.width * i / steps
        val y = surfaceY + amplitude * sin(phase + 2f * PI.toFloat() * i / steps * 1.3f)
        lineTo(x, y)
    }
    lineTo(size.width, size.height)
    close()
}

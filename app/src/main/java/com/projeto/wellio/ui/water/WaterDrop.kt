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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import com.projeto.wellio.domain.goals.progress
import com.projeto.wellio.ui.theme.WellioTheme
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Espaço em volta da gota, onde as gotinhas do transbordar pulam. */
private const val SIDE_MARGIN = 0.14f
private const val TOP_MARGIN = 0.2f

/**
 * Gota que enche até a % da meta (SPEC §2.9). A superfície ondula sem parar, devagar;
 * quando o total muda, o nível sobe com mola elástica e a onda "chacoalha". Registrar
 * com a meta batida (ou batendo agora) faz a gota transbordar: gotinhas pulam da ponta
 * e escorrem pelas laterais. Com "Remover animações", fica tudo parado.
 *
 * [additions] conta os registros feitos pelo usuário nesta tela: só eles transbordam,
 * e não o total subindo ao abrir a tela ou trocar de dia.
 */
@Composable
fun WaterDrop(totalMl: Int, goalMl: Int, additions: Int, modifier: Modifier = Modifier) {
    val colors = WellioTheme.colors.water
    val fraction = progress(totalMl, goalMl)

    val level = remember { Animatable(0f) }
    val agitation = remember { Animatable(0f) }
    val overflow = remember { Animatable(1f) }
    val overflowSeed = remember { mutableIntStateOf(0) }
    val previousTotal = remember { mutableIntStateOf(totalMl) }
    val handledAdditions = remember { mutableIntStateOf(additions) }
    // Escopo próprio: o transbordar termina mesmo se o total mudar no meio (ex.: desfazer
    // logo depois de registrar). Preso ao efeito abaixo, ele era cancelado e as gotinhas
    // ficavam paradas na tela.
    val overflowScope = rememberCoroutineScope()

    LaunchedEffect(totalMl, goalMl) {
        val added = totalMl > previousTotal.intValue && additions != handledAdditions.intValue
        previousTotal.intValue = totalMl
        if (added) handledAdditions.intValue = additions
        if (added && goalMl > 0 && totalMl >= goalMl) {
            overflowSeed.intValue++
            overflowScope.launch {
                overflow.snapTo(0f)
                overflow.animateTo(1f, tween(durationMillis = 1800, easing = LinearEasing))
            }
        }
        agitation.snapTo(1f)
        launch { agitation.animateTo(0f, tween(durationMillis = 1600)) }
        level.animateTo(fraction, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessVeryLow))
    }

    val waves = rememberInfiniteTransition(label = "ondas")
    val phase by waves.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = 3200, easing = LinearEasing)),
        label = "fase"
    )

    Canvas(modifier) {
        val dropSize = Size(size.width * (1 - 2 * SIDE_MARGIN), size.height * (1 - TOP_MARGIN))
        val origin = Offset(size.width * SIDE_MARGIN, size.height * TOP_MARGIN)
        translate(origin.x, origin.y) {
            val drop = dropPath(dropSize)
            val amplitude = dropSize.height * (0.012f + 0.04f * agitation.value)
            // Nível 0 = fundo da gota; 1 = acima da ponta, para a onda não aparecer com a gota cheia
            val surfaceY = dropSize.height * (1f - level.value * 1.04f)

            clipPath(drop) {
                drawRect(colors.track)
                drawPath(wavePath(dropSize, surfaceY, amplitude, phase + PI.toFloat() * 0.8f), colors.accent.copy(alpha = 0.45f))
                drawPath(wavePath(dropSize, surfaceY + amplitude, amplitude, phase), colors.accent)
                // Reflexo do vidro, à esquerda
                drawArc(
                    color = Color.White.copy(alpha = 0.35f),
                    startAngle = 160f,
                    sweepAngle = 50f,
                    useCenter = false,
                    topLeft = Offset(dropSize.width * 0.18f, dropSize.height * 0.42f),
                    size = Size(dropSize.width * 0.64f, dropSize.width * 0.64f),
                    style = Stroke(width = dropSize.width * 0.05f, cap = StrokeCap.Round)
                )
            }
            drawPath(drop, colors.accent, style = Stroke(width = dropSize.width * 0.025f))

            val p = overflow.value
            if (p < 1f) {
                drawSplash(dropSize, (p / 0.55f).coerceAtMost(1f), overflowSeed.intValue, colors.accent)
                drawTrickles(dropSize, ((p - 0.08f) / 0.92f).coerceIn(0f, 1f), colors.accent)
            }
        }
    }
}

/** Gotinhas que saltam da ponta em arco e caem; [t] vai de 0 a 1. */
private fun DrawScope.drawSplash(dropSize: Size, t: Float, seed: Int, color: Color) {
    if (t <= 0f || t >= 1f) return
    val random = Random(seed)
    val tip = Offset(dropSize.width / 2f, dropSize.height * 0.04f)
    repeat(7) {
        val vx = (random.nextFloat() * 2f - 1f) * dropSize.width * 0.75f
        val vy = -(0.35f + random.nextFloat() * 0.35f) * dropSize.height
        val gravity = dropSize.height * 1.4f
        val center = Offset(tip.x + vx * t, tip.y + vy * t + gravity * t * t / 2f)
        val radius = dropSize.width * (0.025f + random.nextFloat() * 0.025f) * (1f - t * 0.4f)
        drawCircle(color.copy(alpha = 1f - t * t), radius, center)
    }
}

/** Fios de água que escorrem por fora, da ponta até a parte mais larga da gota. */
private fun DrawScope.drawTrickles(dropSize: Size, t: Float, color: Color) {
    if (t <= 0f || t >= 1f) return
    val eased = t * t
    listOf(true, false).forEach { right ->
        val measure = PathMeasure().apply { setPath(sidePath(dropSize, right), false) }
        val head = measure.length * eased
        val trail = Path()
        measure.getSegment((head - dropSize.height * 0.25f).coerceAtLeast(0f), head, trail, true)
        val alpha = 1f - ((t - 0.75f) / 0.25f).coerceIn(0f, 1f)
        drawPath(trail, color.copy(alpha = 0.6f * alpha), style = Stroke(dropSize.width * 0.03f, cap = StrokeCap.Round))
        drawCircle(color.copy(alpha = alpha), dropSize.width * 0.04f, measure.getPosition(head))
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

/** Um lado do contorno, da ponta até um pouco abaixo da parte mais larga. */
private fun sidePath(size: Size, right: Boolean): Path {
    val w = size.width
    val h = size.height
    val radius = w / 2f
    val centerY = h - radius
    return Path().apply {
        moveTo(w / 2f, 0f)
        if (right) {
            cubicTo(w * 0.62f, h * 0.18f, w, h * 0.36f, w, centerY)
            arcTo(Rect(0f, centerY - radius, w, h), startAngleDegrees = 0f, sweepAngleDegrees = 35f, forceMoveTo = false)
        } else {
            cubicTo(w * 0.38f, h * 0.18f, 0f, h * 0.36f, 0f, centerY)
            arcTo(Rect(0f, centerY - radius, w, h), startAngleDegrees = 180f, sweepAngleDegrees = -35f, forceMoveTo = false)
        }
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

package com.projeto.wellio.ui.goals

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.projeto.wellio.ui.theme.DarkWellioColors
import kotlin.math.sin
import kotlin.random.Random

private val ConfettiColors = listOf(
    DarkWellioColors.water.accent,
    DarkWellioColors.food.accent,
    DarkWellioColors.exercise.accent,
    DarkWellioColors.base.accent,
    Color(0xFFFFD45C),
    Color(0xFFEC4899)
)

private class Piece(
    val x: Float,
    val delay: Float,
    val speed: Float,
    val sway: Float,
    val spin: Float,
    val width: Float,
    val height: Float,
    val color: Color
)

/**
 * Confete que cai pela tela uma vez (comemorações, SPEC §9.2): peças em dp, de tamanhos
 * variados, balançando e girando; [onDone] ao terminar.
 */
@Composable
fun Confetti(seed: Int, modifier: Modifier = Modifier, onDone: () -> Unit = {}) {
    val pieces = remember(seed) {
        val random = Random(seed)
        List(80) {
            Piece(
                x = random.nextFloat(),
                delay = random.nextFloat() * 0.3f,
                speed = 0.75f + random.nextFloat() * 0.6f,
                sway = 8f + random.nextFloat() * 18f,
                spin = random.nextFloat() * 900f - 450f,
                width = 7f + random.nextFloat() * 6f,
                height = 4f + random.nextFloat() * 4f,
                color = ConfettiColors[random.nextInt(ConfettiColors.size)]
            )
        }
    }
    val time = remember(seed) { Animatable(0f) }
    LaunchedEffect(seed) {
        time.animateTo(1f, tween(2800, easing = LinearEasing))
        onDone()
    }
    Canvas(modifier) {
        val t = time.value
        if (t >= 1f) return@Canvas
        pieces.forEach { piece ->
            val local = ((t - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val y = -24.dp.toPx() + (size.height + 48.dp.toPx()) * local * piece.speed
            val x = piece.x * size.width + sin(local * 9f + piece.x * 6f) * piece.sway.dp.toPx()
            val alpha = if (local > 0.85f) (1f - local) / 0.15f else 1f
            val w = piece.width.dp.toPx()
            val h = piece.height.dp.toPx()
            rotate(piece.spin * local, pivot = Offset(x, y)) {
                drawRect(piece.color.copy(alpha = alpha), topLeft = Offset(x - w / 2, y - h / 2), size = Size(w, h))
            }
        }
    }
}

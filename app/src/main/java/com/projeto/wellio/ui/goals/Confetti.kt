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
import com.projeto.wellio.ui.theme.DarkWellioColors
import kotlin.math.sin
import kotlin.random.Random

private val ConfettiColors = listOf(
    DarkWellioColors.water.accent,
    DarkWellioColors.food.accent,
    DarkWellioColors.exercise.accent,
    DarkWellioColors.base.accent,
    Color(0xFFFFD45C)
)

private class Piece(val x: Float, val delay: Float, val speed: Float, val sway: Float, val spin: Float, val color: Color)

/** Confete discreto que cai uma vez (comemoração de meta concluída, SPEC §9.2); [onDone] ao terminar. */
@Composable
fun Confetti(seed: Int, modifier: Modifier = Modifier, onDone: () -> Unit = {}) {
    val pieces = remember(seed) {
        val random = Random(seed)
        List(36) {
            Piece(
                x = random.nextFloat(),
                delay = random.nextFloat() * 0.35f,
                speed = 0.8f + random.nextFloat() * 0.6f,
                sway = random.nextFloat() * 24f,
                spin = random.nextFloat() * 720f - 360f,
                color = ConfettiColors[random.nextInt(ConfettiColors.size)]
            )
        }
    }
    val time = remember(seed) { Animatable(0f) }
    LaunchedEffect(seed) {
        time.animateTo(1f, tween(2200, easing = LinearEasing))
        onDone()
    }
    Canvas(modifier) {
        val t = time.value
        if (t >= 1f) return@Canvas
        pieces.forEach { piece ->
            val local = ((t - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val y = -12f + (size.height + 24f) * local * piece.speed
            val x = piece.x * size.width + sin(local * 10f) * piece.sway
            val alpha = if (local > 0.8f) (1f - local) / 0.2f else 1f
            rotate(piece.spin * local, pivot = Offset(x, y)) {
                drawRect(piece.color.copy(alpha = alpha), topLeft = Offset(x - 4f, y - 2.5f), size = Size(8f, 5f))
            }
        }
    }
}

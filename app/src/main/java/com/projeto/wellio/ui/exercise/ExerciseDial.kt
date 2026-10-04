package com.projeto.wellio.ui.exercise

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projeto.wellio.domain.exercise.MAX_EXERCISE_MINUTES
import com.projeto.wellio.domain.exercise.dialDegrees
import com.projeto.wellio.domain.exercise.dialMinutes
import com.projeto.wellio.domain.exercise.isDialMark
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** "45 min", "1 h" ou "1 h 15". */
fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "$minutes min"
        rest == 0 -> "$hours h"
        else -> "$hours h $rest"
    }
}

/**
 * Mostrador de relógio para a duração (SPEC §8.0): arrastar o ponteiro em volta marca os
 * minutos; uma volta são 60 min e as voltas se acumulam (anéis por dentro mostram as horas
 * completas). Vibra a cada 5 min. O TalkBack ajusta pelo valor (de 1 em 1 minuto).
 */
@Composable
fun ExerciseDial(minutes: Int, onMinutesChange: (Int) -> Unit, accent: Color, track: Color, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer()
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    val current by rememberUpdatedState(minutes)
    val onChange by rememberUpdatedState(onMinutesChange)
    // Ângulo acumulado do ponteiro durante o arrasto (várias voltas)
    var degrees by remember { mutableFloatStateOf(dialDegrees(minutes)) }

    Box(
        modifier
            .semantics {
                contentDescription = "Duração"
                progressBarRangeInfo = ProgressBarRangeInfo(minutes.toFloat(), 0f..MAX_EXERCISE_MINUTES.toFloat(), MAX_EXERCISE_MINUTES - 1)
                setProgress { value ->
                    onChange(value.roundToInt().coerceIn(0, MAX_EXERCISE_MINUTES))
                    true
                }
            }
            .pointerInput(Unit) {
                var lastAngle = 0f
                detectDragGestures(
                    onDragStart = { start ->
                        degrees = dialDegrees(current)
                        lastAngle = angleOf(start, size.width / 2f, size.height / 2f)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val angle = angleOf(change.position, size.width / 2f, size.height / 2f)
                        // Menor caminho entre os dois ângulos: passar pelo 12 continua a volta
                        var delta = angle - lastAngle
                        if (delta > 180f) delta -= 360f
                        if (delta < -180f) delta += 360f
                        lastAngle = angle
                        degrees = (degrees + delta).coerceIn(0f, dialDegrees(MAX_EXERCISE_MINUTES))
                        val next = dialMinutes(degrees)
                        if (next != current) {
                            if (isDialMark(next)) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onChange(next)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val radius = min(size.width, size.height) / 2f - 14.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            val stroke = 10.dp.toPx()
            // Marcas: a cada minuto; maiores a cada 5; números a cada 15
            for (m in 0 until 60) {
                val a = (m * 6f - 90f) * PI.toFloat() / 180f
                val major = m % 5 == 0
                val outer = radius + stroke / 2 + 10.dp.toPx()
                val inner = outer - (if (major) 6.dp else 3.dp).toPx()
                drawLine(
                    ink.copy(alpha = if (major) 0.7f else 0.3f),
                    Offset(center.x + inner * cos(a), center.y + inner * sin(a)),
                    Offset(center.x + outer * cos(a), center.y + outer * sin(a)),
                    strokeWidth = (if (major) 1.5.dp else 1.dp).toPx(),
                    cap = StrokeCap.Round
                )
            }
            listOf(0 to "60", 15 to "15", 30 to "30", 45 to "45").forEach { (m, label) ->
                val a = (m * 6f - 90f) * PI.toFloat() / 180f
                val text = measurer.measure(label, TextStyle(color = ink, fontSize = 11.sp))
                val r = radius - stroke - 12.dp.toPx()
                drawText(text, topLeft = Offset(center.x + r * cos(a) - text.size.width / 2, center.y + r * sin(a) - text.size.height / 2))
            }
            drawCircle(track, radius, center, style = Stroke(stroke))
            // Horas completas: anéis finos por dentro
            val hours = minutes / 60
            repeat(hours.coerceAtMost(5)) { i ->
                drawCircle(accent.copy(alpha = 0.5f), radius - stroke - 4.dp.toPx() - i * 5.dp.toPx(), center, style = Stroke(2.dp.toPx()))
            }
            val sweep = if (minutes > 0 && minutes % 60 == 0) 360f else (minutes % 60) * 6f
            drawArc(
                accent, -90f, sweep, false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            // Ponteiro (bolinha) na ponta do arco
            val a = (sweep - 90f) * PI.toFloat() / 180f
            val knob = Offset(center.x + radius * cos(a), center.y + radius * sin(a))
            drawCircle(Color.White, stroke * 0.95f, knob)
            drawCircle(accent, stroke * 0.65f, knob)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (minutes < 60) "$minutes" else formatDuration(minutes),
                style = if (minutes < 60) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (minutes < 60) Text("min", style = MaterialTheme.typography.titleMedium, color = ink)
        }
    }
}

/** Ângulo (graus, 0 = meio-dia, sentido horário) do ponto em relação ao centro. */
private fun angleOf(point: Offset, cx: Float, cy: Float): Float {
    val degrees = Math.toDegrees(atan2((point.y - cy).toDouble(), (point.x - cx).toDouble())).toFloat() + 90f
    return (degrees + 360f) % 360f
}

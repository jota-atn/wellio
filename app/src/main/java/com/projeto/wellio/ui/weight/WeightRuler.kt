package com.projeto.wellio.ui.weight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.projeto.wellio.domain.weight.MAX_WEIGHT_KG
import com.projeto.wellio.domain.weight.MIN_WEIGHT_KG
import kotlin.math.roundToInt

private val TickSpacing = 10.dp
private val TickCount = ((MAX_WEIGHT_KG - MIN_WEIGHT_KG) * 10).roundToInt() + 1

private fun kgAt(index: Int): Double = (MIN_WEIGHT_KG * 10 + index) / 10.0
private fun indexOf(kg: Double): Int = ((kg - MIN_WEIGHT_KG) * 10).roundToInt().coerceIn(0, TickCount - 1)

/**
 * Régua de balança (SPEC §2.9): arrastar escolhe o peso de 0,1 em 0,1 kg, com marca
 * maior a cada 1 kg e vibração a cada 0,5 kg (mais forte a cada 1 kg). O ajuste acessível fica nos botões
 * −/+ e no campo de texto do sheet; a régua em si é ignorada pelo TalkBack.
 */
@Composable
fun WeightRuler(
    value: Double,
    onValueChange: (Double) -> Unit,
    tick: Color,
    indicator: Color,
    modifier: Modifier = Modifier
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = indexOf(value))
    val haptics = LocalHapticFeedback.current
    val currentOnChange by rememberUpdatedState(onValueChange)
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = tick)
    val spacingPx = with(LocalDensity.current) { TickSpacing.toPx() }

    // Marca no centro: com o padding lateral, é a primeira visível (mais o quanto já passou dela)
    val centered by remember {
        derivedStateOf { state.firstVisibleItemIndex + (state.firstVisibleItemScrollOffset / spacingPx).roundToInt() }
    }

    // Enquanto a régua anda sozinha até um valor vindo de fora, ela não reporta mudanças
    val following = remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        snapshotFlow { centered }.collect { index ->
            if (state.isScrollInProgress && !following.value) {
                val tenth = MIN_WEIGHT_KG.roundToInt() * 10 + index
                when {
                    tenth % 10 == 0 -> haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    tenth % 5 == 0 -> haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                }
                currentOnChange(kgAt(index.coerceIn(0, TickCount - 1)))
            }
        }
    }
    // Valor mudado por fora (botões −/+, texto): a régua acompanha
    LaunchedEffect(value) {
        val target = indexOf(value)
        if (target == centered || state.isScrollInProgress) return@LaunchedEffect
        following.value = true
        try {
            state.animateScrollToItem(target)
        } finally {
            following.value = false
        }
    }

    BoxWithConstraints(modifier.height(72.dp).clearAndSetSemantics { }) {
        val sidePadding = maxWidth / 2 - TickSpacing / 2
        LazyRow(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state),
            contentPadding = PaddingValues(horizontal = sidePadding),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(TickCount) { index ->
                val tenth = MIN_WEIGHT_KG.roundToInt() * 10 + index
                Box(
                    Modifier
                        .width(TickSpacing)
                        .fillMaxHeight()
                        .drawBehind {
                            val major = tenth % 10 == 0
                            val half = tenth % 5 == 0
                            val length = size.height * when {
                                major -> 0.5f
                                half -> 0.36f
                                else -> 0.24f
                            }
                            drawLine(
                                color = if (major) tick else tick.copy(alpha = 0.5f),
                                start = Offset(size.width / 2, 0f),
                                end = Offset(size.width / 2, length),
                                strokeWidth = (if (major) 2.dp else 1.dp).toPx(),
                                cap = StrokeCap.Round
                            )
                            if (major) {
                                val label = measurer.measure("${tenth / 10}", labelStyle)
                                drawText(
                                    label,
                                    topLeft = Offset(
                                        size.width / 2 - label.size.width / 2,
                                        size.height - label.size.height
                                    )
                                )
                            }
                        }
                )
            }
        }
        // Agulha fixa no centro
        Canvas(Modifier.align(Alignment.TopCenter).width(4.dp).height(44.dp)) {
            drawLine(indicator, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), size.width, StrokeCap.Round)
        }
    }
}

package com.projeto.wellio.ui.goals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.projeto.wellio.ui.theme.DarkWellioColors
import kotlinx.coroutines.delay

/** Uma comemoração em andamento: o aviso e a cor dele. */
@Stable
class CelebrationState {
    var message by mutableStateOf<String?>(null)
        private set
    var color by mutableStateOf(DarkWellioColors.food.accent)
        private set
    var seed by mutableIntStateOf(0)
        private set

    /** Solta o confete e mostra [message] no topo. */
    fun celebrate(message: String, color: Color) {
        this.message = message
        this.color = color
        seed++
    }

    internal fun dismiss() {
        message = null
    }
}

@Composable
fun rememberCelebration(): CelebrationState = remember { CelebrationState() }

/**
 * Por cima da tela (SPEC §9.2): confete de verdade caindo e um aviso que desce do topo e
 * some sozinho, com uma vibração. O TalkBack lê o aviso.
 */
@Composable
fun CelebrationOverlay(state: CelebrationState, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(state.seed) {
        if (state.seed == 0) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        visible = true
        delay(2800)
        visible = false
        delay(400)
        state.dismiss()
    }
    Box(modifier.fillMaxSize()) {
        if (state.seed > 0) Confetti(state.seed, Modifier.fillMaxSize())
        AnimatedVisibility(
            visible = visible && state.message != null,
            enter = slideInVertically(spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)) { -it * 2 },
            exit = slideOutVertically { -it * 2 } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp, start = 24.dp, end = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = state.color,
                contentColor = Color(0xFF101418),
                shadowElevation = 8.dp,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            ) {
                Text(
                    state.message.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }
}

/**
 * Comemora quando um registro feito na tela faz o total do dia passar da meta (SPEC §9.2):
 * [additions] conta esses registros; abrir a tela ou trocar de dia não conta. [onReached]
 * decide se já comemorou hoje.
 */
@Composable
fun CelebrateWhenGoalReached(total: Int, goal: Int, additions: Int, isToday: Boolean, onReached: () -> Unit) {
    val previous = remember { mutableIntStateOf(total) }
    val handled = remember { mutableIntStateOf(additions) }
    // O registro conta na hora do toque, mas o total só muda quando o banco responde:
    // o toque deixa pendente e a próxima mudança do total decide
    val pending = remember { mutableStateOf(false) }
    LaunchedEffect(additions) {
        if (additions > handled.intValue) {
            handled.intValue = additions
            pending.value = true
        }
    }
    LaunchedEffect(total, goal) {
        if (pending.value && total != previous.intValue) {
            if (isToday && goal > 0 && previous.intValue < goal && total >= goal) onReached()
            pending.value = false
        }
        previous.intValue = total
    }
}

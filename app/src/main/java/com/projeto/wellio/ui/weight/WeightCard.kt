package com.projeto.wellio.ui.weight

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.projeto.wellio.AppContainer
import com.projeto.wellio.WellioApp
import com.projeto.wellio.data.time.todayFlow
import com.projeto.wellio.domain.model.WeightEntry
import com.projeto.wellio.domain.weight.WeightGoal
import com.projeto.wellio.domain.weight.WeightPeriod
import com.projeto.wellio.domain.weight.latestWeight
import com.projeto.wellio.domain.weight.remainingKg
import com.projeto.wellio.domain.weight.weekChange
import com.projeto.wellio.domain.weight.weightTrend
import com.projeto.wellio.ui.format.formatKg
import com.projeto.wellio.domain.home.CardSize
import com.projeto.wellio.ui.module.weightSummary
import com.projeto.wellio.ui.navigation.sharedContainer
import com.projeto.wellio.ui.navigation.sharedHero
import com.projeto.wellio.ui.theme.NeedleRed
import com.projeto.wellio.ui.theme.WellioTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import kotlin.math.abs

data class WeightCardState(
    val today: LocalDate,
    val entries: List<WeightEntry> = emptyList(),
    val goal: WeightGoal? = null
) {
    val currentKg: Double? get() = latestWeight(entries)?.weightKg
    val weekChangeKg: Double? get() = weekChange(entries, today)
}

class WeightCardViewModel(container: AppContainer) : ViewModel() {
    private val initialDay = LocalDate.now(container.clock)

    val state: StateFlow<WeightCardState> = combine(
        todayFlow(container.clock),
        container.weightRepository.entries,
        container.goalsStore.weightGoal,
        ::WeightCardState
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightCardState(initialDay))

    companion object {
        val Factory = viewModelFactory {
            initializer { WeightCardViewModel((this[APPLICATION_KEY] as WellioApp).container) }
        }
    }
}

/** Card de Peso na Home (SPEC §5.1): busca os próprios dados. */
@Composable
fun WeightHomeCard(
    size: CardSize,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeightCardViewModel = viewModel(factory = WeightCardViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WeightCardContent(state, size, onOpen, modifier)
}

/**
 * Inteiro: visor da balança com o peso e a variação na semana; ao lado, a mini-curva de
 * 30 dias e o alvo. Metade: visor pequeno, peso e a seta da semana.
 */
@Composable
fun WeightCardContent(state: WeightCardState, size: CardSize, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = WellioTheme.colors.base
    val current = state.currentKg
    val change = state.weekChangeKg
    // A fita gira até o peso quando o card aparece, como na balança da tela
    val shown = remember { Animatable(((current ?: 70.0) - 3).toFloat()) }
    LaunchedEffect(current) {
        current?.let { shown.animateTo(it.toFloat(), spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessVeryLow)) }
    }

    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(28.dp),
        color = colors.container,
        contentColor = colors.onContainer,
        modifier = modifier
            .sharedContainer("card-weight", RoundedCornerShape(28.dp))
            .clearAndSetSemantics {
                contentDescription = weightSummary(current, change, state.goal).spoken
            }
    ) {
        val tape: @Composable (Int) -> Unit = { height ->
            ScaleTape(
                valueKg = shown.value,
                tick = colors.onContainer,
                needle = NeedleRed,
                compact = true,
                modifier = Modifier
                    .sharedHero("hero-weight")
                    .fillMaxWidth()
                    .height(height.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.28f))
            )
        }
        val weightText = current?.let(::formatKg) ?: "—"
        when (size) {
            CardSize.Full -> Row(Modifier.fillMaxSize().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    tape(40)
                    Spacer(Modifier.height(8.dp))
                    Text(weightText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(
                        change?.let { "${arrow(it)} ${formatKg(abs(it))} na semana" } ?: "Peso",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onContainer.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    val trend = weightTrend(state.entries, state.today, WeightPeriod.Month)
                    if (trend.size >= 2) {
                        MiniTrend(trend, targetKg = state.goal?.targetKg, line = colors.accent, modifier = Modifier.weight(1f).fillMaxWidth())
                    } else {
                        // Sem curva ainda: diz o que falta, em vez de deixar o espaço vazio
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                "Registre mais pesagens para ver a curva",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onContainer.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        targetText(state.goal, current) ?: "últimos 30 dias",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            CardSize.Half -> Column(Modifier.fillMaxSize().padding(12.dp)) {
                tape(36)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(weightText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                    change?.let {
                        Spacer(Modifier.width(6.dp))
                        Text(arrow(it), style = MaterialTheme.typography.titleMedium, color = colors.accent, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    change?.let { "${formatKg(abs(it))} na semana" } ?: "Peso",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onContainer.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}

private fun arrow(changeKg: Double) = when {
    changeKg < 0 -> "↓"
    changeKg > 0 -> "↑"
    else -> "="
}

private fun targetText(goal: WeightGoal?, currentKg: Double?): String? {
    if (goal == null || currentKg == null) return null
    val remaining = remainingKg(goal, currentKg)
    return if (remaining == 0.0) "alvo ${formatKg(goal.targetKg)} · chegou!" else "alvo ${formatKg(goal.targetKg)} · faltam ${formatKg(remaining)}"
}

/** Curva compacta (sem toque): linha suave, área em degradê, ponto atual e o alvo tracejado. */
@Composable
private fun MiniTrend(trend: List<WeightEntry>, targetKg: Double?, line: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (trend.size < 2) return@Canvas
        val inset = 4.dp.toPx()
        val firstDay = trend.first().date.toEpochDay()
        val spanDays = (trend.last().date.toEpochDay() - firstDay).coerceAtLeast(1)
        val weights = trend.map { it.weightKg } + listOfNotNull(targetKg)
        val minKg = weights.min() - 0.2
        val maxKg = weights.max() + 0.2
        fun yOf(kg: Double) = inset + (size.height - 2 * inset) * (1 - ((kg - minKg) / (maxKg - minKg)).toFloat())
        val points = trend.map { Offset(inset + (size.width - 2 * inset) * (it.date.toEpochDay() - firstDay) / spanDays, yOf(it.weightKg)) }

        targetKg?.let {
            val y = yOf(it)
            drawLine(line.copy(alpha = 0.5f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())))
        }
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
        drawPath(area, Brush.verticalGradient(listOf(line.copy(alpha = 0.3f), Color.Transparent)))
        drawPath(curve, line, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(line, 3.5.dp.toPx(), points.last())
    }
}

/** Registrar peso pelo FAB da Home: o mesmo sheet com a régua da tela de Peso. */
@Composable
fun WeightRegister(
    onClose: () -> Unit,
    viewModel: WeightViewModel = viewModel(factory = WeightViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // O sheet guarda o peso inicial da régua: recomeça quando o peso de verdade chega do banco
    key(state.currentKg) {
        WeightSheet(
            entry = null,
            suggestedKg = state.currentKg,
            today = state.today,
            onDismiss = onClose,
            onSave = { day, kg ->
                viewModel.save(day, kg)
                onClose()
            },
            onDelete = null
        )
    }
}

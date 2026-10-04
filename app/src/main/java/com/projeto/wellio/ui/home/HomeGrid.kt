package com.projeto.wellio.ui.home

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.projeto.wellio.domain.home.CardSize
import com.projeto.wellio.domain.home.HomeCard
import com.projeto.wellio.domain.home.hideCard
import com.projeto.wellio.domain.home.homeRows
import com.projeto.wellio.domain.home.moveCard
import com.projeto.wellio.domain.home.toggleSize
import com.projeto.wellio.ui.icons.WellioIcons
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.sin

/** Retângulo de cada card (na ordem de [cards]) dentro do grid, seguindo as linhas da SPEC §5.1. */
internal fun cardRects(cards: List<HomeCard>, width: Float, rowHeight: Float, gutter: Float): List<Rect> {
    val half = (width - gutter) / 2
    val byKey = mutableMapOf<HomeCard, Rect>()
    homeRows(cards).forEachIndexed { row, cardsInRow ->
        val top = row * (rowHeight + gutter)
        cardsInRow.forEachIndexed { column, card ->
            val left = if (column == 0) 0f else half + gutter
            val cardWidth = if (card.size == CardSize.Full) width else half
            byKey[card] = Rect(left, top, left + cardWidth, top + rowHeight)
        }
    }
    return cards.map { byKey.getValue(it) }
}

/** Altura total do grid com [cards]. */
internal fun gridHeight(cards: List<HomeCard>, rowHeight: Float, gutter: Float): Float {
    val rows = homeRows(cards).size
    return if (rows == 0) 0f else rows * rowHeight + (rows - 1) * gutter
}

/**
 * Cards da Home num layout único, para a troca de posição e de tamanho animar (SPEC §5.2).
 *
 * Fora da edição, segurar um card chama [onLongPress]. Na edição ([editing]), os cards
 * tremem, ganham os selos ⊖ (ocultar) e ⇔ (tamanho), e arrastar de qualquer ponto move o
 * card; [onChange] recebe o layout novo a cada mudança.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeGrid(
    cards: List<HomeCard>,
    editing: Boolean,
    rowHeight: Dp,
    gutter: Dp,
    onLongPress: () -> Unit,
    onChange: (List<HomeCard>) -> Unit,
    cardTitle: (HomeCard) -> String,
    modifier: Modifier = Modifier,
    card: @Composable (HomeCard, Modifier) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf<String?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var grabOffset by remember { mutableStateOf(Offset.Zero) }
    // Depois de trocar com um card, só troca de novo com ele quando o dedo sair de cima dele
    var lastSwap by remember { mutableStateOf<String?>(null) }
    // Card solto voltando para o lugar com mola
    var settling by remember { mutableStateOf<String?>(null) }
    val drop = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val latestCards by rememberUpdatedState(cards)
    var gridWidth by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val rowPx = with(density) { rowHeight.toPx() }
    val gutterPx = with(density) { gutter.toPx() }

    /** Dedo em [finger] (relativo ao lugar do card arrastado): troca com o card que estiver embaixo. */
    fun swapUnderFinger() {
        val current = latestCards
        val from = current.indexOfFirst { it.key == dragging }
        if (from < 0) return
        val rects = cardRects(current, gridWidth, rowPx, gutterPx)
        val finger = rects[from].topLeft + grabOffset + dragPosition
        val under = rects.indices.firstOrNull { it != from && rects[it].contains(finger) }
        if (under == null) {
            lastSwap = null
            return
        }
        if (current[under].key == lastSwap) return
        lastSwap = current[under].key
        val moved = moveCard(current, from, under)
        // O deslocamento é relativo ao lugar do card; o lugar mudou, o dedo não
        dragPosition += rects[from].topLeft - cardRects(moved, gridWidth, rowPx, gutterPx)[under].topLeft
        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        onChange(moved)
    }

    val wiggle by rememberInfiniteTransition(label = "tremer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 260, easing = LinearEasing)),
        label = "fase"
    )

    LookaheadScope {
        Layout(
            modifier = modifier,
            content = {
                cards.forEachIndexed { index, homeCard ->
                    key(homeCard.key) {
                        val isDragged = dragging == homeCard.key
                        val isSettling = settling == homeCard.key
                        val boundsAnimation = if (isDragged || isSettling) Modifier else Modifier.animateBounds(this@LookaheadScope)
                        Box(
                            boundsAnimation
                                .zIndex(if (isDragged || isSettling) 1f else 0f)
                                .graphicsLayer {
                                    if (editing && !isDragged) {
                                        // Tremidinha, cada card numa fase diferente
                                        rotationZ = 0.7f * sin((wiggle + index * 0.37f) * 2 * PI.toFloat())
                                    }
                                    if (isDragged) {
                                        // Card "levantado" enquanto é arrastado
                                        scaleX = 1.04f
                                        scaleY = 1.04f
                                        shadowElevation = 12.dp.toPx()
                                    }
                                }
                        ) {
                            card(homeCard, Modifier.matchParentSize().then(if (editing) Modifier else Modifier.longPressToEdit(onLongPress)))
                            if (editing) {
                                EditOverlay(
                                    title = cardTitle(homeCard),
                                    size = homeCard.size,
                                    canMoveBack = index > 0,
                                    canMoveForward = index < cards.lastIndex,
                                    onHide = { onChange(hideCard(latestCards, latestCards.indexOf(homeCard))) },
                                    onToggleSize = { onChange(toggleSize(latestCards, latestCards.indexOf(homeCard))) },
                                    onMove = { delta ->
                                        val from = latestCards.indexOf(homeCard)
                                        onChange(moveCard(latestCards, from, from + delta))
                                    },
                                    modifier = Modifier
                                        .matchParentSize()
                                        // Toque no card durante a edição não faz nada (nem sai da edição)
                                        .pointerInput(Unit) { detectTapGestures { } }
                                        .pointerInput(homeCard.key) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                dragging = homeCard.key
                                                grabOffset = offset
                                                dragPosition = Offset.Zero
                                                lastSwap = null
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragPosition += amount
                                                swapUnderFinger()
                                            },
                                            onDragEnd = {
                                                settling = dragging
                                                dragging = null
                                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                                scope.launch {
                                                    drop.snapTo(dragPosition)
                                                    drop.animateTo(Offset.Zero, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)) {
                                                        dragPosition = value
                                                    }
                                                    settling = null
                                                }
                                            },
                                            onDragCancel = {
                                                dragging = null
                                                settling = null
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        ) { measurables, constraints ->
            val width = constraints.maxWidth.toFloat()
            gridWidth = width
            val rects = cardRects(cards, width, rowPx, gutterPx)
            // O card arrastado (ou voltando ao lugar) fica deslocado até onde está o dedo
            val shiftedIndex = cards.indexOfFirst { it.key == (dragging ?: settling) }

            val placeables = measurables.mapIndexed { index, measurable ->
                val rect = rects[index]
                measurable.measure(Constraints.fixed(rect.width.toInt(), rect.height.toInt()))
            }
            layout(constraints.maxWidth, gridHeight(cards, rowPx, gutterPx).toInt()) {
                placeables.forEachIndexed { index, placeable ->
                    val rect = rects[index]
                    val shift = if (index == shiftedIndex) dragPosition else Offset.Zero
                    placeable.place((rect.left + shift.x).toInt(), (rect.top + shift.y).toInt())
                }
            }
        }
    }
}

/**
 * Por cima do card na edição: segura os toques (o card não abre nem registra), mostra os
 * selos ⊖ e ⇔ e oferece ao TalkBack mover, mudar o tamanho e ocultar sem arrastar (SPEC §2.8).
 */
@Composable
private fun EditOverlay(
    title: String,
    size: CardSize,
    canMoveBack: Boolean,
    canMoveForward: Boolean,
    onHide: () -> Unit,
    onToggleSize: () -> Unit,
    onMove: (Int) -> Unit,
    modifier: Modifier
) {
    val sizeLabel = if (size == CardSize.Full) "inteiro" else "metade"
    Box(
        modifier.semantics {
            contentDescription = "$title, $sizeLabel"
            customActions = listOfNotNull(
                if (canMoveBack) CustomAccessibilityAction("Mover para antes") { onMove(-1); true } else null,
                if (canMoveForward) CustomAccessibilityAction("Mover para depois") { onMove(1); true } else null,
                CustomAccessibilityAction(if (size == CardSize.Full) "Deixar metade" else "Deixar inteiro") { onToggleSize(); true },
                CustomAccessibilityAction("Ocultar") { onHide(); true }
            )
        }
    ) {
        Badge(
            onClick = onHide,
            description = "Ocultar $title",
            modifier = Modifier.align(Alignment.TopStart).offset((-6).dp, (-6).dp),
            error = true
        ) { Icon(WellioIcons.Minus, contentDescription = null, modifier = Modifier.size(16.dp)) }
        Badge(
            onClick = onToggleSize,
            description = if (size == CardSize.Full) "Deixar $title metade" else "Deixar $title inteiro",
            modifier = Modifier.align(Alignment.TopEnd).offset(6.dp, (-6).dp),
            error = false
        ) { Icon(WellioIcons.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp)) }
    }
}

@Composable
private fun Badge(onClick: () -> Unit, description: String, modifier: Modifier, error: Boolean, icon: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (error) scheme.error else scheme.inverseSurface,
        contentColor = if (error) scheme.onError else scheme.inverseOnSurface,
        shadowElevation = 3.dp,
        modifier = modifier
            .size(28.dp)
            .semantics { contentDescription = description }
    ) {
        Box(contentAlignment = Alignment.Center) { icon() }
    }
}

/**
 * Segurar o card entra no modo de edição (SPEC §5.2). Olha os toques antes do card
 * (passe Initial): depois do toque longo, consome o resto do gesto para o card não abrir
 * a tela ao soltar. Mover o dedo (rolar a Home) cancela.
 */
private fun Modifier.longPressToEdit(onLongPress: () -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        // Termina antes do tempo (soltou ou moveu): não é toque longo
        val endedEarly = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var ended = false
            while (!ended) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
                ended = change == null || !change.pressed ||
                    (change.position - down.position).getDistance() > viewConfiguration.touchSlop
            }
            true
        }
        if (endedEarly == null) {
            onLongPress()
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        }
    }
}

package com.projeto.wellio.ui.home

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onLongClick
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
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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

/** O que o editor de um card pode fazer (SPEC §5.2). */
enum class QuickAction { Hide, ToggleSize }

/**
 * Cards da Home num layout único, para a troca de posição e de tamanho animar (SPEC §5.2).
 *
 * Fora da edição, segurar um card e soltar abre o editor daquele card, como nos widgets do
 * Android: moldura com uma alça para esticar/encolher (inteiro/metade) e Ocultar
 * ([onQuickAction]); os outros escurecem. Segurar e arrastar entra na edição
 * ([onStartEditing]) já levando o card. Na edição
 * ([editing]), os cards tremem, ganham os selos ⊖ e ⇔, e arrastar de qualquer ponto move
 * o card; [onChange] recebe o layout novo a cada mudança.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeGrid(
    cards: List<HomeCard>,
    editing: Boolean,
    rowHeight: Dp,
    gutter: Dp,
    onStartEditing: () -> Unit,
    onChange: (List<HomeCard>) -> Unit,
    onQuickAction: (key: String, QuickAction) -> Unit,
    onCardEditorChange: (open: Boolean) -> Unit,
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
    // Card com o editor aberto (segurar e soltar)
    var focused by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = focused != null) { focused = null }
    // Entrar ou sair da edição da Home fecha o editor de card
    LaunchedEffect(editing) { focused = null }
    LaunchedEffect(focused) { onCardEditorChange(focused != null) }
    val latestCards by rememberUpdatedState(cards)
    val latestOnChange by rememberUpdatedState(onChange)
    // Os gestos são criados uma vez só (pointerInput(Unit)): o que eles chamam tem de ser sempre o atual
    val latestOnStartEditing by rememberUpdatedState(onStartEditing)
    val latestOnQuickAction by rememberUpdatedState(onQuickAction)
    val latestEditing by rememberUpdatedState(editing)
    var gridWidth by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val rowPx = with(density) { rowHeight.toPx() }
    val gutterPx = with(density) { gutter.toPx() }

    fun startDrag(key: String, offsetInCard: Offset) {
        dragging = key
        grabOffset = offsetInCard
        dragPosition = Offset.Zero
        lastSwap = null
    }

    /** Leva o card junto do dedo e troca de lugar com o card que estiver embaixo dele. */
    fun dragBy(amount: Offset) {
        dragPosition += amount
        val current = latestCards
        val from = current.indexOfFirst { it.key == dragging }
        if (from < 0 || gridWidth <= 0f) return
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
        latestOnChange(moved)
    }

    fun endDrag() {
        if (dragging == null) return
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
    }

    val wiggle by rememberInfiniteTransition(label = "tremer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 260, easing = LinearEasing)),
        label = "fase"
    )

    LookaheadScope {
        Layout(
            // Tocar no espaço entre os cards fecha o editor do card
            modifier = modifier.pointerInput(focused) {
                if (focused != null) detectTapGestures { focused = null }
            },
            content = {
                cards.forEachIndexed { index, homeCard ->
                    key(homeCard.key) {
                        val isDragged = dragging == homeCard.key
                        val isSettling = settling == homeCard.key
                        val boundsAnimation = if (isDragged || isSettling) Modifier else Modifier.animateBounds(this@LookaheadScope)
                        Box(
                            // O gesto de segurar vem primeiro: o Compose compara os modificadores por posição,
                            // e tirar o animateBounds do card arrastado recriaria o que vem depois dele,
                            // cancelando o gesto no meio (o card ficava travado)
                            Modifier
                                .holdToEdit(
                                    // Com o editor de um card aberto, segurar não pode roubar o arrasto da alça
                                    enabled = { !latestEditing && focused == null },
                                    onHold = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        focused = null
                                    },
                                    onHoldReleased = { focused = homeCard.key },
                                    onHoldDragStart = { offset ->
                                        latestOnStartEditing()
                                        startDrag(homeCard.key, offset)
                                    },
                                    onHoldDrag = ::dragBy,
                                    onHoldDragEnd = ::endDrag
                                )
                                .then(boundsAnimation)
                                .zIndex(if (isDragged || isSettling || focused == homeCard.key) 1f else 0f)
                                .graphicsLayer {
                                    // Editando um card fora da edição da Home: os outros escurecem
                                    if (!editing && focused != null && focused != homeCard.key) alpha = 0.35f
                                    // O card selecionado para de tremer, para a alça ficar fácil de pegar
                                    if (editing && !isDragged && focused != homeCard.key) {
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
                                .then(
                                    if (editing) Modifier
                                    else Modifier.semantics {
                                        onLongClick("Editar card") { focused = homeCard.key; true }
                                    }
                                )
                        ) {
                            card(homeCard, Modifier.matchParentSize())
                            if (editing) {
                                EditOverlay(
                                    title = cardTitle(homeCard),
                                    size = homeCard.size,
                                    canMoveBack = index > 0,
                                    canMoveForward = index < cards.lastIndex,
                                    onHide = { onChange(hideCard(latestCards, latestCards.indexOfFirst { it.key == homeCard.key })) },
                                    onToggleSize = { onChange(toggleSize(latestCards, latestCards.indexOfFirst { it.key == homeCard.key })) },
                                    onMove = { delta ->
                                        val from = latestCards.indexOfFirst { it.key == homeCard.key }
                                        onChange(moveCard(latestCards, from, from + delta))
                                    },
                                    modifier = Modifier
                                        .matchParentSize()
                                        // Tocar no card seleciona (moldura com a alça de tamanho), sem sair da edição
                                        .pointerInput(Unit) { detectTapGestures { focused = homeCard.key } }
                                        .pointerInput(homeCard.key) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    startDrag(homeCard.key, it)
                                                },
                                                onDrag = { change, amount ->
                                                    change.consume()
                                                    dragBy(amount)
                                                },
                                                onDragEnd = ::endDrag,
                                                onDragCancel = ::endDrag
                                            )
                                        }
                                )
                            }
                            if (focused == homeCard.key) {
                                val rect = cardRects(cards, gridWidth, rowPx, gutterPx).getOrNull(index)
                                ResizeOverlay(
                                    title = cardTitle(homeCard),
                                    size = homeCard.size,
                                    // A alça fica do lado livre: card da coluna da direita estica para a esquerda
                                    handleOnLeft = rect != null && rect.left > 0f,
                                    widthStepPx = (gridWidth + gutterPx) / 2,
                                    // Na edição da Home, o ⊖ e os toques no card já são da camada de edição
                                    standalone = !editing,
                                    onToggleSize = {
                                        if (latestEditing) latestOnChange(toggleSize(latestCards, latestCards.indexOfFirst { it.key == homeCard.key }))
                                        else latestOnQuickAction(homeCard.key, QuickAction.ToggleSize)
                                    },
                                    onHide = {
                                        focused = null
                                        latestOnQuickAction(homeCard.key, QuickAction.Hide)
                                    },
                                    modifier = Modifier.matchParentSize()
                                )
                            } else if (focused != null && !editing) {
                                // Tocar num card escurecido fecha o editor
                                Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { focused = null } })
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
 * Por cima do card na edição: segura os toques (o card não abre nem registra; tocar
 * seleciona), mostra o selo ⊖ por fora do canto e oferece ao TalkBack mover, mudar o
 * tamanho e ocultar sem arrastar (SPEC §2.8). O tamanho, com o dedo, é pela alça do card
 * selecionado, igual ao editor fora da edição.
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
            modifier = Modifier.align(Alignment.TopStart).offset((-4).dp, (-4).dp),
            error = true
        ) { Icon(WellioIcons.Minus, contentDescription = null, modifier = Modifier.size(12.dp)) }
    }
}

/** Fração do caminho entre metade e inteiro a partir da qual o card troca de tamanho ao soltar. */
private const val RESIZE_THRESHOLD = 0.35f

/** Velocidade (px/s) de um puxão rápido, que troca o tamanho mesmo antes do limite. */
private const val RESIZE_FLING_PX = 900f

/**
 * Editor de um card, como o redimensionamento dos widgets do Android: moldura em volta do
 * card e uma alça na lateral livre. Arrastar a alça estica ou encolhe a moldura junto com
 * o dedo (entre metade e inteiro; [widthStepPx] é a diferença entre os dois). Passar de
 * um terço do caminho vibra; ao soltar (ou num puxão rápido), o card encaixa no outro
 * tamanho com mola, senão volta. Fora da edição da Home ([standalone]), também traz o selo
 * ⊖ e segura os toques no card.
 */
@Composable
private fun ResizeOverlay(
    title: String,
    size: CardSize,
    handleOnLeft: Boolean,
    widthStepPx: Float,
    standalone: Boolean,
    onToggleSize: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val currentSize by rememberUpdatedState(size)
    val growsLeft by rememberUpdatedState(handleOnLeft)
    val step by rememberUpdatedState(widthStepPx)
    val toggleSize by rememberUpdatedState(onToggleSize)
    // Quanto a moldura passou do card: positivo estica (metade → inteiro), negativo encolhe
    val stretch = remember { Animatable(0f) }
    // Para onde o tamanho muda: metade só estica (+1), inteiro só encolhe (-1)
    fun towardOther() = if (currentSize == CardSize.Full) -1f else 1f
    fun range() = if (currentSize == CardSize.Full) -step..0f else 0f..step
    fun progress(value: Float) = value * towardOther() / step
    fun pastThreshold(value: Float) = progress(value) > RESIZE_THRESHOLD

    fun release(velocityX: Float) {
        val outward = velocityX * (if (growsLeft) -1 else 1) * towardOther()
        val toggle = pastThreshold(stretch.value) || (progress(stretch.value) > 0.08f && outward > RESIZE_FLING_PX)
        scope.launch {
            if (toggle) {
                // O card muda de tamanho (com mola, pelo animateBounds); a moldura passa a ser
                // relativa ao card novo e volta a zero junto
                val offset = stretch.value - towardOther() * step
                if (!pastThreshold(stretch.value)) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                toggleSize()
                stretch.snapTo(offset)
            }
            stretch.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    Box(
        modifier
            .then(
                // Fora da edição da Home, toques no card em edição não abrem a tela
                if (standalone) Modifier.pointerInput(Unit) { detectTapGestures { } } else Modifier
            )
            .drawWithContent {
                drawContent()
                val inset = 4.dp.toPx()
                val extra = stretch.value
                val left = if (growsLeft) -inset - extra else -inset
                drawRoundRect(
                    color = scheme.primary,
                    topLeft = Offset(left, -inset),
                    size = Size(this.size.width + 2 * inset + extra, this.size.height + 2 * inset),
                    cornerRadius = CornerRadius(32.dp.toPx()),
                    style = Stroke(2.5.dp.toPx())
                )
            }
            .then(
                if (!standalone) Modifier
                else Modifier.semantics {
                    contentDescription = "Editando $title, ${if (size == CardSize.Full) "inteiro" else "metade"}"
                    customActions = listOf(
                        CustomAccessibilityAction(if (size == CardSize.Full) "Deixar metade" else "Deixar inteiro") { onToggleSize(); true },
                        CustomAccessibilityAction("Ocultar") { onHide(); true }
                    )
                }
            )
    ) {
        Box(
            Modifier
                .align(if (handleOnLeft) Alignment.CenterStart else Alignment.CenterEnd)
                .graphicsLayer {
                    // A alça acompanha a borda da moldura
                    translationX = (if (growsLeft) -1 else 1) * (15.dp.toPx() + stretch.value)
                }
                .size(48.dp)
                .testTag("alca-tamanho")
                .pointerInput(Unit) {
                    val velocity = VelocityTracker()
                    // A alça anda junto com o dedo: a velocidade sai da soma dos movimentos, não da posição nela
                    var travelled = 0f
                    detectHorizontalDragGestures(
                        onDragStart = {
                            velocity.resetTracking()
                            travelled = 0f
                            scope.launch { stretch.stop() }
                        },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            travelled += amount
                            velocity.addPosition(change.uptimeMillis, Offset(travelled, 0f))
                            val before = stretch.value
                            val after = (before + if (growsLeft) -amount else amount).coerceIn(range())
                            if (pastThreshold(after) != pastThreshold(before)) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            }
                            scope.launch { stretch.snapTo(after) }
                        },
                        onDragEnd = { release(velocity.calculateVelocity().x) },
                        // Gesto interrompido decide pelo mesmo critério, em vez de sempre voltar
                        onDragCancel = { release(0f) }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = scheme.primary,
                border = BorderStroke(3.dp, scheme.surface),
                shadowElevation = 3.dp,
                modifier = Modifier.size(22.dp)
            ) {}
        }
        if (standalone) {
            Badge(
                onClick = onHide,
                description = "Ocultar $title",
                modifier = Modifier.align(Alignment.TopStart).offset((-4).dp, (-4).dp),
                error = true
            ) { Icon(WellioIcons.Minus, contentDescription = null, modifier = Modifier.size(12.dp)) }
        }
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
            .size(22.dp)
            .semantics { contentDescription = description }
    ) {
        Box(contentAlignment = Alignment.Center) { icon() }
    }
}

/**
 * Segurar o card (SPEC §5.2), olhando os toques antes do card (passe Initial) para ele não
 * abrir a tela ao soltar. Mover o dedo antes do tempo (rolar a Home) cancela. Depois do
 * toque longo: soltar sem mexer chama [onHoldReleased]; arrastar chama [onHoldDragStart]
 * e segue o dedo no mesmo gesto, sem precisar soltar e tocar de novo. Só começa um gesto
 * novo quando [enabled].
 */
private fun Modifier.holdToEdit(
    enabled: () -> Boolean,
    onHold: () -> Unit,
    onHoldReleased: () -> Unit,
    onHoldDragStart: (Offset) -> Unit,
    onHoldDrag: (Offset) -> Unit,
    onHoldDragEnd: () -> Unit
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (!enabled()) return@awaitEachGesture
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
        if (endedEarly != null) return@awaitEachGesture

        onHold()
        var dragging = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id }
            // Mede antes de consumir: depois de consumido, positionChange() devolve zero
            val moved = change?.positionChange() ?: Offset.Zero
            event.changes.forEach { it.consume() }
            if (change == null || !change.pressed) break
            if (!dragging && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                dragging = true
                onHoldDragStart(down.position)
                // Até aqui o card não andou: o deslocamento desde o toque vale inteiro
                onHoldDrag(change.position - down.position)
            } else if (dragging) {
                // O card anda junto com o dedo: positionChange já desconta isso (como no arrasto da edição)
                onHoldDrag(moved)
            }
        }
        if (dragging) onHoldDragEnd() else onHoldReleased()
    }
}

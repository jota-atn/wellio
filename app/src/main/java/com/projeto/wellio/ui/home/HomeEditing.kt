package com.projeto.wellio.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.projeto.wellio.domain.home.CardSize
import com.projeto.wellio.domain.home.HomeCard
import com.projeto.wellio.ui.icons.WellioIcons
import kotlinx.coroutines.delay

/** Barra do modo de edição (SPEC §5.2): "+ Adicionar card" e "Concluir". */
@Composable
fun EditBar(onAddCard: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onAddCard) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Adicionar card")
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onDone) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Concluir")
        }
    }
}

/**
 * Sugestão discreta nas linhas vazias do fim da Home (SPEC §5.1): tocar abre o catálogo;
 * o × dispensa de vez (volta em Configurações, M3).
 */
@Composable
fun AddCardSuggestion(height: Dp, onClick: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.background),
        modifier = modifier.fillMaxWidth().height(height)
    ) {
        Box(Modifier.fillMaxSize()) {
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text("Adicionar card", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Default.Close, contentDescription = "Dispensar sugestão", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Um card do catálogo: qual é e como se chama. */
data class CatalogEntry(val key: String, val title: String)

/**
 * Catálogo com prévia ao vivo (SPEC §5.2): cada card desenhado com os dados reais e um
 * seletor Inteiro/Metade; "Adicionar" põe no fim da Home no tamanho escolhido. Cards que
 * já estão na Home aparecem marcados.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogSheet(
    entries: List<CatalogEntry>,
    onHome: Set<String>,
    rowHeight: Dp,
    gutter: Dp,
    onDismiss: () -> Unit,
    onAdd: (HomeCard) -> Unit,
    card: @Composable (HomeCard, Modifier) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text(
            "Adicionar card",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            entries.forEach { entry ->
                var size by remember { mutableStateOf(CardSize.Full) }
                val added = entry.key in onHome
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        SingleChoiceSegmentedButtonRow {
                            CardSize.entries.forEachIndexed { index, option ->
                                SegmentedButton(
                                    selected = size == option,
                                    onClick = { size = option },
                                    shape = SegmentedButtonDefaults.itemShape(index, CardSize.entries.size),
                                    label = { Text(if (option == CardSize.Full) "Inteiro" else "Metade") }
                                )
                            }
                        }
                    }
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val width = if (size == CardSize.Full) maxWidth else (maxWidth - gutter) / 2
                        // Prévia: o card de verdade, sem reagir a toques
                        Box(
                            Modifier
                                .size(width, rowHeight)
                                .clearAndSetSemantics { }
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                                    }
                                }
                        ) {
                            card(HomeCard(entry.key, size), Modifier.fillMaxSize())
                        }
                    }
                    if (added) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text("Já está na Home", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        FilledTonalButton(onClick = { onAdd(HomeCard(entry.key, size)) }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Adicionar ${entry.title}")
                        }
                    }
                }
            }
        }
    }
}

/** Abre abaixo do card, alinhado à esquerda dele; se não couber, acima. */
private class QuickMenuPosition(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = anchorBounds.left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val below = anchorBounds.bottom + gapPx
        val y = if (below + popupContentSize.height <= windowSize.height) below
        else (anchorBounds.top - gapPx - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

/**
 * Leque de ações de um card (SPEC §5.2): segurar e soltar abre; as pílulas saem do card
 * uma de cada vez, com mola. Tocar fora fecha.
 */
@Composable
fun QuickMenu(size: CardSize, onDismiss: () -> Unit, onAction: (QuickAction) -> Unit) {
    val gap = with(LocalDensity.current) { 8.dp.roundToPx() }
    val scheme = MaterialTheme.colorScheme
    val items = listOf(
        Triple(QuickAction.Hide, "Ocultar", WellioIcons.Minus),
        Triple(QuickAction.ToggleSize, if (size == CardSize.Full) "Deixar metade" else "Deixar inteiro", WellioIcons.SwapHoriz),
        Triple(QuickAction.EditHome, "Editar Home", Icons.Default.Edit)
    )
    Popup(
        popupPositionProvider = remember(gap) { QuickMenuPosition(gap) },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEachIndexed { index, (action, label, icon) ->
                val shown = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    delay(index * 50L)
                    shown.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium))
                }
                Surface(
                    onClick = { onAction(action) },
                    shape = RoundedCornerShape(50),
                    color = if (action == QuickAction.Hide) scheme.errorContainer else scheme.inverseSurface,
                    contentColor = if (action == QuickAction.Hide) scheme.onErrorContainer else scheme.inverseOnSurface,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .padding(start = (index * 18).dp)
                        .graphicsLayer {
                            val t = shown.value
                            alpha = t.coerceIn(0f, 1f)
                            scaleX = 0.6f + 0.4f * t
                            scaleY = 0.6f + 0.4f * t
                            translationY = (1 - t) * -24.dp.toPx()
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

package com.projeto.wellio.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.projeto.wellio.domain.home.CardSize
import com.projeto.wellio.domain.home.HomeCard
import com.projeto.wellio.ui.navigation.LocalCardTransitionEnabled

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
                            // A prévia não pode virar a tela no lugar do card da Home
                            CompositionLocalProvider(LocalCardTransitionEnabled provides false) {
                                card(HomeCard(entry.key, size), Modifier.fillMaxSize())
                            }
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

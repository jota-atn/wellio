package com.projeto.wellio.ui.water

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
import com.projeto.wellio.domain.goals.progress
import com.projeto.wellio.domain.home.CardSize
import com.projeto.wellio.domain.model.WaterContainer
import com.projeto.wellio.domain.model.WaterEntry
import com.projeto.wellio.domain.water.lastEntry
import com.projeto.wellio.domain.water.quickButtons
import com.projeto.wellio.domain.water.totalMl
import com.projeto.wellio.ui.format.formatMl
import com.projeto.wellio.ui.icons.WellioIcons
import com.projeto.wellio.ui.icons.containerIconRes
import com.projeto.wellio.ui.navigation.sharedContainer
import com.projeto.wellio.ui.navigation.sharedHero
import com.projeto.wellio.ui.theme.WellioTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WaterCardState(
    val totalMl: Int = 0,
    val goalMl: Int = 0,
    val quickButtons: List<WaterContainer> = emptyList(),
    val lastEntry: WaterEntry? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class WaterCardViewModel(container: AppContainer) : ViewModel() {
    private val repository = container.waterRepository

    val state: StateFlow<WaterCardState> = combine(
        todayFlow(container.clock).flatMapLatest { repository.entries(it) },
        repository.containers,
        container.goalsStore.goals
    ) { entries, containers, goals ->
        WaterCardState(totalMl(entries), goals.water.daily, quickButtons(containers), lastEntry(entries))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WaterCardState())

    fun add(container: WaterContainer) {
        viewModelScope.launch { repository.add(container.amountMl, container.name) }
    }

    fun undoLast() {
        val last = state.value.lastEntry ?: return
        viewModelScope.launch { repository.delete(last.id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { WaterCardViewModel((this[APPLICATION_KEY] as WellioApp).container) }
        }
    }
}

/** Card de Água na Home (SPEC §5.1): busca os próprios dados. */
@Composable
fun WaterHomeCard(
    size: CardSize,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WaterCardViewModel = viewModel(factory = WaterCardViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Registros feitos pelo card: só eles fazem a gota transbordar
    var additions by rememberSaveable { mutableIntStateOf(0) }
    WaterCardContent(
        state = state,
        size = size,
        additions = additions,
        onOpen = onOpen,
        onQuickAdd = {
            additions++
            viewModel.add(it)
        },
        onUndo = viewModel::undoLast,
        modifier = modifier
    )
}

/**
 * Inteiro: gota, total/meta, barra, % e os recipientes rápidos com o desfazer.
 * Metade: gota, total/meta e o primeiro recipiente rápido.
 */
@Composable
fun WaterCardContent(
    state: WaterCardState,
    size: CardSize,
    additions: Int,
    onOpen: () -> Unit,
    onQuickAdd: (WaterContainer) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WellioTheme.colors.water
    val fraction = progress(state.totalMl, state.goalMl)
    val percent = (fraction * 100).toInt()
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(28.dp),
        color = colors.container,
        contentColor = colors.onContainer,
        modifier = modifier.sharedContainer("card-water", RoundedCornerShape(28.dp))
    ) {
        val numbers = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "Água: ${formatMl(state.totalMl)} de ${formatMl(state.goalMl)}, $percent por cento"
        }
        when (size) {
            CardSize.Full -> Row(
                Modifier.fillMaxSize().padding(start = 6.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WaterDrop(state.totalMl, state.goalMl, additions, Modifier.sharedHero("hero-water").size(width = 76.dp, height = 100.dp))
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Row(numbers, verticalAlignment = Alignment.Bottom) {
                        Text(formatMl(state.totalMl), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "de ${formatMl(state.goalMl)}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(bottom = 2.dp)
                        )
                        Text("$percent%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.accent)
                    }
                    Spacer(Modifier.height(8.dp))
                    ProgressBar(fraction)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        state.quickButtons.forEach { QuickButton(it, onQuickAdd) }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onUndo, enabled = state.lastEntry != null, modifier = Modifier.size(36.dp)) {
                            Icon(WellioIcons.Undo, contentDescription = "Desfazer último registro de água")
                        }
                    }
                }
            }
            CardSize.Half -> Column(Modifier.fillMaxSize().padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)) {
                Row(numbers, verticalAlignment = Alignment.CenterVertically) {
                    WaterDrop(state.totalMl, state.goalMl, additions, Modifier.sharedHero("hero-water").size(width = 52.dp, height = 68.dp))
                    Column {
                        Text(formatMl(state.totalMl), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("de ${formatMl(state.goalMl)}", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
                Spacer(Modifier.weight(1f))
                state.quickButtons.firstOrNull()?.let {
                    QuickButton(it, onQuickAdd, showName = true, modifier = Modifier.fillMaxWidth().padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float) {
    val colors = WellioTheme.colors.water
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(colors.track)
    ) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(colors.accent))
    }
}

/** Pílula de um recipiente: ícone + ml (e o nome, no card metade). Tocar registra. */
@Composable
private fun QuickButton(
    container: WaterContainer,
    onClick: (WaterContainer) -> Unit,
    modifier: Modifier = Modifier,
    showName: Boolean = false
) {
    val colors = WellioTheme.colors.water
    Surface(
        onClick = { onClick(container) },
        shape = RoundedCornerShape(50),
        color = colors.accent,
        contentColor = colors.onAccent,
        modifier = modifier
            .height(36.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Adicionar ${container.name}, ${formatMl(container.amountMl)}"
            }
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(painterResource(containerIconRes(container.iconKey)), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                if (showName) "${container.name} ${container.amountMl}" else "${container.amountMl}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Registrar água agora pelo FAB da Home: o mesmo sheet da tela de Água. */
@Composable
fun WaterRegister(
    onClose: () -> Unit,
    viewModel: WaterViewModel = viewModel(factory = WaterViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WaterEntrySheet(
        entry = null,
        containers = state.containers,
        zone = viewModel.zone,
        onDismiss = onClose,
        onSave = { amount, name, time ->
            viewModel.add(amount, name, time)
            onClose()
        },
        onDelete = null
    )
}

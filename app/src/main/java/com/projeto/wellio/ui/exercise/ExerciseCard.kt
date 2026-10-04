package com.projeto.wellio.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.projeto.wellio.domain.exercise.exerciseTotals
import com.projeto.wellio.domain.goals.progress
import com.projeto.wellio.domain.home.CardSize
import com.projeto.wellio.domain.model.ExerciseEntry
import com.projeto.wellio.domain.model.ExerciseType
import com.projeto.wellio.domain.model.ExerciseUnit
import com.projeto.wellio.domain.model.Goals
import com.projeto.wellio.ui.format.formatInt
import com.projeto.wellio.ui.icons.exerciseIconRes
import com.projeto.wellio.ui.navigation.sharedContainer
import com.projeto.wellio.ui.navigation.sharedHero
import com.projeto.wellio.ui.theme.WellioTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalTime

data class ExerciseCardState(
    val entries: List<ExerciseEntry> = emptyList(),
    val types: Map<String, ExerciseType> = emptyMap(),
    val goals: Goals = Goals.Default
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseCardViewModel(container: AppContainer) : ViewModel() {
    private val repository = container.exerciseRepository

    val state: StateFlow<ExerciseCardState> = combine(
        todayFlow(container.clock).flatMapLatest { repository.entries(it) },
        repository.types,
        container.goalsStore.goals
    ) { entries, types, goals -> ExerciseCardState(entries, types.associateBy { it.id }, goals) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseCardState())

    companion object {
        val Factory = viewModelFactory {
            initializer { ExerciseCardViewModel((this[APPLICATION_KEY] as WellioApp).container) }
        }
    }
}

/** Card de Exercício na Home (SPEC §8.0): busca os próprios dados; o + abre o registro. */
@Composable
fun ExerciseHomeCard(
    size: CardSize,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseCardViewModel = viewModel(factory = ExerciseCardViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var registering by remember { mutableStateOf(false) }
    // Registros feitos pelo card: só eles avivam a fogueira
    var additions by rememberSaveable { mutableIntStateOf(0) }
    ExerciseCardContent(state, size, additions, onOpen, onRegister = { registering = true }, modifier = modifier)
    if (registering) {
        ExerciseRegister(onClose = { registering = false }, onSaved = { additions++ })
    }
}

/**
 * Inteiro: fogueira, o valor na unidade da meta e o outro menor, barra, os treinos do dia e
 * o +. Metade: fogueira, valor/meta e o +. Tocar no card abre a tela de Exercício.
 */
@Composable
fun ExerciseCardContent(
    state: ExerciseCardState,
    size: CardSize,
    additions: Int,
    onOpen: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WellioTheme.colors.exercise
    val totals = exerciseTotals(state.entries)
    val unit = state.goals.exerciseUnit
    val goal = state.goals.exercise.daily
    val value = totals.toward(unit)
    val fraction = progress(value, goal)
    val main = if (unit == ExerciseUnit.Minutes) formatDuration(totals.minutes) else formatInt(totals.kcal)
    val goalText = if (unit == ExerciseUnit.Minutes) "de ${formatDuration(goal)}" else "de ${formatInt(goal)} kcal"
    val other = if (unit == ExerciseUnit.Minutes) "${formatInt(totals.kcal)} kcal" else formatDuration(totals.minutes)
    val spoken = "Exercício: $main $goalText, ${(fraction * 100).toInt()} por cento"

    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(28.dp),
        color = colors.container,
        contentColor = colors.onContainer,
        modifier = modifier.sharedContainer("card-exercise", RoundedCornerShape(28.dp))
    ) {
        val fire = @Composable { width: Int, height: Int ->
            Campfire(value, goal, additions, Modifier.sharedHero("hero-exercise").size(width = width.dp, height = height.dp))
        }
        val add = @Composable {
            FilledTonalIconButton(
                onClick = onRegister,
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                modifier = Modifier.size(36.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "Registrar exercício") }
        }
        when (size) {
            CardSize.Full -> Row(
                Modifier.fillMaxSize().padding(start = 4.dp, end = 14.dp, top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                fire(84, 104)
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Row(Modifier.semantics(mergeDescendants = true) { contentDescription = spoken }, verticalAlignment = Alignment.Bottom) {
                        Text(main, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Text(goalText, style = MaterialTheme.typography.bodySmall, maxLines = 1, modifier = Modifier.weight(1f).padding(bottom = 2.dp))
                        add()
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(colors.track)
                    ) {
                        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(colors.accent))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(other, style = MaterialTheme.typography.labelMedium)
                    // Treinos do dia: ícone + nome, sem repetir a mesma atividade
                    val today = state.entries.sortedByDescending { it.timestamp }
                        .mapNotNull { state.types[it.typeId] }
                        .distinctBy { it.name }
                    if (today.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            today.take(2).forEach { type ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                                    Icon(painterResource(exerciseIconRes(type)), contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(type.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            if (today.size > 2) Text("+${today.size - 2}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            CardSize.Half -> Column(Modifier.fillMaxSize().padding(start = 2.dp, end = 12.dp, top = 6.dp, bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    fire(60, 74)
                    Column(Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = spoken }) {
                        Text(main, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(goalText, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(start = 10.dp, end = 10.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(colors.track)
                    ) {
                        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(colors.accent))
                    }
                    add()
                }
            }
        }
    }
}

/** Registrar exercício (SPEC §8.0): o sheet com busca, nível, mostrador e kcal; usado pelo card e pelo FAB. */
@Composable
fun ExerciseRegister(
    onClose: () -> Unit,
    onSaved: () -> Unit = {},
    viewModel: ExerciseViewModel = viewModel(factory = ExerciseViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExerciseSheet(
        types = state.types,
        uses = state.uses,
        weightKg = state.weightKg,
        initial = null,
        editing = false,
        defaultTime = LocalTime.now(viewModel.zone),
        onDismiss = onClose,
        onSave = {
            viewModel.add(it)
            onSaved()
            onClose()
        },
        onDelete = null,
        onToggleFavorite = viewModel::toggleFavorite,
        onCreateCustom = viewModel::createCustom
    )
}

package com.projeto.wellio.ui.module

import com.projeto.wellio.domain.home.CardSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.projeto.wellio.AppContainer
import com.projeto.wellio.ui.exercise.ExerciseHomeCard
import com.projeto.wellio.ui.exercise.ExerciseRegister
import com.projeto.wellio.ui.food.FoodHomeCard
import com.projeto.wellio.ui.food.FoodRegister
import com.projeto.wellio.ui.water.WaterHomeCard
import com.projeto.wellio.ui.water.WaterRegister
import com.projeto.wellio.ui.weight.WeightHomeCard
import com.projeto.wellio.ui.weight.WeightRegister
import com.projeto.wellio.R
import com.projeto.wellio.data.time.todayFlow
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.domain.exercise.quickExercises
import com.projeto.wellio.domain.water.quickButtons
import com.projeto.wellio.domain.weight.latestWeight
import com.projeto.wellio.ui.exercise.formatDuration
import com.projeto.wellio.ui.format.formatMl
import com.projeto.wellio.ui.icons.containerIconRes
import com.projeto.wellio.ui.icons.exerciseIconRes
import kotlinx.coroutines.flow.map
import com.projeto.wellio.domain.weight.weekChange
import com.projeto.wellio.ui.navigation.ExerciseDestination
import com.projeto.wellio.ui.navigation.WaterDestination
import com.projeto.wellio.ui.navigation.WeightDestination
import com.projeto.wellio.ui.theme.WellioColors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

/** No máximo 4 ações de 1 toque por módulo, como os itens do FAB (SPEC §5.1). */
private const val MAX_ONE_TAP = 4

/** Os módulos do app, na ordem da seção Registros da sidebar (SPEC §2.2). */
object Modules {
    val all: List<WellioModule> = listOf(WaterModule, FoodModule, ExerciseModule, WeightModule)

    fun of(id: ModuleId): WellioModule = all.first { it.id == id }

    fun byKey(key: String): WellioModule? = all.firstOrNull { it.id.key == key }
}

@OptIn(ExperimentalCoroutinesApi::class)
object WaterModule : WellioModule {
    override val id = ModuleId.Water
    override val title = "Água"
    override val icon = R.drawable.ic_module_water
    override fun colors(palette: WellioColors) = palette.water
    override val goalType = GoalType.WATER
    override fun destination(fromCard: Boolean): Any = WaterDestination(fromCard)

    override fun summary(container: AppContainer): Flow<Summary> = combine(
        todayFlow(container.clock).flatMapLatest { container.waterRepository.totalMl(it..it) },
        container.goalsStore.goals
    ) { total, goals -> waterSummary(total, goals.water.daily) }

    @Composable
    override fun HomeCard(size: CardSize, onOpen: () -> Unit, modifier: Modifier) = WaterHomeCard(size, onOpen, modifier)

    /** Os recipientes rápidos (até 4). */
    override fun oneTapActions(container: AppContainer): Flow<List<OneTapAction>> =
        container.waterRepository.containers.map { containers ->
            quickButtons(containers, max = MAX_ONE_TAP).map { item ->
                OneTapAction("${item.name} ${formatMl(item.amountMl)}", containerIconRes(item.iconKey)) {
                    val id = container.waterRepository.add(item.amountMl, item.name)
                    suspend { container.waterRepository.delete(id) }
                }
            }
        }

    override val registerLabel = "Água"

    @Composable
    override fun Register(onClose: () -> Unit) = WaterRegister(onClose)
}

@OptIn(ExperimentalCoroutinesApi::class)
object FoodModule : WellioModule {
    override val id = ModuleId.Food
    override val title = "Alimentação"
    override val icon = R.drawable.ic_module_food
    override fun colors(palette: WellioColors) = palette.food
    override val goalType = GoalType.FOOD
    override fun destination(fromCard: Boolean): Any? = null

    override fun summary(container: AppContainer): Flow<Summary> = combine(
        todayFlow(container.clock).flatMapLatest { container.foodRepository.kcal(it..it) },
        container.goalsStore.goals
    ) { kcal, goals -> foodSummary(kcal, goals.food.daily) }

    // Sem tela ainda: tocar no card registra
    @Composable
    override fun HomeCard(size: CardSize, onOpen: () -> Unit, modifier: Modifier) = FoodHomeCard(size, modifier)

    override val registerLabel = "Alimento"

    @Composable
    override fun Register(onClose: () -> Unit) = FoodRegister(onClose)
}

@OptIn(ExperimentalCoroutinesApi::class)
object ExerciseModule : WellioModule {
    override val id = ModuleId.Exercise
    override val title = "Exercício"
    override val icon = R.drawable.ic_module_exercise
    override fun colors(palette: WellioColors) = palette.exercise
    override val goalType = GoalType.EXERCISE
    override fun destination(fromCard: Boolean): Any = ExerciseDestination(fromCard)

    override fun summary(container: AppContainer): Flow<Summary> = combine(
        todayFlow(container.clock).flatMapLatest { container.exerciseRepository.kcal(it..it) },
        todayFlow(container.clock).flatMapLatest { container.exerciseRepository.minutes(it..it) },
        container.goalsStore.goals
    ) { kcal, minutes, goals -> exerciseSummary(kcal, minutes, goals.exercise.daily, goals.exerciseUnit) }

    @Composable
    override fun HomeCard(size: CardSize, onOpen: () -> Unit, modifier: Modifier) = ExerciseHomeCard(size, onOpen, modifier)

    /** Os favoritos e recentes, no último nível e duração usados (até 4). */
    override fun oneTapActions(container: AppContainer): Flow<List<OneTapAction>> = combine(
        container.exerciseRepository.types,
        container.exerciseRepository.lastUses
    ) { types, uses ->
        quickExercises(types, uses, max = MAX_ONE_TAP).map { item ->
            OneTapAction("${item.type.name} · ${formatDuration(item.minutes)}", exerciseIconRes(item.type)) {
                val id = container.exerciseRepository.add(item.type, item.minutes)
                suspend { container.exerciseRepository.delete(id) }
            }
        }
    }

    override val registerLabel = "Exercício"

    @Composable
    override fun Register(onClose: () -> Unit) = ExerciseRegister(onClose)
}

object WeightModule : WellioModule {
    override val id = ModuleId.Weight
    override val title = "Peso"
    override val icon = R.drawable.ic_module_weight
    override fun colors(palette: WellioColors) = palette.base
    override val goalType: GoalType? = null
    override fun destination(fromCard: Boolean): Any = WeightDestination(fromCard)

    override fun summary(container: AppContainer): Flow<Summary> = combine(
        todayFlow(container.clock),
        container.weightRepository.entries,
        container.goalsStore.weightGoal
    ) { today, entries, goal ->
        weightSummary(latestWeight(entries)?.weightKg, weekChange(entries, today), goal)
    }

    @Composable
    override fun HomeCard(size: CardSize, onOpen: () -> Unit, modifier: Modifier) = WeightHomeCard(size, onOpen, modifier)

    override val registerLabel = "Peso"

    @Composable
    override fun Register(onClose: () -> Unit) = WeightRegister(onClose)
}

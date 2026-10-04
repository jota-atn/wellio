package com.projeto.wellio.ui.module

import com.projeto.wellio.AppContainer
import com.projeto.wellio.R
import com.projeto.wellio.data.time.todayFlow
import com.projeto.wellio.domain.weight.latestWeight
import com.projeto.wellio.domain.weight.weekChange
import com.projeto.wellio.ui.navigation.WaterDestination
import com.projeto.wellio.ui.navigation.WeightDestination
import com.projeto.wellio.ui.theme.WellioColors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

/** Os módulos do app, na ordem da seção Registros da sidebar (SPEC §2.2). */
object Modules {
    val all: List<WellioModule> = listOf(WaterModule, FoodModule, ExerciseModule, WeightModule)

    fun of(id: ModuleId): WellioModule = all.first { it.id == id }
}

@OptIn(ExperimentalCoroutinesApi::class)
object WaterModule : WellioModule {
    override val id = ModuleId.Water
    override val title = "Água"
    override val icon = R.drawable.ic_module_water
    override fun colors(palette: WellioColors) = palette.water
    override val destination: Any = WaterDestination

    override fun summary(container: AppContainer): Flow<Summary> = combine(
        todayFlow(container.clock).flatMapLatest { container.waterRepository.totalMl(it..it) },
        container.goalsStore.goals
    ) { total, goals -> waterSummary(total, goals.water.daily) }
}

@OptIn(ExperimentalCoroutinesApi::class)
object FoodModule : WellioModule {
    override val id = ModuleId.Food
    override val title = "Alimentação"
    override val icon = R.drawable.ic_module_food
    override fun colors(palette: WellioColors) = palette.food
    override val destination: Any? = null

    override fun summary(container: AppContainer): Flow<Summary> = combine(
        todayFlow(container.clock).flatMapLatest { container.foodRepository.kcal(it..it) },
        container.goalsStore.goals
    ) { kcal, goals -> foodSummary(kcal, goals.food.daily) }
}

@OptIn(ExperimentalCoroutinesApi::class)
object ExerciseModule : WellioModule {
    override val id = ModuleId.Exercise
    override val title = "Exercício"
    override val icon = R.drawable.ic_module_exercise
    override fun colors(palette: WellioColors) = palette.exercise
    override val destination: Any? = null

    override fun summary(container: AppContainer): Flow<Summary> = combine(
        todayFlow(container.clock).flatMapLatest { container.exerciseRepository.kcal(it..it) },
        container.goalsStore.goals
    ) { kcal, goals -> exerciseSummary(kcal, goals.exercise.daily) }
}

object WeightModule : WellioModule {
    override val id = ModuleId.Weight
    override val title = "Peso"
    override val icon = R.drawable.ic_module_weight
    override fun colors(palette: WellioColors) = palette.base
    override val destination: Any = WeightDestination

    override fun summary(container: AppContainer): Flow<Summary> = combine(
        todayFlow(container.clock),
        container.weightRepository.entries,
        container.goalsStore.weightGoal
    ) { today, entries, goal ->
        weightSummary(latestWeight(entries)?.weightKg, weekChange(entries, today), goal)
    }
}

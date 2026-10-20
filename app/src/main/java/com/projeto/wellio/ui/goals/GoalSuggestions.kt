package com.projeto.wellio.ui.goals

import com.projeto.wellio.AppContainer
import com.projeto.wellio.data.time.todayFlow
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.domain.profile.CalorieSuggestion
import com.projeto.wellio.domain.profile.objective
import com.projeto.wellio.domain.profile.suggestedCalories
import com.projeto.wellio.domain.water.suggestedWaterGoal
import com.projeto.wellio.domain.weight.latestWeight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Sugestões do sheet de metas: água por 35 ml/kg e kcal pelo perfil (SPEC §6, §9.1). */
fun goalSuggestions(container: AppContainer): Flow<Map<GoalType, Int>> = combine(
    container.profileStore.profile,
    container.weightRepository.entries,
    container.goalsStore.weightGoal,
    todayFlow(container.clock)
) { profile, weights, weightGoal, today ->
    val weightKg = latestWeight(weights)?.weightKg
    buildMap {
        weightKg?.let { put(GoalType.WATER, suggestedWaterGoal(it)) }
        val kcal = suggestedCalories(profile, weightKg, objective(weightGoal, weightKg), today)
        if (kcal is CalorieSuggestion.Ready) put(GoalType.FOOD, kcal.kcal)
    }
}

package com.projeto.wellio.ui.module

import com.projeto.wellio.domain.goals.progress
import com.projeto.wellio.domain.model.ExerciseUnit
import com.projeto.wellio.domain.weight.WeightGoal
import com.projeto.wellio.domain.weight.goalProgress
import com.projeto.wellio.ui.format.formatInt
import com.projeto.wellio.ui.format.formatKg
import com.projeto.wellio.ui.format.formatLiters
import kotlin.math.abs

/**
 * Resumo de uma funcionalidade (SPEC §5.3): só os dados, sem layout. Ícone e cor vêm do
 * módulo; Dashboard, card Combinado e cards mini montam o visual a partir daqui.
 */
data class Summary(
    val module: ModuleId,
    /** Valor principal: "1,4 L". */
    val value: String,
    /** Meta ou valor secundário: "de 2 L", "↓ 0,3 kg na semana". */
    val secondary: String? = null,
    /** Progresso de 0 a 1; null quando não há meta (anel cheio em tom suave). */
    val progress: Float? = null,
    /** O que o TalkBack lê. */
    val spoken: String
)

private fun percent(fraction: Float) = (fraction * 100).toInt()

fun waterSummary(totalMl: Int, goalMl: Int): Summary {
    val fraction = progress(totalMl, goalMl)
    return Summary(
        module = ModuleId.Water,
        value = formatLiters(totalMl),
        secondary = "de ${formatLiters(goalMl)}",
        progress = fraction,
        spoken = "Água: ${formatLiters(totalMl).spokenLiters()} de ${formatLiters(goalMl).spokenLiters()}, ${percent(fraction)} por cento"
    )
}

fun foodSummary(consumedKcal: Int, goalKcal: Int): Summary {
    val fraction = progress(consumedKcal, goalKcal)
    return Summary(
        module = ModuleId.Food,
        value = formatInt(consumedKcal),
        secondary = "de ${formatInt(goalKcal)}",
        progress = fraction,
        spoken = "Alimentação: ${formatInt(consumedKcal)} de ${formatInt(goalKcal)} quilocalorias, ${percent(fraction)} por cento"
    )
}

/** Exercício na unidade da meta (SPEC §8.0): kcal queimadas ou minutos ativos. */
fun exerciseSummary(burnedKcal: Int, minutes: Int, goal: Int, unit: ExerciseUnit = ExerciseUnit.Kcal): Summary {
    if (unit == ExerciseUnit.Minutes) {
        val fraction = progress(minutes, goal)
        return Summary(
            module = ModuleId.Exercise,
            value = "$minutes min",
            secondary = "de $goal",
            progress = fraction,
            spoken = "Exercício: $minutes de $goal minutos ativos, ${percent(fraction)} por cento"
        )
    }
    val fraction = progress(burnedKcal, goal)
    return Summary(
        module = ModuleId.Exercise,
        value = formatInt(burnedKcal),
        secondary = "de ${formatInt(goal)}",
        progress = fraction,
        spoken = "Exercício: ${formatInt(burnedKcal)} de ${formatInt(goal)} quilocalorias queimadas, ${percent(fraction)} por cento"
    )
}

/**
 * Peso: com alvo, o progresso até ele; sem alvo, a variação da semana no lugar da meta.
 * [currentKg] null = ainda sem pesagens.
 */
fun weightSummary(currentKg: Double?, weekChangeKg: Double?, goal: WeightGoal?): Summary {
    if (currentKg == null) {
        return Summary(ModuleId.Weight, value = "—", secondary = "sem pesagens", spoken = "Peso: sem pesagens")
    }
    val change = weekChangeKg?.let(::arrowChange)
    if (goal != null) {
        return Summary(
            module = ModuleId.Weight,
            value = formatKg(currentKg),
            secondary = "alvo ${formatKg(goal.targetKg)}",
            progress = goalProgress(goal, currentKg),
            spoken = "Peso: ${formatKg(currentKg).spokenKg()}, alvo ${formatKg(goal.targetKg).spokenKg()}"
        )
    }
    return Summary(
        module = ModuleId.Weight,
        value = formatKg(currentKg),
        secondary = change?.let { "$it na semana" },
        spoken = listOfNotNull(
            "Peso: ${formatKg(currentKg).spokenKg()}",
            weekChangeKg?.let { spokenChange(it) }
        ).joinToString(", ")
    )
}

/** "↓ 0,3 kg", "↑ 0,2 kg" ou "= 0,0 kg" */
private fun arrowChange(kg: Double): String = when {
    kg < 0 -> "↓ ${formatKg(abs(kg))}"
    kg > 0 -> "↑ ${formatKg(kg)}"
    else -> "= ${formatKg(0.0)}"
}

private fun spokenChange(kg: Double): String = when {
    kg < 0 -> "caiu ${formatKg(abs(kg)).spokenKg()} na semana"
    kg > 0 -> "subiu ${formatKg(kg).spokenKg()} na semana"
    else -> "estável na semana"
}

private fun String.spokenLiters() = removeSuffix(" L") + " litros"
private fun String.spokenKg() = removeSuffix(" kg") + " quilos"

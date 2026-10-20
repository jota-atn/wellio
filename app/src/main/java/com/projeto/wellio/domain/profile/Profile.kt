package com.projeto.wellio.domain.profile

import com.projeto.wellio.domain.model.Profile
import com.projeto.wellio.domain.model.Sex
import com.projeto.wellio.domain.weight.WeightGoal
import java.time.LocalDate
import java.time.Period
import kotlin.math.abs
import kotlin.math.roundToInt

const val MIN_HEIGHT_CM = 100
const val MAX_HEIGHT_CM = 250

/** Idade completa em [today]. */
fun age(birthDate: LocalDate, today: LocalDate): Int = Period.between(birthDate, today).years

/** Altura digitada em metros ou centímetros ("1,78", "1.78" ou "178"), em cm. */
fun parseHeight(text: String): Int? {
    val value = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
    val cm = (if (value < 3) value * 100 else value).roundToInt()
    return cm.takeIf { it in MIN_HEIGHT_CM..MAX_HEIGHT_CM }
}

/** IMC com uma casa. */
fun bmi(weightKg: Double, heightCm: Int): Double {
    val meters = heightCm / 100.0
    return (weightKg / (meters * meters) * 10).roundToInt() / 10.0
}

/** Faixas do IMC (OMS), para a cor do ponto na ficha do perfil. */
enum class BmiRange(val label: String) {
    Under("Abaixo do peso"),
    Normal("Peso normal"),
    Over("Sobrepeso"),
    Obese("Obesidade");

    companion object {
        fun of(bmi: Double): BmiRange = when {
            bmi < 18.5 -> Under
            bmi < 25 -> Normal
            bmi < 30 -> Over
            else -> Obese
        }
    }
}

/** Objetivo, que sai do peso alvo (SPEC §9.1), e o ajuste que ele faz na meta calórica. */
enum class Objective(val label: String, val kcalAdjust: Int) {
    Lose("Perder peso", -500),
    Keep("Manter peso", 0),
    Gain("Ganhar peso", 300)
}

/** Alvo abaixo do peso atual = perder, acima = ganhar; sem alvo, sem peso ou a menos de 1 kg = manter. */
fun objective(goal: WeightGoal?, currentKg: Double?): Objective {
    if (goal == null || currentKg == null) return Objective.Keep
    val diff = goal.targetKg - currentKg
    return when {
        abs(diff) < 1.0 -> Objective.Keep
        diff < 0 -> Objective.Lose
        else -> Objective.Gain
    }
}

/** O que falta no perfil para sugerir a meta calórica. */
enum class MissingField(val label: String) {
    BirthDate("data de nascimento"),
    Sex("sexo"),
    Height("altura"),
    Weight("peso")
}

/** Meta calórica sugerida, ou por que ela não pode ser calculada. */
sealed interface CalorieSuggestion {
    /** [atFloor]: a conta deu menos que o mínimo recomendado, e a sugestão ficou no mínimo. */
    data class Ready(val kcal: Int, val atFloor: Boolean = false) : CalorieSuggestion
    /** Faltam dados; [fields] na ordem em que aparecem no perfil. */
    data class Missing(val fields: List<MissingField>) : CalorieSuggestion
    /** "Prefiro não informar": a fórmula depende do sexo. */
    data object SexUndisclosed : CalorieSuggestion
}

/**
 * Meta calórica sugerida (SPEC §9.1): Mifflin-St Jeor × fator do nível de atividade
 * (sedentário quando não informado), mais o ajuste do objetivo, arredondada para 50 kcal e
 * nunca abaixo do mínimo recomendado (1.200 kcal para mulheres, 1.500 para homens).
 */
fun suggestedCalories(profile: Profile, weightKg: Double?, objective: Objective, today: LocalDate): CalorieSuggestion {
    if (profile.sex == Sex.Undisclosed) return CalorieSuggestion.SexUndisclosed
    val missing = buildList {
        if (profile.birthDate == null) add(MissingField.BirthDate)
        if (profile.sex == null) add(MissingField.Sex)
        if (profile.heightCm == null) add(MissingField.Height)
        if (weightKg == null) add(MissingField.Weight)
    }
    if (missing.isNotEmpty()) return CalorieSuggestion.Missing(missing)

    val sexConstant = if (profile.sex == Sex.Male) 5 else -161
    val basal = 10 * weightKg!! + 6.25 * profile.heightCm!! - 5 * age(profile.birthDate!!, today) + sexConstant
    val factor = profile.activity?.factor ?: 1.2
    val kcal = ((basal * factor + objective.kcalAdjust) / 50).roundToInt() * 50
    val floor = if (profile.sex == Sex.Male) 1500 else 1200
    return if (kcal < floor) CalorieSuggestion.Ready(floor, atFloor = true) else CalorieSuggestion.Ready(kcal)
}

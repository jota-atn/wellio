package com.projeto.wellio.ui.goals

import com.projeto.wellio.domain.goals.Rings
import com.projeto.wellio.domain.model.ExerciseUnit
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.domain.model.Goals
import com.projeto.wellio.domain.model.Totals
import com.projeto.wellio.ui.format.formatInt
import kotlin.math.roundToInt

/** Como cada meta é chamada nas frases. */
private fun phraseName(type: GoalType) = when (type) {
    GoalType.WATER -> "a água"
    GoalType.FOOD -> "as calorias"
    GoalType.EXERCISE -> "o exercício"
}

/**
 * Frase do dia no Dashboard (SPEC §5.4), no tom zoeiro e épico (TEXTOS.md): diz o que
 * falta para o anel mais perto de fechar, comemora os fechados e provoca no dia em branco.
 */
fun dayPhrase(totals: Totals, goals: Goals, rings: Rings, isToday: Boolean): String {
    val closed = listOf(rings.water, rings.food, rings.exercise).count { it >= 1f }
    if (!isToday) {
        return when {
            closed == 3 -> "Dia lendário: os três anéis fechados."
            totals == Totals() -> "Um dia em branco. Acontece até com os melhores."
            else -> "Esse dia fechou com ${(rings.score * 100).roundToInt()}% das metas."
        }
    }
    if (closed == 3) return "Três anéis fechados. Lenda."
    if (totals == Totals()) return "Dia em branco. O primeiro gole é o mais difícil."

    // O anel aberto mais perto de fechar
    val open = listOf(GoalType.WATER to rings.water, GoalType.FOOD to rings.food, GoalType.EXERCISE to rings.exercise)
        .filter { it.second < 1f }
        .maxBy { it.second }
    val (type, fraction) = open
    if (fraction >= 0.75f) {
        val left = goals[type].daily - totals.toward(type, goals)
        val unit = goalUnit(type, goals.exerciseUnit)
        val amount = if (type == GoalType.EXERCISE && goals.exerciseUnit == ExerciseUnit.Minutes) "$left min de treino" else "${formatInt(left)} $unit"
        return "Faltam $amount pra fechar ${phraseName(type)}. Bora?"
    }
    return when (closed) {
        2 -> "Dois anéis fechados. Falta um pra lenda."
        1 -> "Um anel fechado. Os outros estão com ciúme."
        else -> "Bora encher esses anéis."
    }
}

/** "+12% que ontem", "−5% que ontem" ou "igual a ontem", em pontos do "% do dia". */
fun comparedToYesterday(today: Float, yesterday: Float): String {
    val diff = ((today - yesterday) * 100).roundToInt()
    return when {
        diff > 0 -> "+$diff% que ontem"
        diff < 0 -> "−${-diff}% que ontem"
        else -> "igual a ontem"
    }
}

package com.projeto.wellio.domain.weight

import com.projeto.wellio.domain.model.WeightEntry
import kotlin.math.roundToInt

const val MIN_WEIGHT_KG = 20.0
const val MAX_WEIGHT_KG = 400.0

/** Peso usado no cálculo de kcal enquanto não houver nenhuma pesagem. */
const val DEFAULT_WEIGHT_KG = 70.0

/** Peso digitado, com vírgula ou ponto ("72,4" ou "72.4"), arredondado a uma casa. */
fun parseWeight(text: String): Double? {
    val value = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (value < MIN_WEIGHT_KG || value > MAX_WEIGHT_KG) return null
    return (value * 10).roundToInt() / 10.0
}

/** Pesagem mais recente (pela data), que vale no cálculo de kcal. */
fun latestWeight(entries: List<WeightEntry>): WeightEntry? = entries.maxByOrNull { it.date }

/** Pesagens da mais recente para a mais antiga, cada uma com a variação em relação à anterior. */
fun withChanges(entries: List<WeightEntry>): List<Pair<WeightEntry, Double?>> {
    val newestFirst = entries.sortedByDescending { it.date }
    return newestFirst.mapIndexed { i, entry ->
        val previous = newestFirst.getOrNull(i + 1)
        entry to previous?.let { ((entry.weightKg - it.weightKg) * 10).roundToInt() / 10.0 }
    }
}

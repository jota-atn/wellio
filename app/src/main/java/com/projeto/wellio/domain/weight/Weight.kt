package com.projeto.wellio.domain.weight

import com.projeto.wellio.domain.model.WeightEntry
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
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

/** Períodos da curva de evolução; [days] = null é todo o histórico. */
enum class WeightPeriod(val days: Long?, val label: String) {
    Month(30, "30 dias"),
    Quarter(90, "90 dias"),
    All(null, "Tudo")
}

/** Pesagens do período, da mais antiga para a mais recente (pontos da curva). */
fun weightTrend(entries: List<WeightEntry>, today: LocalDate, period: WeightPeriod): List<WeightEntry> {
    val since = period.days?.let { today.minusDays(it) }
    return entries.filter { since == null || !it.date.isBefore(since) }.sortedBy { it.date }
}

/** Variação entre a primeira e a última pesagem da curva; null com menos de duas. */
fun trendChange(trend: List<WeightEntry>): Double? {
    if (trend.size < 2) return null
    return ((trend.last().weightKg - trend.first().weightKg) * 10).roundToInt() / 10.0
}

/** Peso alvo e o peso de quando ele foi definido (início da barra de progresso). */
data class WeightGoal(val targetKg: Double, val startKg: Double)

/** Fração do caminho entre o início e o alvo já percorrida, entre 0 e 1. */
fun goalProgress(goal: WeightGoal, currentKg: Double): Float {
    val total = goal.targetKg - goal.startKg
    if (total == 0.0) return 1f
    return ((currentKg - goal.startKg) / total).toFloat().coerceIn(0f, 1f)
}

/** Quanto falta até o alvo, sempre positivo; 0 quando já chegou (ou passou). */
fun remainingKg(goal: WeightGoal, currentKg: Double): Double {
    val losing = goal.targetKg <= goal.startKg
    val left = if (losing) currentKg - goal.targetKg else goal.targetKg - currentKg
    return (left.coerceAtLeast(0.0) * 10).roundToInt() / 10.0
}

data class WeightSummary(val minKg: Double, val maxKg: Double, val averageKg: Double)

/** Menor, maior e média das pesagens; null sem pesagens. */
fun summarize(trend: List<WeightEntry>): WeightSummary? {
    if (trend.isEmpty()) return null
    val weights = trend.map { it.weightKg }
    return WeightSummary(weights.min(), weights.max(), (weights.average() * 10).roundToInt() / 10.0)
}

/** Ritmo em kg por semana, entre a primeira e a última pesagem; null com menos de 7 dias entre elas. */
fun weeklyRate(trend: List<WeightEntry>): Double? {
    if (trend.size < 2) return null
    val days = ChronoUnit.DAYS.between(trend.first().date, trend.last().date)
    if (days < 7) return null
    return (trend.last().weightKg - trend.first().weightKg) / days * 7
}

/** Dia estimado para chegar ao alvo no ritmo atual; null se o ritmo não leva até lá (ou leva mais de 2 anos). */
fun projectedDate(goal: WeightGoal, currentKg: Double, ratePerWeek: Double?, today: LocalDate): LocalDate? {
    if (ratePerWeek == null || ratePerWeek == 0.0) return null
    if (remainingKg(goal, currentKg) == 0.0) return null
    val days = (goal.targetKg - currentKg) / ratePerWeek * 7
    if (days <= 0 || days > 730) return null
    return today.plusDays(ceil(days).toLong())
}

/** Variação nos últimos 7 dias (da primeira à última pesagem desse intervalo); null com menos de duas. */
fun weekChange(entries: List<WeightEntry>, today: LocalDate): Double? =
    trendChange(entries.filter { !it.date.isBefore(today.minusDays(7)) }.sortedBy { it.date })

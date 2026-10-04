package com.projeto.wellio.domain.weight

import com.projeto.wellio.domain.model.WeightEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WeightTest {

    private fun weighing(day: Int, kg: Double) = WeightEntry("d$day", LocalDate.of(2026, 9, day), kg)

    @Test
    fun `peso aceita virgula`() {
        assertEquals(72.4, parseWeight("72,4")!!, 0.0)
    }

    @Test
    fun `peso aceita ponto`() {
        assertEquals(72.4, parseWeight("72.4")!!, 0.0)
    }

    @Test
    fun `peso arredonda a uma casa`() {
        assertEquals(72.5, parseWeight("72,46")!!, 0.0)
    }

    @Test
    fun `peso rejeita texto e valores fora da faixa`() {
        assertNull(parseWeight("abc"))
        assertNull(parseWeight("5"))
        assertNull(parseWeight("500"))
    }

    @Test
    fun `peso mais recente e o da data mais nova`() {
        val entries = listOf(weighing(10, 74.0), weighing(28, 72.4), weighing(20, 73.1))

        assertEquals(72.4, latestWeight(entries)!!.weightKg, 0.0)
    }

    @Test
    fun `sem pesagens nao ha peso mais recente`() {
        assertNull(latestWeight(emptyList()))
    }

    @Test
    fun `variacao e em relacao a pesagem anterior`() {
        val result = withChanges(listOf(weighing(10, 74.0), weighing(20, 73.1), weighing(28, 72.4)))

        assertEquals(listOf(-0.7, -0.9, null), result.map { it.second })
    }

    @Test
    fun `curva do mes ignora pesagens mais antigas e vai da mais antiga para a mais recente`() {
        val today = LocalDate.of(2026, 9, 30)
        val entries = listOf(weighing(20, 72.0), weighing(1, 74.0), weighing(10, 73.0))
        val old = WeightEntry("ago", LocalDate.of(2026, 8, 1), 75.0)

        val trend = weightTrend(entries + old, today, WeightPeriod.Month)

        assertEquals(listOf("d1", "d10", "d20"), trend.map { it.id })
    }

    @Test
    fun `curva de todo o historico inclui tudo`() {
        val old = WeightEntry("2025", LocalDate.of(2025, 1, 1), 80.0)

        assertEquals(2, weightTrend(listOf(old, weighing(1, 74.0)), LocalDate.of(2026, 9, 30), WeightPeriod.All).size)
    }

    @Test
    fun `variacao da curva e da primeira para a ultima pesagem`() {
        assertEquals(-1.2, trendChange(listOf(weighing(1, 74.0), weighing(10, 73.5), weighing(20, 72.8)))!!, 0.0)
    }

    @Test
    fun `curva com uma pesagem nao tem variacao`() {
        assertNull(trendChange(listOf(weighing(1, 74.0))))
    }

    private val perder = WeightGoal(targetKg = 70.0, startKg = 80.0)

    @Test
    fun `progresso do alvo conta desde o peso inicial`() {
        assertEquals(0.4f, goalProgress(perder, 76.0), 0.001f)
    }

    @Test
    fun `progresso nao passa de 100 por cento nem fica negativo`() {
        assertEquals(1f, goalProgress(perder, 68.0), 0f)
        assertEquals(0f, goalProgress(perder, 82.0), 0f)
    }

    @Test
    fun `falta e sempre positivo para perder ou ganhar peso`() {
        assertEquals(6.0, remainingKg(perder, 76.0), 0.0)
        assertEquals(2.5, remainingKg(WeightGoal(targetKg = 65.0, startKg = 60.0), 62.5), 0.0)
    }

    @Test
    fun `alvo alcancado nao falta nada`() {
        assertEquals(0.0, remainingKg(perder, 69.5), 0.0)
    }

    @Test
    fun `resumo traz menor maior e media`() {
        val summary = summarize(listOf(weighing(1, 74.0), weighing(10, 73.0), weighing(20, 72.5)))

        assertEquals(WeightSummary(minKg = 72.5, maxKg = 74.0, averageKg = 73.2), summary)
    }

    @Test
    fun `ritmo semanal precisa de pelo menos 7 dias`() {
        assertNull(weeklyRate(listOf(weighing(1, 74.0), weighing(5, 73.0))))
        assertEquals(-0.5, weeklyRate(listOf(weighing(1, 74.0), weighing(15, 73.0)))!!, 0.001)
    }

    @Test
    fun `projecao usa o ritmo para estimar o dia`() {
        val today = LocalDate.of(2026, 10, 1)

        assertEquals(today.plusDays(28), projectedDate(perder, 72.0, -0.5, today))
    }

    @Test
    fun `ritmo na direcao contraria nao projeta`() {
        assertNull(projectedDate(perder, 72.0, 0.3, LocalDate.of(2026, 10, 1)))
    }

    @Test
    fun `variacao da semana olha so os ultimos 7 dias`() {
        val today = LocalDate.of(2026, 9, 20)

        assertEquals(-0.5, weekChange(listOf(weighing(1, 75.0), weighing(14, 73.0), weighing(20, 72.5)), today)!!, 0.0)
    }
}

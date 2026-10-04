package com.projeto.wellio.ui.module

import com.projeto.wellio.domain.weight.WeightGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SummaryTest {

    @Test
    fun `agua mostra litros e progresso da meta`() {
        val summary = waterSummary(totalMl = 1400, goalMl = 2000)

        assertEquals("1,4 L", summary.value)
        assertEquals("de 2 L", summary.secondary)
        assertEquals(0.7f, summary.progress!!, 0.001f)
        assertEquals("Água: 1,4 litros de 2 litros, 70 por cento", summary.spoken)
    }

    @Test
    fun `alimentacao mostra kcal da meta, com a unidade so na fala`() {
        val summary = foodSummary(consumedKcal = 1250, goalKcal = 2000)

        assertEquals("1.250", summary.value)
        assertEquals("de 2.000", summary.secondary)
        assertEquals("Alimentação: 1.250 de 2.000 quilocalorias, 62 por cento", summary.spoken)
    }

    @Test
    fun `peso sem alvo troca a meta pela variacao da semana e nao tem progresso`() {
        val summary = weightSummary(currentKg = 72.4, weekChangeKg = -0.3, goal = null)

        assertEquals("72,4 kg", summary.value)
        assertEquals("↓ 0,3 kg na semana", summary.secondary)
        assertNull(summary.progress)
        assertEquals("Peso: 72,4 quilos, caiu 0,3 quilos na semana", summary.spoken)
    }

    @Test
    fun `peso com alvo mostra o progresso ate ele`() {
        val summary = weightSummary(currentKg = 76.0, weekChangeKg = null, goal = WeightGoal(targetKg = 70.0, startKg = 80.0))

        assertEquals("alvo 70,0 kg", summary.secondary)
        assertEquals(0.4f, summary.progress!!, 0.001f)
    }

    @Test
    fun `peso sem pesagens avisa`() {
        assertEquals("—", weightSummary(null, null, null).value)
    }
}

package com.projeto.wellio.domain.goals

import com.projeto.wellio.domain.model.Goals
import com.projeto.wellio.domain.model.Totals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DayScoreTest {

    private val goals = Goals.Default // água 2.000 ml, comida 2.000 kcal, exercício 500 kcal

    @Test
    fun `media das tres metas`() {
        assertEquals(0.5f, dayScore(Totals(waterMl = 1000, kcalConsumed = 1000, kcalBurned = 250), goals), 0.001f)
    }

    @Test
    fun `meta passada conta no maximo 100 por cento`() {
        assertEquals(1f / 3, dayScore(Totals(waterMl = 5000), goals), 0.001f)
    }

    @Test
    fun `ultimos dias terminam hoje`() {
        val today = LocalDate.of(2026, 10, 4)

        val days = lastDays(today)

        assertEquals(7, days.size)
        assertEquals(LocalDate.of(2026, 9, 28), days.first())
        assertEquals(today, days.last())
    }
}

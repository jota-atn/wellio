package com.projeto.wellio.domain.goals

import com.projeto.wellio.domain.model.ExerciseUnit
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

    @Test
    fun `exercicio conta na unidade da meta`() {
        val inMinutes = goals.withExerciseUnit(ExerciseUnit.Minutes) // meta de 30 min

        assertEquals(1f / 6, dayScore(Totals(kcalBurned = 500, exerciseMinutes = 15), inMinutes), 0.001f)
    }

    @Test
    fun `trocar a unidade volta a meta ao padrao dela`() {
        val inMinutes = goals.withExerciseUnit(ExerciseUnit.Minutes)

        assertEquals(30, inMinutes.exercise.daily)
        assertEquals(inMinutes, inMinutes.withExerciseUnit(ExerciseUnit.Minutes))
    }
}

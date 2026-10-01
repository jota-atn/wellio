package com.projeto.wellio.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalsTest {

    @Test
    fun `meta semanal e a diaria vezes sete por padrao`() {
        assertEquals(14000, Goal(daily = 2000).weekly)
    }

    @Test
    fun `meta semanal definida pelo usuario substitui a derivada`() {
        assertEquals(10000, Goal(daily = 2000, weeklyOverride = 10000).weekly)
    }

    @Test
    fun `alterar uma meta nao mexe nas outras`() {
        val updated = Goals.Default.with(GoalType.WATER, Goal(daily = 3000))

        assertEquals(3000, updated.water.daily)
        assertEquals(Goals.Default.food, updated.food)
        assertEquals(Goals.Default.exercise, updated.exercise)
    }
}

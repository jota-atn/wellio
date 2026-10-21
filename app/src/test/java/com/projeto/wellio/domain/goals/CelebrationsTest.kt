package com.projeto.wellio.domain.goals

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CelebrationsTest {

    @Test
    fun `registro que passa da meta comemora`() {
        assertTrue(justReached(GoalReading(1800.0, 2000.0), GoalReading(2100.0, 2000.0)))
    }

    @Test
    fun `abrir o app com a meta ja cumprida nao comemora`() {
        assertFalse(justReached(null, GoalReading(2100.0, 2000.0)))
    }

    @Test
    fun `baixar a meta abaixo do que ja foi feito nao comemora`() {
        assertFalse(justReached(GoalReading(1600.0, 2000.0), GoalReading(1600.0, 1500.0)))
    }

    @Test
    fun `ja cumprida e mais um registro nao comemora de novo`() {
        assertFalse(justReached(GoalReading(2100.0, 2000.0), GoalReading(2400.0, 2000.0)))
    }

    @Test
    fun `apagar e cumprir de novo comemora de novo`() {
        val dropped = GoalReading(1800.0, 2000.0)
        assertTrue(justReached(dropped, GoalReading(2100.0, 2000.0)))
    }

    @Test
    fun `meta zero nunca conta como cumprida`() {
        assertFalse(justReached(GoalReading(0.0, 0.0), GoalReading(10.0, 0.0)))
    }
}

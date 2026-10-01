package com.projeto.wellio.domain.goals

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressTest {

    @Test
    fun `progresso e a fracao da meta`() {
        assertEquals(0.7f, progress(current = 1400, goal = 2000), 0.0001f)
    }

    @Test
    fun `progresso acima da meta fica em 100 por cento`() {
        assertEquals(1f, progress(current = 2600, goal = 2000), 0f)
    }

    @Test
    fun `meta zero nao divide por zero`() {
        assertEquals(0f, progress(current = 500, goal = 0), 0f)
    }
}

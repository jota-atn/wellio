package com.projeto.wellio.data.seed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSeedTest {

    private val categories = setOf("Cardio", "Musculação", "Lutas", "Esportes", "Aquáticos", "Zen/Dança", "Outros")

    @Test
    fun `seed tem as 136 atividades aprovadas com ids unicos`() {
        assertEquals(136, exerciseSeed.size)
        assertEquals(exerciseSeed.size, exerciseSeed.map { it.id }.toSet().size)
    }

    @Test
    fun `toda atividade tem categoria do app, METs e codigo do Compendium`() {
        exerciseSeed.forEach {
            assertTrue(it.name, it.category in categories)
            assertTrue(it.name, it.mets > 0)
            assertTrue(it.name, it.compendiumCode != null)
        }
    }
}

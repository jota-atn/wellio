package com.projeto.wellio.data.seed

import com.projeto.wellio.domain.exercise.exerciseFamilies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSeedTest {

    private val categories = setOf("Cardio", "Musculação", "Lutas", "Esportes", "Aquáticos", "Zen/Dança", "Outros")

    @Test
    fun `seed tem 67 atividades em 103 niveis, com ids unicos`() {
        assertEquals(103, exerciseSeed.size)
        assertEquals(67, exerciseFamilies(exerciseSeed).size)
        assertEquals(exerciseSeed.size, exerciseSeed.map { it.id }.toSet().size)
    }

    @Test
    fun `atividade com variacoes tem niveis diferentes e todos marcados`() {
        exerciseFamilies(exerciseSeed).filter { it.hasLevels }.forEach { family ->
            val levels = family.variants.map { it.level }
            assertTrue(family.name, levels.none { it == null })
            assertEquals(family.name, levels.size, levels.toSet().size)
        }
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

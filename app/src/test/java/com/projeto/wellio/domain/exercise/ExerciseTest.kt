package com.projeto.wellio.domain.exercise

import com.projeto.wellio.domain.model.ExerciseType
import com.projeto.wellio.domain.model.ExerciseUse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ExerciseTest {

    private fun type(id: String, name: String = id, category: String = "Cardio", favorite: Boolean = false) =
        ExerciseType(id, name, category, mets = 5.0, iconKey = "exercise.x", isFavorite = favorite)

    private fun use(id: String, minutes: Int, hour: Int) =
        ExerciseUse(id, minutes, Instant.parse("2026-10-04T%02d:00:00Z".format(hour)))

    private val corrida = type("corrida", "Corrida", favorite = true)
    private val yoga = type("yoga", "Yoga", "Zen/Dança", favorite = true)
    private val bike = type("bike", "Bicicleta")
    private val natacao = type("natacao", "Natação", "Aquáticos")
    private val todos = listOf(corrida, yoga, bike, natacao)

    @Test
    fun `categoria desconhecida cai em outros`() {
        assertEquals(ExerciseCategory.Zen, ExerciseCategory.of("Zen/Dança"))
        assertEquals(ExerciseCategory.Other, ExerciseCategory.of("Sumiu"))
    }

    @Test
    fun `fileira traz favoritos primeiro e depois recentes, com a ultima duracao`() {
        val uses = listOf(use("bike", 45, 12), use("corrida", 30, 9))

        val quick = quickExercises(todos, uses)

        assertEquals(listOf("corrida", "yoga", "bike"), quick.map { it.type.id })
        assertEquals(30, quick[0].minutes)
        assertEquals(DEFAULT_EXERCISE_MINUTES, quick[1].minutes)
        assertEquals(45, quick[2].minutes)
    }

    @Test
    fun `fileira respeita o maximo`() {
        assertEquals(1, quickExercises(todos, emptyList(), max = 1).size)
    }

    @Test
    fun `recentes vem do uso mais recente para o mais antigo`() {
        val uses = listOf(use("natacao", 20, 15), use("bike", 45, 12))

        assertEquals(listOf("natacao", "bike"), recentExercises(todos, uses).map { it.id })
    }

    @Test
    fun `busca ignora acento e maiuscula e poe quem comeca com o texto antes`() {
        val caminhada = type("cam", "Caminhada")
        val corridaCaminhada = type("cc", "Corrida e caminhada")

        val result = searchExercises(listOf(corridaCaminhada, caminhada, natacao), "CAMIN")

        assertEquals(listOf("cam", "cc"), result.map { it.id })
        assertEquals(listOf("natacao"), searchExercises(todos, "natacao").map { it.id })
    }

    @Test
    fun `busca filtra pela categoria`() {
        assertEquals(listOf("natacao"), searchExercises(todos, "", ExerciseCategory.Water).map { it.id })
    }

    @Test
    fun `mostrador tem 60 minutos por volta e acumula voltas`() {
        assertEquals(15, dialMinutes(90f))
        assertEquals(75, dialMinutes(450f))
        assertEquals(450f, dialDegrees(75), 0f)
    }

    @Test
    fun `mostrador fica entre zero e o maximo`() {
        assertEquals(0, dialMinutes(-30f))
        assertEquals(MAX_EXERCISE_MINUTES, dialMinutes(99_999f))
    }

    @Test
    fun `mostrador vibra a cada 5 minutos`() {
        assertTrue(isDialMark(45))
        assertFalse(isDialMark(46))
    }

    @Test
    fun `kcal digitada precisa ser inteira e dentro do limite`() {
        assertEquals(320, parseKcal("320"))
        assertNull(parseKcal("0"))
        assertNull(parseKcal("abc"))
    }

    @Test
    fun `atividade propria precisa de nome e aceita kcal por minuto com virgula`() {
        assertEquals(CustomExerciseValidation.BlankName, validateCustomExercise("  ", ""))
        assertEquals(CustomExerciseValidation.Valid("Treino do parque", 8.5), validateCustomExercise("Treino do parque", "8,5"))
        assertEquals(CustomExerciseValidation.Valid("Treino", null), validateCustomExercise("Treino", ""))
        assertEquals(CustomExerciseValidation.InvalidKcalPerMinute, validateCustomExercise("Treino", "99"))
    }

    @Test
    fun `fogueira cresce ate a meta e solta faiscas ao passar dela`() {
        assertEquals(FireLevel(0.5f, sparks = false), fireLevel(0.5f))
        assertEquals(FireLevel(1f, sparks = true), fireLevel(1.3f))
    }
}

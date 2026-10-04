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

    private fun level(id: String, name: String, intensity: Intensity) =
        ExerciseType(id, name, "Cardio", mets = 5.0 + intensity.ordinal, iconKey = "exercise.run", level = intensity)

    private val corridaLeve = level("c1", "Corrida", Intensity.Light)
    private val corridaModerada = level("c2", "Corrida", Intensity.Moderate)
    private val corridaIntensa = level("c3", "Corrida", Intensity.Vigorous)

    @Test
    fun `niveis da mesma atividade viram uma so, do leve ao intenso`() {
        val families = exerciseFamilies(listOf(corridaIntensa, bike, corridaLeve, corridaModerada))

        assertEquals(2, families.size)
        val corrida = families.first { it.name == "Corrida" }
        assertEquals(listOf("c1", "c2", "c3"), corrida.variants.map { it.id })
        assertEquals("c2", corrida.default.id)
        assertTrue(corrida.hasLevels)
    }

    @Test
    fun `ultimo uso da atividade traz o nivel e a duracao`() {
        val corrida = exerciseFamilies(listOf(corridaLeve, corridaModerada, corridaIntensa)).single()

        assertEquals(QuickExercise(corridaIntensa, 40), lastOf(corrida, listOf(use("c3", 40, 10))))
        assertEquals(QuickExercise(corridaModerada, DEFAULT_EXERCISE_MINUTES), lastOf(corrida, emptyList()))
    }

    @Test
    fun `fileira traz favoritas primeiro e depois recentes, com a ultima duracao`() {
        val uses = listOf(use("bike", 45, 12), use("corrida", 30, 9))

        val quick = quickExercises(todos, uses)

        assertEquals(listOf("corrida", "yoga", "bike"), quick.map { it.type.id })
        assertEquals(30, quick[0].minutes)
        assertEquals(DEFAULT_EXERCISE_MINUTES, quick[1].minutes)
        assertEquals(45, quick[2].minutes)
    }

    @Test
    fun `fileira mostra cada atividade uma vez, no ultimo nivel usado`() {
        val uses = listOf(use("c3", 20, 12), use("c1", 50, 9))

        val quick = quickExercises(listOf(corridaLeve, corridaModerada, corridaIntensa), uses)

        assertEquals(listOf(QuickExercise(corridaIntensa, 20)), quick)
    }

    @Test
    fun `fileira respeita o maximo`() {
        assertEquals(1, quickExercises(todos, emptyList(), max = 1).size)
    }

    @Test
    fun `recentes vem do uso mais recente para o mais antigo, sem repetir`() {
        val families = exerciseFamilies(todos + listOf(corridaLeve, corridaIntensa))
        val uses = listOf(use("natacao", 20, 15), use("c3", 30, 13), use("c1", 30, 12))

        assertEquals(listOf("Natação", "Corrida"), recentFamilies(families, uses).map { it.name })
    }

    @Test
    fun `busca ignora acento e maiuscula e poe quem comeca com o texto antes`() {
        val caminhada = type("cam", "Caminhada")
        val corridaCaminhada = type("cc", "Corrida e caminhada")

        val result = searchFamilies(exerciseFamilies(listOf(corridaCaminhada, caminhada, natacao)), "CAMIN")

        assertEquals(listOf("Caminhada", "Corrida e caminhada"), result.map { it.name })
        assertEquals(listOf("Natação"), searchFamilies(exerciseFamilies(todos), "natacao").map { it.name })
    }

    @Test
    fun `busca filtra pela categoria`() {
        assertEquals(listOf("Natação"), searchFamilies(exerciseFamilies(todos), "", ExerciseCategory.Water).map { it.name })
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

package com.projeto.wellio.domain.calories

import com.projeto.wellio.domain.model.ExerciseType
import org.junit.Assert.assertEquals
import org.junit.Test

class CaloriesTest {

    @Test
    fun `exercicio usa METs vezes peso vezes horas`() {
        assertEquals(350, exerciseKcal(mets = 10.0, weightKg = 70.0, minutes = 30))
    }

    @Test
    fun `exercicio arredonda para o inteiro mais proximo`() {
        // 3.5 × 70 × (25/60) = 102,08
        assertEquals(102, exerciseKcal(mets = 3.5, weightKg = 70.0, minutes = 25))
    }

    @Test
    fun `exercicio de zero minutos nao gasta nada`() {
        assertEquals(0, exerciseKcal(mets = 8.0, weightKg = 80.0, minutes = 0))
    }

    @Test
    fun `alimento multiplica kcal da porcao pela quantidade`() {
        assertEquals(260, foodKcal(kcalPerPortion = 130.0, portions = 2.0))
    }

    @Test
    fun `alimento aceita meia porcao`() {
        assertEquals(68, foodKcal(kcalPerPortion = 135.0, portions = 0.5))
    }

    @Test
    fun `atividade propria com kcal por minuto ignora os METs`() {
        val custom = ExerciseType("x", "Treino do parque", "Outros", mets = 5.0, iconKey = "exercise.other", isCustom = true, kcalPerMinute = 8.5)

        assertEquals(255, exerciseKcal(custom, weightKg = 70.0, minutes = 30))
    }

    @Test
    fun `atividade sem kcal por minuto usa os METs`() {
        val walk = ExerciseType("w", "Caminhada", "Cardio", mets = 3.5, iconKey = "exercise.walk")

        assertEquals(exerciseKcal(3.5, 70.0, 30), exerciseKcal(walk, 70.0, 30))
    }
}

package com.projeto.wellio.domain.profile

import com.projeto.wellio.domain.model.ActivityLevel
import com.projeto.wellio.domain.model.Profile
import com.projeto.wellio.domain.model.Sex
import com.projeto.wellio.domain.weight.WeightGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ProfileTest {

    private val today = LocalDate.of(2026, 10, 20)

    @Test
    fun `idade conta so aniversarios ja feitos`() {
        assertEquals(27, age(LocalDate.of(1999, 3, 14), today))
        assertEquals(26, age(LocalDate.of(1999, 10, 21), today))
        assertEquals(27, age(LocalDate.of(1999, 10, 20), today))
    }

    @Test
    fun `altura aceita metros com virgula ou ponto e centimetros`() {
        assertEquals(178, parseHeight("1,78"))
        assertEquals(178, parseHeight("1.78"))
        assertEquals(178, parseHeight("178"))
    }

    @Test
    fun `altura rejeita texto e valores fora da faixa`() {
        assertNull(parseHeight("abc"))
        assertNull(parseHeight("90"))
        assertNull(parseHeight("2,60"))
    }

    @Test
    fun `imc com uma casa`() {
        assertEquals(22.9, bmi(72.4, 178), 0.0)
    }

    @Test
    fun `faixas do imc`() {
        assertEquals(BmiRange.Under, BmiRange.of(18.4))
        assertEquals(BmiRange.Normal, BmiRange.of(18.5))
        assertEquals(BmiRange.Over, BmiRange.of(25.0))
        assertEquals(BmiRange.Obese, BmiRange.of(30.0))
    }

    @Test
    fun `objetivo sai do peso alvo`() {
        assertEquals(Objective.Lose, objective(WeightGoal(targetKg = 70.0, startKg = 75.0), 72.4))
        assertEquals(Objective.Gain, objective(WeightGoal(targetKg = 80.0, startKg = 75.0), 72.4))
    }

    @Test
    fun `sem alvo, sem peso ou perto do alvo o objetivo e manter`() {
        assertEquals(Objective.Keep, objective(null, 72.4))
        assertEquals(Objective.Keep, objective(WeightGoal(70.0, 75.0), null))
        assertEquals(Objective.Keep, objective(WeightGoal(targetKg = 72.0, startKg = 75.0), 72.4))
    }

    private val complete = Profile(
        name = "João",
        birthDate = LocalDate.of(1999, 3, 14),
        sex = Sex.Male,
        heightCm = 178,
        activity = ActivityLevel.Moderate
    )

    @Test
    fun `sugestao pela formula de Mifflin-St Jeor com atividade e objetivo`() {
        // 10 × 72,4 + 6,25 × 178 − 5 × 27 + 5 = 1706,5; × 1,55 = 2645,1; − 500 = 2145,1 → 2150
        assertEquals(CalorieSuggestion.Ready(2150), suggestedCalories(complete, 72.4, Objective.Lose, today))
    }

    @Test
    fun `sugestao feminina usa a constante da mulher`() {
        // 10 × 60 + 6,25 × 165 − 5 × 27 − 161 = 1335,25; × 1,2 = 1602,3 → 1600
        val profile = complete.copy(sex = Sex.Female, heightCm = 165, activity = ActivityLevel.Sedentary)
        assertEquals(CalorieSuggestion.Ready(1600), suggestedCalories(profile, 60.0, Objective.Keep, today))
    }

    @Test
    fun `sem nivel de atividade conta como sedentario`() {
        val withSedentary = suggestedCalories(complete.copy(activity = ActivityLevel.Sedentary), 72.4, Objective.Keep, today)
        assertEquals(withSedentary, suggestedCalories(complete.copy(activity = null), 72.4, Objective.Keep, today))
    }

    @Test
    fun `sem dados a sugestao diz o que falta`() {
        assertEquals(
            CalorieSuggestion.Missing(listOf(MissingField.BirthDate, MissingField.Sex, MissingField.Height, MissingField.Weight)),
            suggestedCalories(Profile(name = "João"), null, Objective.Keep, today)
        )
    }

    @Test
    fun `prefiro nao informar nao tem sugestao`() {
        assertEquals(
            CalorieSuggestion.SexUndisclosed,
            suggestedCalories(complete.copy(sex = Sex.Undisclosed), 72.4, Objective.Keep, today)
        )
    }
}

package com.projeto.wellio.ui.goals

import com.projeto.wellio.domain.goals.PeriodKind
import com.projeto.wellio.domain.goals.PeriodMetric
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class PeriodTextsTest {

    private val today = LocalDate.of(2026, 10, 20)
    private val october = LocalDate.of(2026, 10, 1)..LocalDate.of(2026, 10, 31)

    @Test
    fun `nome gerado diz o valor e o periodo`() {
        assertEquals(
            "Beber 60 L em outubro",
            periodGoalTitle(PeriodMetric.WaterTotal, 60_000.0, PeriodKind.Month, october.start, october.endInclusive, false, today)
        )
        assertEquals(
            "150 min de exercício por semana",
            periodGoalTitle(PeriodMetric.ExerciseMinutesTotal, 150.0, PeriodKind.Week, today, today.plusDays(6), true, today)
        )
        assertEquals(
            "Chegar a 75,0 kg até 31/12",
            periodGoalTitle(PeriodMetric.Weight, 75.0, PeriodKind.Custom, today, LocalDate.of(2026, 12, 31), false, today)
        )
    }

    @Test
    fun `dias e treinos no singular e no plural`() {
        assertEquals("1 dia", formatMetric(PeriodMetric.WaterDays, 1.0))
        assertEquals("12 treinos", formatMetric(PeriodMetric.Workouts, 12.0))
    }

    @Test
    fun `nomes dos dias da semana sem feira`() {
        assertEquals("segunda", weekdayName(DayOfWeek.MONDAY))
        assertEquals("domingo", weekdayName(DayOfWeek.SUNDAY))
    }
}

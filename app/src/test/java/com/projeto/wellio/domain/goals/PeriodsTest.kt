package com.projeto.wellio.domain.goals

import com.projeto.wellio.domain.model.ExerciseUnit
import com.projeto.wellio.domain.model.Goal
import com.projeto.wellio.domain.model.Goals
import com.projeto.wellio.domain.model.Totals
import com.projeto.wellio.domain.weight.WeightGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class PeriodsTest {

    private fun day(d: Int, month: Int = 10) = LocalDate.of(2026, month, d)
    private val goals2000 = Goals.Default
    private val goals2500 = Goals.Default.copy(water = Goal(2500))

    @Test
    fun `cada dia usa a meta que valia nele`() {
        val history = listOf(DailyGoalsSnapshot(day(5), goals2000), DailyGoalsSnapshot(day(10), goals2500))

        assertEquals(goals2000, goalsOn(day(9), history, goals2500))
        assertEquals(goals2500, goalsOn(day(10), history, goals2000))
        assertEquals(goals2500, goalsOn(day(20), history, goals2000))
    }

    @Test
    fun `antes do historico vale a primeira meta e sem historico a atual`() {
        val history = listOf(DailyGoalsSnapshot(day(5), goals2000))

        assertEquals(goals2000, goalsOn(day(1), history, goals2500))
        assertEquals(goals2500, goalsOn(day(1), emptyList(), goals2500))
    }

    @Test
    fun `semana comeca no dia escolhido`() {
        // 20/10/2026 é uma terça
        assertEquals(day(19), weekDays(day(20), DayOfWeek.MONDAY).first())
        assertEquals(day(18), weekDays(day(20), DayOfWeek.SUNDAY).first())
        assertEquals(day(25), weekDays(day(20), DayOfWeek.MONDAY).last())
    }

    @Test
    fun `aneis fecham ao bater cada meta`() {
        val totals = Totals(waterMl = 2000, kcalConsumed = 1000, kcalBurned = 500)

        assertEquals(Rings(water = 1f, food = 0.5f, exercise = 1f), rings(totals, goals2000))
        assertEquals(2.5f / 3, rings(totals, goals2000).score, 0.001f)
        assertFalse(rings(totals, goals2000).allClosed)
        assertTrue(rings(totals.copy(kcalConsumed = 2000), goals2000).allClosed)
    }

    @Test
    fun `periodos de semana mes e ano`() {
        assertEquals(day(19)..day(25), periodContaining(PeriodKind.Week, day(20), DayOfWeek.MONDAY))
        assertEquals(day(1)..day(31), periodContaining(PeriodKind.Month, day(20), DayOfWeek.MONDAY))
        assertEquals(day(1, 1)..day(31, 12), periodContaining(PeriodKind.Year, day(20), DayOfWeek.MONDAY))
    }

    @Test
    fun `total soma os dias ate hoje`() {
        val logs = mapOf(
            day(1) to DayLog(Totals(waterMl = 2000)),
            day(2) to DayLog(Totals(waterMl = 1500)),
            day(25) to DayLog(Totals(waterMl = 9999))
        )
        val value = periodValue(PeriodMetric.WaterTotal, day(1)..day(31), today = day(20), logs = logs) { goals2000 }

        assertEquals(3500.0, value, 0.0)
    }

    @Test
    fun `dias batidos usam a meta de cada dia`() {
        val logs = mapOf(day(1) to DayLog(Totals(waterMl = 2200)), day(2) to DayLog(Totals(waterMl = 2200)))
        val goalsFor = { d: LocalDate -> if (d.isBefore(day(2))) goals2000 else goals2500 }

        assertEquals(1.0, periodValue(PeriodMetric.WaterDays, day(1)..day(7), day(7), logs, goalsFor), 0.0)
    }

    @Test
    fun `kcal conta dentro de mais ou menos 10 por cento e com algo registrado`() {
        assertTrue(dayHits(PeriodMetric.KcalDays, DayLog(Totals(kcalConsumed = 2200)), goals2000))
        assertFalse(dayHits(PeriodMetric.KcalDays, DayLog(Totals(kcalConsumed = 2300)), goals2000))
        assertFalse(dayHits(PeriodMetric.KcalDays, DayLog(), goals2000.copy(food = Goal(0))))
    }

    @Test
    fun `exercicio em dias usa a unidade da meta daquele dia`() {
        val minutes = goals2000.copy(exerciseUnit = ExerciseUnit.Minutes, exercise = Goal(30))

        assertTrue(dayHits(PeriodMetric.ExerciseDays, DayLog(Totals(exerciseMinutes = 30)), minutes))
        assertFalse(dayHits(PeriodMetric.ExerciseDays, DayLog(Totals(exerciseMinutes = 30, kcalBurned = 100)), goals2000))
    }

    @Test
    fun `treinos contam os registros`() {
        val logs = mapOf(day(1) to DayLog(workouts = 2), day(3) to DayLog(workouts = 1))

        assertEquals(3.0, periodValue(PeriodMetric.Workouts, day(1)..day(7), day(7), logs) { goals2000 }, 0.0)
    }

    private val october = PeriodGoal("m", "Beber 60 L em outubro", PeriodMetric.WaterTotal, 60_000.0, PeriodKind.Month, day(1), day(31))

    @Test
    fun `progresso com marca de onde deveria estar e projecao`() {
        val progress = periodProgress(october, value = 34_000.0, today = day(20))

        assertEquals(34f / 60f, progress.fraction, 0.001f)
        assertEquals(20f / 31f, progress.expected, 0.001f)
        assertEquals(11, progress.daysLeft)
        assertEquals(34_000.0 / 20 * 31, progress.projection!!, 0.01)
        assertFalse(progress.reached)
    }

    @Test
    fun `antes de comecar nao ha projecao`() {
        assertNull(periodProgress(october, 0.0, today = day(30, 9)).projection)
    }

    @Test
    fun `sugestao usa o periodo anterior arredondado`() {
        assertEquals(53_000.0, suggestedTarget(PeriodMetric.WaterTotal, 31, previousValue = 52_600.0, goals = goals2000), 0.0)
    }

    @Test
    fun `sem historico a sugestao e a meta diaria vezes os dias`() {
        assertEquals(62_000.0, suggestedTarget(PeriodMetric.WaterTotal, 31, null, goals2000), 0.0)
        assertEquals(210.0, suggestedTarget(PeriodMetric.ExerciseMinutesTotal, 7, null, goals2000), 0.0)
        assertEquals(5.0, suggestedTarget(PeriodMetric.ExerciseDays, 7, null, goals2000), 0.0)
    }

    @Test
    fun `fechar marca concluida ou nao e guarda o valor final`() {
        val (done, _) = closePeriod(october, 61_000.0, day(1, 11), DayOfWeek.MONDAY)
        val (missed, next) = closePeriod(october, 40_000.0, day(1, 11), DayOfWeek.MONDAY)

        assertEquals(PeriodGoalStatus.Completed, done.status)
        assertEquals(PeriodGoalStatus.Missed, missed.status)
        assertEquals(40_000.0, missed.finalValue!!, 0.0)
        assertNull(next)
    }

    @Test
    fun `recorrente comeca outra no periodo de hoje com o mesmo valor`() {
        val weekly = october.copy(kind = PeriodKind.Week, start = day(12), end = day(18), recurring = true, celebrated = true)

        // Ficou duas semanas sem abrir: a nova é a semana de hoje
        val (_, next) = closePeriod(weekly, 0.0, day(28), DayOfWeek.MONDAY)

        assertEquals(day(26)..day(31, 10).plusDays(1), next!!.start..next.end)
        assertEquals(60_000.0, next.target, 0.0)
        assertEquals(PeriodGoalStatus.Active, next.status)
        assertFalse(next.celebrated)
    }

    @Test
    fun `meta de peso mede os kg percorridos e projeta pelo ritmo`() {
        val goal = WeightGoal(
            targetKg = 70.0, startKg = 75.0, deadline = day(31, 12), since = day(1)
        )

        val progress = weightPeriodProgress(goal, currentKg = 73.0, ratePerWeek = -0.5, today = day(20))!!

        assertEquals(2.0, progress.value, 0.001)
        assertEquals(5.0, progress.target, 0.001)
        assertEquals(0.4f, progress.fraction, 0.001f)
        // 72 dias até 31/12 a 0,5 kg por semana: mais ~5,1 kg
        assertEquals(2.0 + 0.5 * 72 / 7, progress.projection!!, 0.001)
        assertTrue(progress.projection!! >= progress.target)
    }

    @Test
    fun `alvo sem prazo nao e meta de periodo`() {
        assertNull(weightPeriodProgress(WeightGoal(70.0, 75.0), 73.0, null, day(20)))
    }
}

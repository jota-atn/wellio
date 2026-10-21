package com.projeto.wellio.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.projeto.wellio.domain.model.ExerciseUnit
import com.projeto.wellio.domain.model.Goal
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.domain.model.Goals
import com.projeto.wellio.domain.weight.WeightGoal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.DayOfWeek
import java.time.LocalDate

class GoalsStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val recorded = mutableListOf<Goals>()

    private fun TestScope.store() = GoalsStore(
        PreferenceDataStoreFactory.create(scope = backgroundScope) { folder.newFile("test.preferences_pb") },
        onDailyGoalsChanged = { recorded += it }
    )

    @Test
    fun `semana comeca na segunda ate o usuario escolher`() = runTest {
        val store = store()
        assertEquals(DayOfWeek.MONDAY, store.weekStart.first())

        store.setWeekStart(DayOfWeek.SUNDAY)

        assertEquals(DayOfWeek.SUNDAY, store.weekStart.first())
    }

    @Test
    fun `mudar meta diaria ou unidade grava o historico`() = runTest {
        val store = store()

        store.setGoal(GoalType.WATER, Goal(daily = 2500))
        store.setExerciseUnit(ExerciseUnit.Minutes)

        assertEquals(2, recorded.size)
        assertEquals(2500, recorded[0].water.daily)
        assertEquals(ExerciseUnit.Minutes, recorded[1].exerciseUnit)
    }

    @Test
    fun `mudar o valor do alvo mantem o inicio e o prazo`() = runTest {
        val store = store()
        val deadline = LocalDate.of(2026, 12, 31)
        store.setWeightGoal(WeightGoal(targetKg = 70.0, startKg = 75.0, deadline = deadline))

        store.setWeightTarget(68.0, currentKg = 72.0)

        assertEquals(WeightGoal(targetKg = 68.0, startKg = 75.0, deadline = deadline), store.weightGoal.first())
    }

    @Test
    fun `alvo novo comeca no peso atual e sem prazo`() = runTest {
        val store = store()

        store.setWeightTarget(70.0, currentKg = 72.4)

        assertEquals(WeightGoal(targetKg = 70.0, startKg = 72.4), store.weightGoal.first())
    }

    @Test
    fun `remover o alvo apaga o prazo e a comemoracao`() = runTest {
        val store = store()
        store.setWeightGoal(WeightGoal(70.0, 75.0, LocalDate.of(2026, 12, 31)))
        store.setWeightCelebrated()
        assertTrue(store.weightCelebrated.first())

        store.setWeightGoal(null)

        assertEquals(null, store.weightGoal.first())
        assertFalse(store.weightCelebrated.first())
    }

    @Test
    fun `meta diaria comemora uma vez por dia`() = runTest {
        val store = store()
        val today = LocalDate.of(2026, 10, 20)

        store.markDailyCelebrated(today, GoalType.WATER)
        store.markDailyCelebrated(today, GoalType.EXERCISE)

        assertEquals(setOf(GoalType.WATER, GoalType.EXERCISE), store.dailyCelebrated(today).first())
        assertEquals(emptySet<GoalType>(), store.dailyCelebrated(today.plusDays(1)).first())

        store.markDailyCelebrated(today.plusDays(1), GoalType.FOOD)

        assertEquals(setOf(GoalType.FOOD), store.dailyCelebrated(today.plusDays(1)).first())
    }
}

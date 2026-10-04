package com.projeto.wellio.domain.goals

import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.domain.model.Goals
import com.projeto.wellio.domain.model.Totals
import java.time.LocalDate

/** "% do dia" de um dia. */
data class DayScore(val date: LocalDate, val score: Float)

/** "% do dia" (SPEC §5.1): média do atingido das 3 metas diárias, cada uma limitada a 100%. */
fun dayScore(totals: Totals, goals: Goals): Float =
    GoalType.entries.map { progress(totals.toward(it, goals), goals[it].daily) }.average().toFloat()

/** Os [count] dias que terminam em [today], do mais antigo para hoje. */
fun lastDays(today: LocalDate, count: Int = 7): List<LocalDate> =
    (count - 1 downTo 0).map { today.minusDays(it.toLong()) }

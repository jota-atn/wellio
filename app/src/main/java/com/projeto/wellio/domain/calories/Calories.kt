package com.projeto.wellio.domain.calories

import com.projeto.wellio.domain.model.ExerciseType
import kotlin.math.roundToInt

/** kcal gastas num exercício: METs × peso (kg) × horas. */
fun exerciseKcal(mets: Double, weightKg: Double, minutes: Int): Int =
    (mets * weightKg * (minutes / 60.0)).roundToInt()

/** kcal de uma atividade: pela kcal por minuto, se ela tiver (atividade própria), senão pelos METs. */
fun exerciseKcal(type: ExerciseType, weightKg: Double, minutes: Int): Int =
    type.kcalPerMinute?.let { (it * minutes).roundToInt() } ?: exerciseKcal(type.mets, weightKg, minutes)

/** kcal de uma quantidade de porções de um alimento. */
fun foodKcal(kcalPerPortion: Double, portions: Double): Int =
    (kcalPerPortion * portions).roundToInt()

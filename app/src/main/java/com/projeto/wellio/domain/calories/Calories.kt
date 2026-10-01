package com.projeto.wellio.domain.calories

import kotlin.math.roundToInt

/** kcal gastas num exercício: METs × peso (kg) × horas. */
fun exerciseKcal(mets: Double, weightKg: Double, minutes: Int): Int =
    (mets * weightKg * (minutes / 60.0)).roundToInt()

/** kcal de uma quantidade de porções de um alimento. */
fun foodKcal(kcalPerPortion: Double, portions: Double): Int =
    (kcalPerPortion * portions).roundToInt()

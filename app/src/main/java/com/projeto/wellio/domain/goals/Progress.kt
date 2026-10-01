package com.projeto.wellio.domain.goals

/** Fração da meta atingida, entre 0 e 1. Meta zero ou negativa conta como 0. */
fun progress(current: Int, goal: Int): Float =
    if (goal <= 0) 0f else (current.toFloat() / goal).coerceIn(0f, 1f)

/**
 * Ajuste de água que pode ser registrado: somar é sempre permitido (inclusive
 * acima da meta), e subtrair nunca deixa o total do dia negativo.
 */
fun waterAdjustment(currentTotalMl: Int, deltaMl: Int): Int =
    if (deltaMl >= 0) deltaMl else maxOf(deltaMl, -currentTotalMl)

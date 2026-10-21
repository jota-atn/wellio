package com.projeto.wellio.domain.goals

/** O quanto uma meta tem agora, para saber se ela acabou de ser cumprida. */
data class GoalReading(val value: Double, val target: Double) {
    val reached: Boolean get() = target > 0 && value >= target
}

/**
 * A meta acabou de ser cumprida (SPEC §9.2): passou de não cumprida para cumprida porque o
 * valor subiu (um registro). Abrir o app (sem leitura anterior), apagar dados ou baixar a
 * meta abaixo do que já foi feito não contam. Comemora toda vez que isso acontece.
 */
fun justReached(previous: GoalReading?, current: GoalReading): Boolean =
    previous != null && !previous.reached && current.reached && current.value > previous.value

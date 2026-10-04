package com.projeto.wellio.domain.water

import com.projeto.wellio.domain.model.WaterContainer
import com.projeto.wellio.domain.model.WaterEntry

const val MAX_QUICK_BUTTONS = 3
const val MAX_WATER_AMOUNT_ML = 5000

/** Recipientes que aparecem como botões rápidos no card, na ordem definida. */
fun quickButtons(containers: List<WaterContainer>, max: Int = MAX_QUICK_BUTTONS): List<WaterContainer> =
    containers.filter { it.isQuick }.sortedBy { it.position }.take(max)

/** Registro mais recente do dia, que o "Desfazer último" remove. */
fun lastEntry(entries: List<WaterEntry>): WaterEntry? = entries.maxByOrNull { it.timestamp }

fun totalMl(entries: List<WaterEntry>): Int = entries.sumOf { it.amountMl }

/** Quantidade digitada em ml; aceita só inteiros entre 1 e o máximo. */
fun parseWaterAmount(text: String): Int? =
    text.trim().toIntOrNull()?.takeIf { it in 1..MAX_WATER_AMOUNT_ML }

sealed interface ContainerValidation {
    data class Valid(val name: String, val amountMl: Int) : ContainerValidation
    data object BlankName : ContainerValidation
    data object InvalidAmount : ContainerValidation
}

fun validateContainer(name: String, amountText: String): ContainerValidation {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return ContainerValidation.BlankName
    val amount = parseWaterAmount(amountText) ?: return ContainerValidation.InvalidAmount
    return ContainerValidation.Valid(trimmed, amount)
}

/** Ícones disponíveis para recipientes (SPEC §6), na ordem do seletor. */
enum class ContainerIcon(val key: String, val label: String) {
    Copo("water.copo", "Copo"),
    Caneca("water.caneca", "Caneca"),
    Xicara("water.xicara", "Xícara"),
    Garrafa("water.garrafa", "Garrafa"),
    Squeeze("water.squeeze", "Squeeze"),
    Taca("water.taca", "Taça"),
    Termica("water.termica", "Garrafa térmica"),
    Galao("water.galao", "Galão");

    companion object {
        /** Chave desconhecida cai no copo (SPEC §2.3). */
        fun fromKey(key: String): ContainerIcon = entries.find { it.key == key } ?: Copo
    }
}

/** Ícone sugerido para um recipiente novo, pelo volume. */
fun suggestedIcon(amountMl: Int): ContainerIcon = when {
    amountMl <= 250 -> ContainerIcon.Copo
    amountMl <= 400 -> ContainerIcon.Caneca
    amountMl <= 1000 -> ContainerIcon.Garrafa
    else -> ContainerIcon.Galao
}

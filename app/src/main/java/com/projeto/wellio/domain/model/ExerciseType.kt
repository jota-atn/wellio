package com.projeto.wellio.domain.model

import com.projeto.wellio.domain.exercise.Intensity
import java.time.Instant
import java.time.LocalDate

/**
 * Uma atividade num nível (SPEC §8.1): do Compendium ([compendiumCode]) ou criada pelo
 * usuário ([isCustom]). Atividades com variações têm uma linha por [level] com o mesmo
 * [name] (ex.: Corrida leve/moderada/intensa); [levelHint] explica o nível ("~11 km/h").
 * Atividade própria pode ter [kcalPerMinute] no lugar dos METs.
 */
data class ExerciseType(
    val id: String,
    val name: String,
    val category: String,
    val mets: Double,
    val iconKey: String,
    val compendiumCode: String? = null,
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val kcalPerMinute: Double? = null,
    val level: Intensity? = null,
    val levelHint: String? = null
)

/** Um treino registrado; [kcalIsManual] quando o usuário digitou as kcal (ex.: do relógio). */
data class ExerciseEntry(
    val id: String,
    val date: LocalDate,
    val timestamp: Instant,
    val typeId: String,
    val minutes: Int,
    val kcal: Int,
    val kcalIsManual: Boolean = false
)

/** Último uso de uma atividade: quando e por quantos minutos (recentes e favoritos, SPEC §8.0). */
data class ExerciseUse(val typeId: String, val minutes: Int, val lastAt: Instant)

package com.projeto.wellio.domain.exercise

import com.projeto.wellio.domain.model.ExerciseEntry
import com.projeto.wellio.domain.model.ExerciseType
import com.projeto.wellio.domain.model.ExerciseUnit
import com.projeto.wellio.domain.model.ExerciseUse
import java.text.Normalizer
import kotlin.math.roundToInt

/** Categorias do app (SPEC §8.1), cada uma com o seu ícone desenhado. */
enum class ExerciseCategory(val label: String, val iconKey: String) {
    Cardio("Cardio", "exercise.cat.cardio"),
    Strength("Musculação", "exercise.cat.strength"),
    Fight("Lutas", "exercise.cat.fight"),
    Sports("Esportes", "exercise.cat.sports"),
    Water("Aquáticos", "exercise.cat.water"),
    Zen("Zen/Dança", "exercise.cat.zen"),
    Other("Outros", "exercise.cat.other");

    companion object {
        /** Categoria desconhecida cai em Outros. */
        fun of(label: String): ExerciseCategory = entries.firstOrNull { it.label == label } ?: Other
    }
}

/** kcal e minutos somados de um dia. */
data class ExerciseTotals(val kcal: Int, val minutes: Int) {
    /** Total na unidade da meta. */
    fun toward(unit: ExerciseUnit): Int = if (unit == ExerciseUnit.Minutes) minutes else kcal
}

fun exerciseTotals(entries: List<ExerciseEntry>): ExerciseTotals =
    ExerciseTotals(entries.sumOf { it.kcal }, entries.sumOf { it.minutes })

/** Duração padrão de uma atividade nunca usada. */
const val DEFAULT_EXERCISE_MINUTES = 30
const val MAX_EXERCISE_MINUTES = 300
const val MAX_EXERCISE_KCAL = 5000

/**
 * Uma atividade como aparece na lista (SPEC §8.1): o nome e os níveis dela (Leve, Moderada,
 * Intensa), ou um só quando não tem variações.
 */
data class ExerciseFamily(val name: String, val category: String, val iconKey: String, val variants: List<ExerciseType>) {
    val isFavorite: Boolean get() = variants.any { it.isFavorite }
    val hasLevels: Boolean get() = variants.size > 1

    /** Nível que abre por padrão: o moderado, se houver; senão o primeiro. */
    val default: ExerciseType get() = variants.firstOrNull { it.level == Intensity.Moderate } ?: variants.first()

    fun contains(typeId: String): Boolean = variants.any { it.id == typeId }
}

/** Junta os níveis de cada atividade (mesmo nome e categoria), do leve ao intenso. */
fun exerciseFamilies(types: List<ExerciseType>): List<ExerciseFamily> =
    types.groupBy { it.category to it.name }.map { (key, variants) ->
        ExerciseFamily(
            name = key.second,
            category = key.first,
            iconKey = variants.first().iconKey,
            variants = variants.sortedBy { it.level?.ordinal ?: -1 }
        )
    }

/** Um atalho da fileira de favoritos: a atividade no nível usado por último e a duração. */
data class QuickExercise(val type: ExerciseType, val minutes: Int)

/** Nível e duração do último uso da atividade; sem uso, o nível padrão e a duração padrão. */
fun lastOf(family: ExerciseFamily, uses: List<ExerciseUse>): QuickExercise {
    val use = uses.firstOrNull { family.contains(it.typeId) }
    val type = use?.let { u -> family.variants.first { it.id == u.typeId } } ?: family.default
    return QuickExercise(type, use?.minutes ?: DEFAULT_EXERCISE_MINUTES)
}

/**
 * Fileira de favoritos da tela (SPEC §8.0): primeiro as atividades favoritas, depois as
 * mais recentes que não são favoritas; cada uma no último nível e duração usados.
 * Favoritas usadas mais recentemente vêm antes; favoritas nunca usadas, por nome.
 * [uses] vem do uso mais recente para o mais antigo.
 */
fun quickExercises(types: List<ExerciseType>, uses: List<ExerciseUse>, max: Int = 8): List<QuickExercise> {
    val families = exerciseFamilies(types)
    fun lastAt(f: ExerciseFamily) = uses.firstOrNull { f.contains(it.typeId) }?.lastAt
    val favorites = families.filter { it.isFavorite }
        .sortedWith(compareByDescending<ExerciseFamily> { lastAt(it) }.thenBy { it.name })
    val recents = recentFamilies(families, uses).filter { !it.isFavorite }
    return (favorites + recents).take(max).map { lastOf(it, uses) }
}

/** Atividades usadas por último, da mais recente para a mais antiga (faixa de Recentes do registro). */
fun recentFamilies(families: List<ExerciseFamily>, uses: List<ExerciseUse>, max: Int = 6): List<ExerciseFamily> =
    uses.mapNotNull { use -> families.firstOrNull { it.contains(use.typeId) } }.distinct().take(max)

/** Texto sem acento e em minúsculas, para a busca achar "musculacao" em "Musculação". */
fun normalize(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase().trim()

/**
 * Busca (SPEC §8.0): sem acento nem maiúsculas, opcionalmente numa categoria. Nomes que
 * começam com o texto vêm antes; depois os que só contêm; empate por nome.
 */
fun searchFamilies(families: List<ExerciseFamily>, query: String, category: ExerciseCategory? = null): List<ExerciseFamily> {
    val q = normalize(query)
    return families
        .filter { category == null || ExerciseCategory.of(it.category) == category }
        .filter { q.isEmpty() || normalize(it.name).contains(q) }
        .sortedWith(compareBy<ExerciseFamily> { if (q.isNotEmpty() && normalize(it.name).startsWith(q)) 0 else 1 }.thenBy { normalize(it.name) })
}

/**
 * Mostrador de relógio (SPEC §8.0): uma volta (360°) são 60 minutos, 6° por minuto; o
 * ângulo acumula voltas. Arredonda para o minuto, entre 0 e o máximo.
 */
fun dialMinutes(totalDegrees: Float): Int = (totalDegrees / 6f).roundToInt().coerceIn(0, MAX_EXERCISE_MINUTES)

/** Ângulo do ponteiro para [minutes] (o inverso de [dialMinutes]). */
fun dialDegrees(minutes: Int): Float = minutes * 6f

/** O mostrador vibra a cada 5 minutos. */
fun isDialMark(minutes: Int): Boolean = minutes % 5 == 0

/** kcal digitadas (ex.: do relógio): inteiro entre 1 e o máximo. */
fun parseKcal(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 1..MAX_EXERCISE_KCAL }

/** Intensidade de uma atividade própria (SPEC §8), em METs. */
enum class Intensity(val label: String, val mets: Double) {
    Light("Leve", 3.0),
    Moderate("Moderada", 5.0),
    Vigorous("Intensa", 8.0)
}

sealed interface CustomExerciseValidation {
    data class Valid(val name: String, val kcalPerMinute: Double?) : CustomExerciseValidation
    data object BlankName : CustomExerciseValidation
    data object InvalidKcalPerMinute : CustomExerciseValidation
}

/** Atividade própria: nome obrigatório; kcal por minuto opcional (vírgula ou ponto, até 30). */
fun validateCustomExercise(name: String, kcalPerMinuteText: String): CustomExerciseValidation {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return CustomExerciseValidation.BlankName
    if (kcalPerMinuteText.isBlank()) return CustomExerciseValidation.Valid(trimmed, null)
    val value = kcalPerMinuteText.trim().replace(',', '.').toDoubleOrNull()
        ?.takeIf { it > 0 && it <= 30 } ?: return CustomExerciseValidation.InvalidKcalPerMinute
    return CustomExerciseValidation.Valid(trimmed, value)
}

/**
 * Fogueira (SPEC §8.0): intensidade da chama pelo progresso do dia, de brasa (0) a
 * labareda (1); passou da meta, solta faíscas.
 */
data class FireLevel(val intensity: Float, val sparks: Boolean)

fun fireLevel(progress: Float): FireLevel = FireLevel(
    intensity = progress.coerceIn(0f, 1f),
    sparks = progress >= 1f
)

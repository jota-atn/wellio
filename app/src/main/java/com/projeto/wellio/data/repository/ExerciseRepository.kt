package com.projeto.wellio.data.repository

import com.projeto.wellio.data.db.ExerciseDao
import com.projeto.wellio.data.db.ExerciseEntryEntity
import com.projeto.wellio.data.db.ExerciseTypeEntity
import com.projeto.wellio.data.seed.exerciseSeed
import com.projeto.wellio.domain.calories.exerciseKcal
import com.projeto.wellio.domain.exercise.Intensity
import com.projeto.wellio.domain.model.ExerciseEntry
import com.projeto.wellio.domain.model.ExerciseType
import com.projeto.wellio.domain.model.ExerciseUse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class ExerciseRepository(
    private val dao: ExerciseDao,
    private val weightRepository: WeightRepository,
    private val clock: Clock
) {

    val types: Flow<List<ExerciseType>> = dao.types().map { types -> types.map { it.toDomain() } }

    /** Último uso de cada atividade, do mais recente para o mais antigo. */
    val lastUses: Flow<List<ExerciseUse>> =
        dao.lastUses().map { rows -> rows.map { ExerciseUse(it.typeId, it.minutes, it.lastAt) } }

    fun entries(day: LocalDate): Flow<List<ExerciseEntry>> =
        dao.entriesOn(day).map { entries -> entries.map { it.toDomain() } }

    fun kcal(days: ClosedRange<LocalDate>): Flow<Int> = dao.kcalBetween(days.start, days.endInclusive)

    fun minutes(days: ClosedRange<LocalDate>): Flow<Int> = dao.minutesBetween(days.start, days.endInclusive)

    /** kcal de cada dia do intervalo (dias sem registro ficam de fora). */
    fun kcalByDay(days: ClosedRange<LocalDate>): Flow<Map<LocalDate, Int>> =
        dao.kcalByDay(days.start, days.endInclusive).map { rows -> rows.associate { it.date to it.total } }

    /** Minutos de cada dia do intervalo (dias sem registro ficam de fora). */
    fun minutesByDay(days: ClosedRange<LocalDate>): Flow<Map<LocalDate, Int>> =
        dao.minutesByDay(days.start, days.endInclusive).map { rows -> rows.associate { it.date to it.total } }

    /**
     * Registra um treino agora (sem [day]/[at]) ou num dia e horário escolhidos; devolve o id.
     * Sem [manualKcal], as kcal são estimadas com o peso atual (SPEC §8).
     */
    suspend fun add(
        type: ExerciseType,
        minutes: Int,
        manualKcal: Int? = null,
        day: LocalDate = LocalDate.now(clock),
        at: Instant = Instant.now(clock)
    ): String {
        val now = Instant.now(clock)
        val id = UUID.randomUUID().toString()
        dao.insertEntry(
            ExerciseEntryEntity(
                id = id,
                date = day,
                timestamp = at,
                exerciseTypeId = type.id,
                minutes = minutes,
                kcal = manualKcal ?: estimateKcal(type, minutes),
                kcalIsManual = manualKcal != null,
                createdAt = now,
                updatedAt = now
            )
        )
        return id
    }

    suspend fun update(entry: ExerciseEntry) {
        val stored = dao.entryById(entry.id) ?: return
        dao.updateEntry(
            stored.copy(
                date = entry.date,
                timestamp = entry.timestamp,
                exerciseTypeId = entry.typeId,
                minutes = entry.minutes,
                kcal = entry.kcal,
                kcalIsManual = entry.kcalIsManual,
                updatedAt = Instant.now(clock)
            )
        )
    }

    /** Apaga marcando `deletedAt`, para o "Desfazer" poder restaurar. */
    suspend fun delete(id: String) = dao.softDelete(id, Instant.now(clock))

    suspend fun restore(id: String) = dao.restore(id, Instant.now(clock))

    /** Favorito vale para a atividade inteira: todos os níveis dela. */
    suspend fun setFavorite(typeIds: List<String>, favorite: Boolean) = typeIds.forEach { dao.setFavorite(it, favorite) }

    /** Cria uma atividade própria (SPEC §8): pela intensidade em METs ou por kcal por minuto. */
    suspend fun createCustom(name: String, category: String, iconKey: String, mets: Double, kcalPerMinute: Double?): ExerciseType {
        val type = ExerciseType(
            id = "custom." + UUID.randomUUID(),
            name = name,
            category = category,
            mets = mets,
            iconKey = iconKey,
            isCustom = true,
            kcalPerMinute = kcalPerMinute
        )
        dao.insertTypes(listOf(type.toEntity()))
        return type
    }

    /** kcal estimadas com o peso atual. */
    suspend fun estimateKcal(type: ExerciseType, minutes: Int): Int =
        exerciseKcal(type, weightRepository.currentWeightKg.first(), minutes)

    suspend fun seedIfEmpty() {
        if (dao.typeCount() == 0) dao.insertTypes(exerciseSeed.map { it.toEntity() })
    }
}

private fun ExerciseTypeEntity.toDomain() = ExerciseType(
    id, name, category, mets, iconKey, compendiumCode, isCustom, isFavorite, kcalPerMinute,
    level = Intensity.entries.firstOrNull { it.name == level },
    levelHint = levelHint
)

private fun ExerciseType.toEntity() = ExerciseTypeEntity(
    id, name, category, mets, iconKey, isCustom, compendiumCode, isFavorite, kcalPerMinute,
    level = level?.name,
    levelHint = levelHint
)

private fun ExerciseEntryEntity.toDomain() =
    ExerciseEntry(id, date, timestamp, exerciseTypeId, minutes, kcal, kcalIsManual)

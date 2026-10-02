package com.projeto.wellio.data.repository

import com.projeto.wellio.data.db.WeightDao
import com.projeto.wellio.data.db.WeightEntryEntity
import com.projeto.wellio.domain.model.WeightEntry
import com.projeto.wellio.domain.weight.DEFAULT_WEIGHT_KG
import com.projeto.wellio.domain.weight.latestWeight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class WeightRepository(private val dao: WeightDao, private val clock: Clock) {

    val entries: Flow<List<WeightEntry>> = dao.entries().map { entries -> entries.map { it.toDomain() } }

    /** Peso que vale no cálculo de kcal: a pesagem mais recente, ou o padrão se não houver. */
    val currentWeightKg: Flow<Double> = entries.map { latestWeight(it)?.weightKg ?: DEFAULT_WEIGHT_KG }

    /** Registra o peso de um dia; se o dia já tem pesagem (mesmo apagada), ela é substituída. */
    suspend fun save(day: LocalDate, weightKg: Double) {
        val now = Instant.now(clock)
        val existing = dao.onDate(day)
        if (existing != null) {
            dao.update(existing.copy(weightKg = weightKg, deletedAt = null, updatedAt = now))
        } else {
            dao.insert(WeightEntryEntity(UUID.randomUUID().toString(), day, weightKg, now, now))
        }
    }

    /** Edita uma pesagem; mudar para um dia que já tem pesagem substitui a daquele dia. */
    suspend fun update(entry: WeightEntry, day: LocalDate, weightKg: Double) {
        if (day != entry.date) delete(entry.id)
        save(day, weightKg)
    }

    suspend fun delete(id: String) = dao.softDelete(id, Instant.now(clock))

    suspend fun restore(id: String) = dao.restore(id, Instant.now(clock))
}

private fun WeightEntryEntity.toDomain() = WeightEntry(id, date, weightKg)

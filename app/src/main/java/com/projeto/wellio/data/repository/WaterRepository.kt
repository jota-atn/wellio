package com.projeto.wellio.data.repository

import com.projeto.wellio.data.db.WaterContainerEntity
import com.projeto.wellio.data.db.WaterDao
import com.projeto.wellio.data.db.WaterEntryEntity
import com.projeto.wellio.domain.model.WaterContainer
import com.projeto.wellio.domain.model.WaterEntry
import com.projeto.wellio.domain.water.ContainerIcon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class WaterRepository(private val dao: WaterDao, private val clock: Clock) {

    fun totalMl(days: ClosedRange<LocalDate>): Flow<Int> = dao.totalBetween(days.start, days.endInclusive)

    /** Total de cada dia do intervalo (dias sem registro ficam de fora). */
    fun totalsByDay(days: ClosedRange<LocalDate>): Flow<Map<LocalDate, Int>> =
        dao.totalsByDay(days.start, days.endInclusive).map { rows -> rows.associate { it.date to it.total } }

    fun entries(day: LocalDate): Flow<List<WaterEntry>> =
        dao.entriesOn(day).map { entries -> entries.map { it.toDomain() } }

    val containers: Flow<List<WaterContainer>> =
        dao.containers().map { containers -> containers.map { it.toDomain() } }

    /** Registra água agora (sem [day]/[at]) ou num dia e horário escolhidos; devolve o id do registro. */
    suspend fun add(
        amountMl: Int,
        containerName: String? = null,
        day: LocalDate = LocalDate.now(clock),
        at: Instant = Instant.now(clock)
    ): String {
        val now = Instant.now(clock)
        val id = UUID.randomUUID().toString()
        dao.insert(
            WaterEntryEntity(
                id = id,
                date = day,
                timestamp = at,
                amountMl = amountMl,
                containerName = containerName,
                createdAt = now,
                updatedAt = now
            )
        )
        return id
    }

    suspend fun update(entry: WaterEntry) {
        val stored = dao.byId(entry.id) ?: return
        dao.update(
            stored.copy(
                date = entry.date,
                timestamp = entry.timestamp,
                amountMl = entry.amountMl,
                containerName = entry.containerName,
                updatedAt = Instant.now(clock)
            )
        )
    }

    /** Apaga marcando `deletedAt`, para o "Desfazer" poder restaurar. */
    suspend fun delete(id: String) = dao.softDelete(id, Instant.now(clock))

    suspend fun restore(id: String) = dao.restore(id, Instant.now(clock))

    suspend fun saveContainers(containers: List<WaterContainer>) =
        dao.upsertContainers(containers.map { it.toEntity() })

    suspend fun deleteContainer(id: String) = dao.deleteContainer(id)

    suspend fun seedIfEmpty() {
        if (dao.containerCount() == 0) saveContainers(defaultContainers)
    }

    private companion object {
        val defaultContainers = listOf(
            WaterContainer("copo", "Copo", 200, isQuick = true, position = 0, iconKey = ContainerIcon.Copo.key),
            WaterContainer("caneca", "Caneca", 300, isQuick = true, position = 1, iconKey = ContainerIcon.Caneca.key),
            WaterContainer("garrafa", "Garrafa", 500, isQuick = true, position = 2, iconKey = ContainerIcon.Garrafa.key)
        )
    }
}

private fun WaterEntryEntity.toDomain() = WaterEntry(id, date, timestamp, amountMl, containerName)
private fun WaterContainerEntity.toDomain() = WaterContainer(id, name, amountMl, isQuick, position, iconKey)
private fun WaterContainer.toEntity() = WaterContainerEntity(id, name, amountMl, isQuick, position, iconKey)

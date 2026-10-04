package com.projeto.wellio.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/** Soma de um dia (consultas agrupadas por data). */
data class DayTotal(val date: LocalDate, val total: Int)

@Dao
interface WaterDao {
    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_entry WHERE date BETWEEN :from AND :to AND deletedAt IS NULL")
    fun totalBetween(from: LocalDate, to: LocalDate): Flow<Int>

    @Query("SELECT date, SUM(amountMl) AS total FROM water_entry WHERE date BETWEEN :from AND :to AND deletedAt IS NULL GROUP BY date")
    fun totalsByDay(from: LocalDate, to: LocalDate): Flow<List<DayTotal>>

    @Query("SELECT * FROM water_entry WHERE date = :day AND deletedAt IS NULL ORDER BY timestamp")
    fun entriesOn(day: LocalDate): Flow<List<WaterEntryEntity>>

    @Query("SELECT * FROM water_entry WHERE id = :id")
    suspend fun byId(id: String): WaterEntryEntity?

    @Insert
    suspend fun insert(entry: WaterEntryEntity)

    @Update
    suspend fun update(entry: WaterEntryEntity)

    @Query("UPDATE water_entry SET deletedAt = :at, updatedAt = :at WHERE id = :id")
    suspend fun softDelete(id: String, at: Instant)

    @Query("UPDATE water_entry SET deletedAt = NULL, updatedAt = :at WHERE id = :id")
    suspend fun restore(id: String, at: Instant)

    @Query("SELECT * FROM water_container ORDER BY position")
    fun containers(): Flow<List<WaterContainerEntity>>

    @Query("SELECT COUNT(*) FROM water_container")
    suspend fun containerCount(): Int

    @Upsert
    suspend fun upsertContainers(containers: List<WaterContainerEntity>)

    @Query("DELETE FROM water_container WHERE id = :id")
    suspend fun deleteContainer(id: String)
}

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_item ORDER BY name")
    fun items(): Flow<List<FoodItemEntity>>

    @Query("SELECT COUNT(*) FROM food_item")
    suspend fun itemCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<FoodItemEntity>)

    @Query("SELECT COALESCE(SUM(kcalTotal), 0) FROM food_entry WHERE date BETWEEN :from AND :to AND deletedAt IS NULL")
    fun kcalBetween(from: LocalDate, to: LocalDate): Flow<Int>

    @Query("SELECT date, SUM(kcalTotal) AS total FROM food_entry WHERE date BETWEEN :from AND :to AND deletedAt IS NULL GROUP BY date")
    fun kcalByDay(from: LocalDate, to: LocalDate): Flow<List<DayTotal>>

    @Insert
    suspend fun insertEntry(entry: FoodEntryEntity)
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise_type ORDER BY name")
    fun types(): Flow<List<ExerciseTypeEntity>>

    @Query("SELECT COUNT(*) FROM exercise_type")
    suspend fun typeCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTypes(types: List<ExerciseTypeEntity>)

    @Query("SELECT COALESCE(SUM(kcal), 0) FROM exercise_entry WHERE date BETWEEN :from AND :to AND deletedAt IS NULL")
    fun kcalBetween(from: LocalDate, to: LocalDate): Flow<Int>

    @Query("SELECT date, SUM(kcal) AS total FROM exercise_entry WHERE date BETWEEN :from AND :to AND deletedAt IS NULL GROUP BY date")
    fun kcalByDay(from: LocalDate, to: LocalDate): Flow<List<DayTotal>>

    @Insert
    suspend fun insertEntry(entry: ExerciseEntryEntity)
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_entry WHERE deletedAt IS NULL ORDER BY date DESC")
    fun entries(): Flow<List<WeightEntryEntity>>

    /** Inclui pesagens apagadas: registrar de novo no mesmo dia reaproveita a linha. */
    @Query("SELECT * FROM weight_entry WHERE date = :day")
    suspend fun onDate(day: LocalDate): WeightEntryEntity?

    @Query("SELECT * FROM weight_entry WHERE id = :id")
    suspend fun byId(id: String): WeightEntryEntity?

    @Insert
    suspend fun insert(entry: WeightEntryEntity)

    @Update
    suspend fun update(entry: WeightEntryEntity)

    @Query("UPDATE weight_entry SET deletedAt = :at, updatedAt = :at WHERE id = :id")
    suspend fun softDelete(id: String, at: Instant)

    @Query("UPDATE weight_entry SET deletedAt = NULL, updatedAt = :at WHERE id = :id")
    suspend fun restore(id: String, at: Instant)
}

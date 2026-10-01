package com.projeto.wellio.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface WaterDao {
    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_entry WHERE date BETWEEN :from AND :to AND deletedAt IS NULL")
    fun totalBetween(from: LocalDate, to: LocalDate): Flow<Int>

    @Insert
    suspend fun insert(entry: WaterEntryEntity)
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

    @Insert
    suspend fun insertEntry(entry: ExerciseEntryEntity)
}

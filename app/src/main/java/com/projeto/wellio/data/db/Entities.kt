package com.projeto.wellio.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "water_entry", indices = [Index("date")])
data class WaterEntryEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val timestamp: Instant,
    val amountMl: Int,
    val containerName: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null
)

@Entity(tableName = "water_container")
data class WaterContainerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val amountMl: Int,
    val isQuick: Boolean,
    val position: Int,
    val iconKey: String
)

/** Uma pesagem por dia (índice único em `date`). */
@Entity(tableName = "weight_entry", indices = [Index("date", unique = true)])
data class WeightEntryEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val weightKg: Double,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null
)

@Entity(tableName = "food_item")
data class FoodItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val kcalPerPortion: Double,
    val portionDescription: String,
    val iconKey: String,
    val isFavorite: Boolean = false
)

@Entity(tableName = "food_entry", indices = [Index("date")])
data class FoodEntryEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val timestamp: Instant,
    val foodItemId: String,
    val quantity: Double,
    val kcalTotal: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null
)

@Entity(tableName = "exercise_type")
data class ExerciseTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val mets: Double,
    val iconKey: String,
    val isCustom: Boolean = false,
    val compendiumCode: String? = null,
    val isFavorite: Boolean = false,
    val kcalPerMinute: Double? = null,
    /** Nível (nome do enum Intensity) quando a atividade tem variações. */
    val level: String? = null,
    val levelHint: String? = null
)

@Entity(tableName = "exercise_entry", indices = [Index("date"), Index("exerciseTypeId")])
data class ExerciseEntryEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val timestamp: Instant,
    val exerciseTypeId: String,
    val minutes: Int,
    val kcal: Int,
    val kcalIsManual: Boolean = false,
    val source: String = "manual",
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null
)

/**
 * Metas diárias valendo a partir de [effectiveFrom] (SPEC §9.2): os dias anteriores a uma
 * mudança continuam avaliados pela meta da época. Semanais null = diária × 7.
 */
@Entity(tableName = "daily_goal_history")
data class DailyGoalHistoryEntity(
    @PrimaryKey val effectiveFrom: LocalDate,
    val waterMl: Int,
    val waterWeekly: Int?,
    val kcal: Int,
    val kcalWeekly: Int?,
    val exerciseUnit: String,
    val exercise: Int,
    val exerciseWeekly: Int?
)

/** Meta de período (SPEC §9.2); a de peso vive no peso alvo e só entra aqui no histórico. */
@Entity(tableName = "goal", indices = [Index("status")])
data class PeriodGoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val metric: String,
    val target: Double,
    val periodKind: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val recurring: Boolean,
    val status: String,
    val completedAt: Instant?,
    val celebrated: Boolean,
    /** Resultado ao terminar (o valor alcançado), para o histórico. */
    val finalValue: Double?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null
)

package com.projeto.wellio.data.repository

import com.projeto.wellio.data.db.ExerciseDao
import com.projeto.wellio.data.db.ExerciseEntryEntity
import com.projeto.wellio.data.db.ExerciseTypeEntity
import com.projeto.wellio.data.db.FoodDao
import com.projeto.wellio.data.db.FoodEntryEntity
import com.projeto.wellio.data.db.FoodItemEntity
import com.projeto.wellio.data.seed.exerciseSeed
import com.projeto.wellio.data.seed.foodSeed
import com.projeto.wellio.domain.calories.exerciseKcal
import com.projeto.wellio.domain.calories.foodKcal
import com.projeto.wellio.domain.model.ExerciseType
import com.projeto.wellio.domain.model.Food
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class FoodRepository(private val dao: FoodDao, private val clock: Clock) {

    val foods: Flow<List<Food>> = dao.items().map { items -> items.map { it.toDomain() } }

    fun kcal(days: ClosedRange<LocalDate>): Flow<Int> = dao.kcalBetween(days.start, days.endInclusive)

    /** kcal de cada dia do intervalo (dias sem registro ficam de fora). */
    fun kcalByDay(days: ClosedRange<LocalDate>): Flow<Map<LocalDate, Int>> =
        dao.kcalByDay(days.start, days.endInclusive).map { rows -> rows.associate { it.date to it.total } }

    suspend fun add(food: Food, portions: Double) {
        val now = Instant.now(clock)
        dao.insertEntry(
            FoodEntryEntity(
                id = UUID.randomUUID().toString(),
                date = LocalDate.now(clock),
                timestamp = now,
                foodItemId = food.id,
                quantity = portions,
                kcalTotal = foodKcal(food.kcalPerPortion, portions),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun seedIfEmpty() {
        if (dao.itemCount() == 0) dao.insertItems(foodSeed.map { it.toEntity() })
    }
}

class ExerciseRepository(
    private val dao: ExerciseDao,
    private val weightRepository: WeightRepository,
    private val clock: Clock
) {

    val types: Flow<List<ExerciseType>> = dao.types().map { types -> types.map { it.toDomain() } }

    fun kcal(days: ClosedRange<LocalDate>): Flow<Int> = dao.kcalBetween(days.start, days.endInclusive)

    /** kcal de cada dia do intervalo (dias sem registro ficam de fora). */
    fun kcalByDay(days: ClosedRange<LocalDate>): Flow<Map<LocalDate, Int>> =
        dao.kcalByDay(days.start, days.endInclusive).map { rows -> rows.associate { it.date to it.total } }

    suspend fun add(type: ExerciseType, minutes: Int) {
        val now = Instant.now(clock)
        val weightKg = weightRepository.currentWeightKg.first()
        dao.insertEntry(
            ExerciseEntryEntity(
                id = UUID.randomUUID().toString(),
                date = LocalDate.now(clock),
                timestamp = now,
                exerciseTypeId = type.id,
                minutes = minutes,
                kcal = exerciseKcal(type.mets, weightKg, minutes),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun seedIfEmpty() {
        if (dao.typeCount() == 0) dao.insertTypes(exerciseSeed.map { it.toEntity() })
    }
}

private fun FoodItemEntity.toDomain() = Food(id, name, category, kcalPerPortion, portionDescription, iconKey)
private fun Food.toEntity() = FoodItemEntity(id, name, category, kcalPerPortion, portionDescription, iconKey)
private fun ExerciseTypeEntity.toDomain() = ExerciseType(id, name, category, mets, iconKey)
private fun ExerciseType.toEntity() = ExerciseTypeEntity(id, name, category, mets, iconKey)

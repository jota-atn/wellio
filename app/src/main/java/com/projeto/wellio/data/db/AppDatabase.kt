package com.projeto.wellio.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        WaterEntryEntity::class,
        WaterContainerEntity::class,
        FoodItemEntity::class,
        FoodEntryEntity::class,
        ExerciseTypeEntity::class,
        ExerciseEntryEntity::class,
        WeightEntryEntity::class,
        DailyGoalHistoryEntity::class,
        PeriodGoalEntity::class
    ],
    version = 7
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun waterDao(): WaterDao
    abstract fun foodDao(): FoodDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun weightDao(): WeightDao
    abstract fun goalDao(): GoalDao
    abstract fun journeyDao(): JourneyDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "wellio.db")
                // Fase de desenvolvimento (até o fim do M1): mudanças no banco apagam os dados.
                // Depois disso, toda mudança de versão exige migração com teste (SPEC §2.7).
                // Migrações que só acrescentam tabelas, para não perder o uso de verdade que já começou
                .addMigrations(MIGRATION_6_7)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}

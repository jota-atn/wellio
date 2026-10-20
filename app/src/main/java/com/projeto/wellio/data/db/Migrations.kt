package com.projeto.wellio.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** M2b: histórico das metas diárias e metas de período (SPEC §9.2). Só cria tabelas. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `daily_goal_history` (`effectiveFrom` TEXT NOT NULL, `waterMl` INTEGER NOT NULL, " +
                "`waterWeekly` INTEGER, `kcal` INTEGER NOT NULL, `kcalWeekly` INTEGER, `exerciseUnit` TEXT NOT NULL, " +
                "`exercise` INTEGER NOT NULL, `exerciseWeekly` INTEGER, PRIMARY KEY(`effectiveFrom`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `goal` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `metric` TEXT NOT NULL, " +
                "`target` REAL NOT NULL, `periodKind` TEXT NOT NULL, `startDate` TEXT NOT NULL, `endDate` TEXT NOT NULL, " +
                "`recurring` INTEGER NOT NULL, `status` TEXT NOT NULL, `completedAt` INTEGER, `celebrated` INTEGER NOT NULL, " +
                "`finalValue` REAL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_status` ON `goal` (`status`)")
    }
}

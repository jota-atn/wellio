package com.projeto.wellio.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Uma migração, um teste (SPEC §2.7): os dados de antes continuam lá. */
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun de6Para7MantemAsPesagensECriaAsTabelasDeMetas() {
        helper.createDatabase(DB, 6).use { db ->
            db.execSQL(
                "INSERT INTO weight_entry (id, date, weightKg, createdAt, updatedAt) VALUES ('p1', '2026-10-01', 72.4, 0, 0)"
            )
        }

        helper.runMigrationsAndValidate(DB, 7, true, MIGRATION_6_7).use { db ->
            db.query("SELECT weightKg FROM weight_entry WHERE id = 'p1'").use {
                it.moveToFirst()
                assertEquals(72.4, it.getDouble(0), 0.0)
            }
            db.query("SELECT COUNT(*) FROM goal").use {
                it.moveToFirst()
                assertEquals(0, it.getInt(0))
            }
        }
    }

    private companion object {
        const val DB = "migracao-teste"
    }
}

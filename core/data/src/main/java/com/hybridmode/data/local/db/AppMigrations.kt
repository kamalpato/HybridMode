package com.hybridmode.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Ordered Room migrations for store builds.
 *
 * Version history:
 * - 1 → unknown / pre-release (destructive fallback only)
 * - 2 → uploads without owner scoping
 * - 3 → uploads.ownerUserId (per-account isolation)
 */
object AppMigrations {

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE uploads
                ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''
                """.trimIndent(),
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_2_3)
}

package com.example.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create maintenance_templates table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `maintenance_templates` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `defaultIntervalDays` INTEGER,
                `defaultIntervalMeters` INTEGER,
                `description` TEXT NOT NULL,
                `isBuiltIn` INTEGER NOT NULL,
                `isArchived` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_templates_isBuiltIn` ON `maintenance_templates` (`isBuiltIn`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_templates_isArchived` ON `maintenance_templates` (`isArchived`)")

        // 2. Create maintenance_entries table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `maintenance_entries` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `vehicleId` INTEGER NOT NULL,
                `templateId` INTEGER,
                `title` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `performedAt` INTEGER NOT NULL,
                `odometerMeters` INTEGER,
                `cost` INTEGER,
                `providerName` TEXT NOT NULL,
                `note` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`templateId`) REFERENCES `maintenance_templates`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_entries_vehicleId` ON `maintenance_entries` (`vehicleId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_entries_templateId` ON `maintenance_entries` (`templateId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_entries_performedAt` ON `maintenance_entries` (`performedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_entries_vehicleId_performedAt` ON `maintenance_entries` (`vehicleId`, `performedAt`)")

        // 3. Create maintenance_schedules table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `maintenance_schedules` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `vehicleId` INTEGER NOT NULL,
                `templateId` INTEGER,
                `title` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `dueAt` INTEGER,
                `dueOdometerMeters` INTEGER,
                `intervalDays` INTEGER,
                `intervalMeters` INTEGER,
                `lastCompletedAt` INTEGER,
                `lastCompletedOdometerMeters` INTEGER,
                `status` TEXT NOT NULL,
                `isArchived` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`templateId`) REFERENCES `maintenance_templates`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_schedules_vehicleId` ON `maintenance_schedules` (`vehicleId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_schedules_templateId` ON `maintenance_schedules` (`templateId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_schedules_dueAt` ON `maintenance_schedules` (`dueAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_schedules_dueOdometerMeters` ON `maintenance_schedules` (`dueOdometerMeters`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_schedules_status` ON `maintenance_schedules` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_schedules_vehicleId_status` ON `maintenance_schedules` (`vehicleId`, `status`)")
    }
}

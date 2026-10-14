package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.database.dao.FuelEntryDao
import com.example.core.database.dao.FuelTypeDao
import com.example.core.database.dao.MaintenanceEntryDao
import com.example.core.database.dao.MaintenanceScheduleDao
import com.example.core.database.dao.MaintenanceTemplateDao
import com.example.core.database.dao.VehicleDao
import com.example.core.database.entity.FuelEntryEntity
import com.example.core.database.entity.FuelTypeEntity
import com.example.core.database.entity.MaintenanceEntryEntity
import com.example.core.database.entity.MaintenanceScheduleEntity
import com.example.core.database.entity.MaintenanceTemplateEntity
import com.example.core.database.entity.VehicleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Database(
    entities = [
        VehicleEntity::class,
        FuelTypeEntity::class,
        FuelEntryEntity::class,
        MaintenanceTemplateEntity::class,
        MaintenanceEntryEntity::class,
        MaintenanceScheduleEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun fuelTypeDao(): FuelTypeDao
    abstract fun fuelEntryDao(): FuelEntryDao
    abstract fun maintenanceTemplateDao(): MaintenanceTemplateDao
    abstract fun maintenanceEntryDao(): MaintenanceEntryDao
    abstract fun maintenanceScheduleDao(): MaintenanceScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fueltrack.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Initial seed on first install
                            CoroutineScope(Dispatchers.IO).launch {
                                seedInitialData(getInstance(context))
                            }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // Idempotent seed check for maintenance templates if upgrading or newly added
                            CoroutineScope(Dispatchers.IO).launch {
                                seedMaintenanceTemplatesIfEmpty(getInstance(context))
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedInitialData(database: AppDatabase) {
            val now = System.currentTimeMillis()
            // 1. Initial fuel types
            if (database.fuelTypeDao().getFuelTypeCount() == 0) {
                database.fuelTypeDao().insertAll(
                    listOf(
                        FuelTypeEntity(
                            name = "RON 95-III",
                            description = "Xăng cao cấp cho xe tay ga & ô tô",
                            createdAt = now,
                            updatedAt = now
                        ),
                        FuelTypeEntity(
                            name = "E5 RON 92-II",
                            description = "Xăng sinh học phổ thông",
                            createdAt = now + 1,
                            updatedAt = now + 1
                        ),
                        FuelTypeEntity(
                            name = "DO 0,05S-II (Diesel)",
                            description = "Dầu Diesel cho xe máy dầu",
                            createdAt = now + 2,
                            updatedAt = now + 2
                        )
                    )
                )
            }
            seedMaintenanceTemplatesIfEmpty(database)
        }

        private val seedMutex = Mutex()

        private suspend fun seedMaintenanceTemplatesIfEmpty(database: AppDatabase) {
            seedMutex.withLock {
                if (database.maintenanceTemplateDao().getTemplateCount() == 0) {
                    val now = System.currentTimeMillis()
                    val builtInTemplates = listOf(
                        MaintenanceTemplateEntity(
                            name = "Thay dầu động cơ (Nhớt máy)",
                            category = "Động cơ",
                            defaultIntervalDays = 90,
                            defaultIntervalMeters = 2_000_000L, // 2,000 km
                            description = "Thay dầu nhớt bôi trơn động cơ định kỳ",
                            isBuiltIn = true,
                            isArchived = false,
                            createdAt = now,
                            updatedAt = now
                        ),
                        MaintenanceTemplateEntity(
                            name = "Thay lọc nhớt",
                            category = "Động cơ",
                            defaultIntervalDays = 180,
                            defaultIntervalMeters = 6_000_000L, // 6,000 km
                            description = "Thay lọc nhớt để loại bỏ cặn bẩn",
                            isBuiltIn = true,
                            isArchived = false,
                            createdAt = now + 1,
                            updatedAt = now + 1
                        ),
                        MaintenanceTemplateEntity(
                            name = "Thay lọc gió",
                            category = "Hút khí",
                            defaultIntervalDays = 365,
                            defaultIntervalMeters = 10_000_000L, // 10,000 km
                            description = "Vệ sinh hoặc thay lọc gió buồng đốt",
                            isBuiltIn = true,
                            isArchived = false,
                            createdAt = now + 2,
                            updatedAt = now + 2
                        ),
                        MaintenanceTemplateEntity(
                            name = "Thay bugi",
                            category = "Đánh lửa",
                            defaultIntervalDays = 365,
                            defaultIntervalMeters = 10_000_000L, // 10,000 km
                            description = "Thay bugi đánh lửa để xe vận hành êm ái",
                            isBuiltIn = true,
                            isArchived = false,
                            createdAt = now + 3,
                            updatedAt = now + 3
                        ),
                        MaintenanceTemplateEntity(
                            name = "Kiểm tra / Thay má phanh",
                            category = "Hệ thống phanh",
                            defaultIntervalDays = 180,
                            defaultIntervalMeters = 8_000_000L, // 8,000 km
                            description = "Kiểm tra độ mòn má phanh trước và sau",
                            isBuiltIn = true,
                            isArchived = false,
                            createdAt = now + 4,
                            updatedAt = now + 4
                        ),
                        MaintenanceTemplateEntity(
                            name = "Kiểm tra / Đảo lốp xe",
                            category = "Bánh xe & Lốp",
                            defaultIntervalDays = 180,
                            defaultIntervalMeters = 10_000_000L, // 10,000 km
                            description = "Kiểm tra gai lốp, áp suất và đảo lốp",
                            isBuiltIn = true,
                            isArchived = false,
                            createdAt = now + 5,
                            updatedAt = now + 5
                        ),
                        MaintenanceTemplateEntity(
                            name = "Kiểm tra bình ắc quy",
                            category = "Điện",
                            defaultIntervalDays = 180,
                            defaultIntervalMeters = null,
                            description = "Kiểm tra điện áp và cọc bình ắc quy",
                            isBuiltIn = true,
                            isArchived = false,
                            createdAt = now + 6,
                            updatedAt = now + 6
                        ),
                        MaintenanceTemplateEntity(
                            name = "Bảo dưỡng định kỳ tổng thể",
                            category = "Tổng thể",
                            defaultIntervalDays = 180,
                            defaultIntervalMeters = 5_000_000L, // 5,000 km
                            description = "Kiểm tra tổng quát xe toàn diện tại gara",
                            isBuiltIn = true,
                            isArchived = false,
                            createdAt = now + 7,
                            updatedAt = now + 7
                        )
                    )
                    // Insert only templates that do not already exist by name
                    for (template in builtInTemplates) {
                        if (database.maintenanceTemplateDao().getTemplateByName(template.name) == null) {
                            database.maintenanceTemplateDao().insertTemplate(template)
                        }
                    }
                }
            }
        }
    }
}

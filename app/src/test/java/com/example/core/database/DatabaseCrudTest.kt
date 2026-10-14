package com.example.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.dao.FuelEntryDao
import com.example.core.database.dao.FuelTypeDao
import com.example.core.database.dao.VehicleDao
import com.example.core.database.entity.FuelEntryEntity
import com.example.core.database.entity.FuelTypeEntity
import com.example.core.database.entity.VehicleEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import com.example.core.database.dao.MaintenanceEntryDao
import com.example.core.database.dao.MaintenanceScheduleDao
import com.example.core.database.dao.MaintenanceTemplateDao
import com.example.core.database.entity.MaintenanceEntryEntity
import com.example.core.database.entity.MaintenanceScheduleEntity
import com.example.core.database.entity.MaintenanceTemplateEntity

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseCrudTest {

    private lateinit var db: AppDatabase
    private lateinit var vehicleDao: VehicleDao
    private lateinit var fuelTypeDao: FuelTypeDao
    private lateinit var fuelEntryDao: FuelEntryDao
    private lateinit var maintenanceTemplateDao: MaintenanceTemplateDao
    private lateinit var maintenanceEntryDao: MaintenanceEntryDao
    private lateinit var maintenanceScheduleDao: MaintenanceScheduleDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        vehicleDao = db.vehicleDao()
        fuelTypeDao = db.fuelTypeDao()
        fuelEntryDao = db.fuelEntryDao()
        maintenanceTemplateDao = db.maintenanceTemplateDao()
        maintenanceEntryDao = db.maintenanceEntryDao()
        maintenanceScheduleDao = db.maintenanceScheduleDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testVehicleCrudAndArchive() = runBlocking {
        // 1. Insert vehicle
        val vehicle = VehicleEntity(
            name = "Honda Vision",
            type = "MOTORCYCLE",
            brand = "Honda",
            initialOdometerMeters = 10_000_000L,
            currentOdometerMeters = 10_000_000L
        )
        val vehicleId = vehicleDao.insertVehicle(vehicle)
        assertTrue(vehicleId > 0)

        // 2. Read vehicle
        val loaded = vehicleDao.getVehicleById(vehicleId)
        assertNotNull(loaded)
        assertEquals("Honda Vision", loaded?.name)
        assertEquals(10_000_000L, loaded?.currentOdometerMeters)

        // 3. Update vehicle
        val updated = loaded!!.copy(name = "Honda Vision 2024", currentOdometerMeters = 10_500_000L)
        vehicleDao.updateVehicle(updated)
        val afterUpdate = vehicleDao.getVehicleById(vehicleId)
        assertEquals("Honda Vision 2024", afterUpdate?.name)
        assertEquals(10_500_000L, afterUpdate?.currentOdometerMeters)

        // 4. Archive vehicle (soft delete)
        vehicleDao.archiveVehicle(vehicleId)
        val activeList = vehicleDao.observeActiveVehicles().first()
        assertTrue(activeList.none { it.id == vehicleId })

        val allList = vehicleDao.observeAllVehicles().first()
        assertTrue(allList.any { it.id == vehicleId && it.isArchived })
    }

    @Test
    fun testFuelTypeCrudAndArchive() = runBlocking {
        val ft = FuelTypeEntity(name = "RON 95-V", description = "Xăng cao cấp")
        val ftId = fuelTypeDao.insertFuelType(ft)
        assertTrue(ftId > 0)

        val loaded = fuelTypeDao.getFuelTypeById(ftId)
        assertEquals("RON 95-V", loaded?.name)

        // Update
        fuelTypeDao.updateFuelType(loaded!!.copy(description = "Tiêu chuẩn Euro 5"))
        val updated = fuelTypeDao.getFuelTypeById(ftId)
        assertEquals("Tiêu chuẩn Euro 5", updated?.description)

        // Archive
        fuelTypeDao.archiveFuelType(ftId)
        val activeTypes = fuelTypeDao.observeActiveFuelTypes().first()
        assertTrue(activeTypes.none { it.id == ftId })
    }

    @Test
    fun testFuelEntryCrudAndCalculations() = runBlocking {
        val vehicleId = vehicleDao.insertVehicle(
            VehicleEntity(name = "Toyota Vios", type = "CAR", initialOdometerMeters = 20_000_000L, currentOdometerMeters = 20_000_000L)
        )
        val ftId = fuelTypeDao.insertFuelType(FuelTypeEntity(name = "RON 95-III"))

        // Insert Entry 1
        val entry1 = FuelEntryEntity(
            vehicleId = vehicleId,
            timestamp = 1000L,
            odometerMeters = 20_000_000L,
            fuelTypeId = ftId,
            pricePerLiter = 25000L,
            amountPaid = 500_000L,
            liters = 20.0,
            isFullTank = true
        )
        val entry1Id = fuelEntryDao.insertEntry(entry1)
        assertTrue(entry1Id > 0)

        // Insert Entry 2
        val entry2 = FuelEntryEntity(
            vehicleId = vehicleId,
            timestamp = 2000L,
            odometerMeters = 20_350_000L,
            fuelTypeId = ftId,
            pricePerLiter = 25000L,
            amountPaid = 525_000L,
            liters = 21.0,
            isFullTank = true
        )
        val entry2Id = fuelEntryDao.insertEntry(entry2)
        assertTrue(entry2Id > 0)

        val entries = fuelEntryDao.observeEntriesForVehicle(vehicleId).first()
        assertEquals(2, entries.size)
        // Descending order by ODO
        assertEquals(20_350_000L, entries[0].odometerMeters)
        assertEquals(20_000_000L, entries[1].odometerMeters)

        // Update Entry 2
        fuelEntryDao.updateEntry(entry2.copy(id = entry2Id, note = "Đã sửa ghi chú"))
        val loadedEntry2 = fuelEntryDao.getEntryById(entry2Id)
        assertEquals("Đã sửa ghi chú", loadedEntry2?.note)

        // Delete Entry 1
        fuelEntryDao.deleteEntryById(entry1Id)
        assertNull(fuelEntryDao.getEntryById(entry1Id))
        val remaining = fuelEntryDao.observeEntriesForVehicle(vehicleId).first()
        assertEquals(1, remaining.size)
    }

    @Test
    fun testMaintenanceTemplateCrudAndArchive() = runBlocking {
        val template = MaintenanceTemplateEntity(
            name = "Thay nhớt hộp số",
            category = "Truyền động",
            defaultIntervalDays = 180,
            defaultIntervalMeters = 5_000_000L,
            description = "Bảo dưỡng dầu láp / hộp số",
            isBuiltIn = false,
            isArchived = false
        )
        val templateId = maintenanceTemplateDao.insertTemplate(template)
        assertTrue(templateId > 0)

        val loaded = maintenanceTemplateDao.getTemplateById(templateId)
        assertNotNull(loaded)
        assertEquals("Thay nhớt hộp số", loaded?.name)
        assertEquals(5_000_000L, loaded?.defaultIntervalMeters)

        // Update
        val updated = loaded!!.copy(description = "Cập nhật mô tả dầu láp")
        maintenanceTemplateDao.updateTemplate(updated)
        assertEquals("Cập nhật mô tả dầu láp", maintenanceTemplateDao.getTemplateById(templateId)?.description)

        // Archive
        maintenanceTemplateDao.archiveTemplate(templateId)
        val activeTemplates = maintenanceTemplateDao.observeActiveTemplates().first()
        assertTrue(activeTemplates.none { it.id == templateId })

        val allTemplates = maintenanceTemplateDao.observeAllTemplates().first()
        assertTrue(allTemplates.any { it.id == templateId && it.isArchived })
    }

    @Test
    fun testMaintenanceEntryCrud() = runBlocking {
        val vehicleId = vehicleDao.insertVehicle(
            VehicleEntity(name = "Yamaha NVX", type = "MOTORCYCLE", initialOdometerMeters = 5_000_000L, currentOdometerMeters = 5_000_000L)
        )
        val entry = MaintenanceEntryEntity(
            vehicleId = vehicleId,
            title = "Thay bugi Iridium",
            category = "Đánh lửa",
            description = "Bugi Denso Iridium IU24",
            performedAt = System.currentTimeMillis(),
            odometerMeters = 5_200_000L,
            cost = 220_000L,
            providerName = "Gara Honda Phát Tiến",
            note = "Xe êm và nhạy nổ"
        )
        val entryId = maintenanceEntryDao.insertEntry(entry)
        assertTrue(entryId > 0)

        val loaded = maintenanceEntryDao.getEntryById(entryId)
        assertNotNull(loaded)
        assertEquals("Thay bugi Iridium", loaded?.title)
        assertEquals(220_000L, loaded?.cost)

        // Observe for vehicle
        val list = maintenanceEntryDao.observeEntriesForVehicle(vehicleId).first()
        assertEquals(1, list.size)
        assertEquals(entryId, list[0].id)

        // Delete
        maintenanceEntryDao.deleteEntryById(entryId)
        assertNull(maintenanceEntryDao.getEntryById(entryId))
    }

    @Test
    fun testMaintenanceScheduleCrudAndLifecycle() = runBlocking {
        val vehicleId = vehicleDao.insertVehicle(
            VehicleEntity(name = "Mazda CX-5", type = "CAR", initialOdometerMeters = 30_000_000L, currentOdometerMeters = 30_000_000L)
        )
        val now = System.currentTimeMillis()
        val schedule = MaintenanceScheduleEntity(
            vehicleId = vehicleId,
            title = "Bảo dưỡng 40,000 km",
            category = "Định kỳ",
            dueAt = now + (90 * 86_400_000L),
            dueOdometerMeters = 40_000_000L,
            intervalDays = 180,
            intervalMeters = 10_000_000L,
            status = "ACTIVE",
            isArchived = false
        )
        val scheduleId = maintenanceScheduleDao.insertSchedule(schedule)
        assertTrue(scheduleId > 0)

        val loaded = maintenanceScheduleDao.getScheduleById(scheduleId)
        assertNotNull(loaded)
        assertEquals("ACTIVE", loaded?.status)
        assertEquals(40_000_000L, loaded?.dueOdometerMeters)

        // Update status to COMPLETED
        maintenanceScheduleDao.updateStatus(scheduleId, "COMPLETED")
        assertEquals("COMPLETED", maintenanceScheduleDao.getScheduleById(scheduleId)?.status)

        // Archive schedule
        maintenanceScheduleDao.archiveSchedule(scheduleId)
        val active = maintenanceScheduleDao.observeActiveSchedulesForVehicle(vehicleId).first()
        assertTrue(active.none { it.id == scheduleId })
    }

    @Test
    fun testMigration1To2PreservesData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sqliteCallback = object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `vehicles` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `brand` TEXT NOT NULL,
                        `model` TEXT NOT NULL,
                        `year` INTEGER,
                        `licensePlate` TEXT NOT NULL,
                        `initialOdometerMeters` INTEGER NOT NULL,
                        `currentOdometerMeters` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `fuel_types` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `fuel_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `vehicleId` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `odometerMeters` INTEGER NOT NULL,
                        `fuelTypeId` INTEGER NOT NULL,
                        `pricePerLiter` INTEGER NOT NULL,
                        `amountPaid` INTEGER NOT NULL,
                        `liters` REAL NOT NULL,
                        `isFullTank` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`fuelTypeId`) REFERENCES `fuel_types`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                // Seed V1 data
                db.execSQL("INSERT INTO vehicles (name, type, brand, model, licensePlate, initialOdometerMeters, currentOdometerMeters, isArchived, createdAt, updatedAt) VALUES ('Air Blade', 'MOTORCYCLE', 'Honda', '150', '29A-12345', 10000000, 10000000, 0, 1000, 1000)")
                db.execSQL("INSERT INTO fuel_types (name, description, isArchived, createdAt, updatedAt) VALUES ('RON 95', 'Xang 95', 0, 1000, 1000)")
            }

            override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }

        val openHelper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(sqliteCallback)
                .build()
        )
        val writableDb = openHelper.writableDatabase

        // Execute MIGRATION_1_2
        MIGRATION_1_2.migrate(writableDb)

        // Verify existing V1 data remains intact
        val vehicleCursor = writableDb.query("SELECT name, currentOdometerMeters FROM vehicles")
        assertTrue(vehicleCursor.moveToFirst())
        assertEquals("Air Blade", vehicleCursor.getString(0))
        assertEquals(10_000_000L, vehicleCursor.getLong(1))
        vehicleCursor.close()

        val fuelTypeCursor = writableDb.query("SELECT name FROM fuel_types")
        assertTrue(fuelTypeCursor.moveToFirst())
        assertEquals("RON 95", fuelTypeCursor.getString(0))
        fuelTypeCursor.close()

        // Verify new maintenance tables exist and work
        writableDb.execSQL("INSERT INTO maintenance_templates (name, category, description, isBuiltIn, isArchived, createdAt, updatedAt) VALUES ('Thay dầu', 'Động cơ', 'Nhớt', 1, 0, 1000, 1000)")
        val templateCursor = writableDb.query("SELECT name FROM maintenance_templates")
        assertTrue(templateCursor.moveToFirst())
        assertEquals("Thay dầu", templateCursor.getString(0))
        templateCursor.close()

        writableDb.close()
    }
}

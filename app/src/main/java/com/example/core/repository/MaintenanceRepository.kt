package com.example.core.repository

import com.example.core.database.AppDatabase
import com.example.core.database.dao.MaintenanceEntryDao
import com.example.core.database.dao.MaintenanceScheduleDao
import com.example.core.database.dao.MaintenanceTemplateDao
import com.example.core.database.entity.MaintenanceEntryEntity
import com.example.core.database.entity.MaintenanceScheduleEntity
import com.example.core.database.entity.MaintenanceTemplateEntity
import com.example.core.model.MaintenanceEntry
import com.example.core.model.MaintenanceSchedule
import com.example.core.model.MaintenanceStats
import com.example.core.model.MaintenanceTemplate
import com.example.core.model.MonthlyCostStat
import com.example.core.model.ScheduleLifecycleStatus
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

interface MaintenanceRepository {
    // Templates
    fun observeActiveTemplates(): Flow<List<MaintenanceTemplate>>
    fun observeAllTemplates(): Flow<List<MaintenanceTemplate>>
    suspend fun getTemplateById(id: Long): MaintenanceTemplate?
    suspend fun insertTemplate(template: MaintenanceTemplate): Long
    suspend fun updateTemplate(template: MaintenanceTemplate)
    suspend fun archiveTemplate(id: Long)
    suspend fun unarchiveTemplate(id: Long)

    // Entries (History)
    fun observeEntriesForVehicle(vehicleId: Long): Flow<List<MaintenanceEntry>>
    fun observeAllEntries(): Flow<List<MaintenanceEntry>>
    suspend fun getEntryById(id: Long): MaintenanceEntry?
    suspend fun insertEntry(entry: MaintenanceEntry): Long
    suspend fun updateEntry(entry: MaintenanceEntry)
    suspend fun deleteEntry(id: Long)

    // Schedules
    fun observeActiveSchedulesForVehicle(vehicleId: Long): Flow<List<MaintenanceSchedule>>
    fun observeAllActiveSchedules(): Flow<List<MaintenanceSchedule>>
    fun observeAllSchedulesForVehicle(vehicleId: Long): Flow<List<MaintenanceSchedule>>
    suspend fun getScheduleById(id: Long): MaintenanceSchedule?
    suspend fun insertSchedule(schedule: MaintenanceSchedule): Long
    suspend fun updateSchedule(schedule: MaintenanceSchedule)
    suspend fun cancelSchedule(id: Long)
    suspend fun archiveSchedule(id: Long)
    suspend fun deleteSchedule(id: Long)

    // Transaction: Complete schedule and record entry atomically
    suspend fun completeScheduleWithEntry(
        scheduleId: Long,
        entry: MaintenanceEntry
    ): Pair<Long, MaintenanceSchedule?>

    // Cost Statistics calculation
    fun computeMaintenanceStats(entries: List<MaintenanceEntry>): MaintenanceStats
}

@javax.inject.Singleton
class MaintenanceRepositoryImpl @javax.inject.Inject constructor(
    private val database: AppDatabase,
    private val templateDao: MaintenanceTemplateDao,
    private val entryDao: MaintenanceEntryDao,
    private val scheduleDao: MaintenanceScheduleDao,
    private val workScheduler: com.example.core.work.MaintenanceWorkScheduler? = null
) : MaintenanceRepository {

    override fun observeActiveTemplates(): Flow<List<MaintenanceTemplate>> {
        return templateDao.observeActiveTemplates().map { list -> list.map { it.toDomain() } }
    }

    override fun observeAllTemplates(): Flow<List<MaintenanceTemplate>> {
        return templateDao.observeAllTemplates().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTemplateById(id: Long): MaintenanceTemplate? {
        return templateDao.getTemplateById(id)?.toDomain()
    }

    override suspend fun insertTemplate(template: MaintenanceTemplate): Long {
        return templateDao.insertTemplate(MaintenanceTemplateEntity.fromDomain(template))
    }

    override suspend fun updateTemplate(template: MaintenanceTemplate) {
        templateDao.updateTemplate(MaintenanceTemplateEntity.fromDomain(template))
    }

    override suspend fun archiveTemplate(id: Long) {
        templateDao.archiveTemplate(id)
    }

    override suspend fun unarchiveTemplate(id: Long) {
        templateDao.unarchiveTemplate(id)
    }

    override fun observeEntriesForVehicle(vehicleId: Long): Flow<List<MaintenanceEntry>> {
        return entryDao.observeEntriesForVehicle(vehicleId).map { list -> list.map { it.toDomain() } }
    }

    override fun observeAllEntries(): Flow<List<MaintenanceEntry>> {
        return entryDao.observeAllEntries().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getEntryById(id: Long): MaintenanceEntry? {
        return entryDao.getEntryById(id)?.toDomain()
    }

    override suspend fun insertEntry(entry: MaintenanceEntry): Long {
        val entryId = entryDao.insertEntry(MaintenanceEntryEntity.fromDomain(entry))
        if (entry.odometerMeters != null) {
            workScheduler?.evaluateOdometerReminders(entry.vehicleId, entry.odometerMeters)
        }
        return entryId
    }

    override suspend fun updateEntry(entry: MaintenanceEntry) {
        entryDao.updateEntry(MaintenanceEntryEntity.fromDomain(entry))
        if (entry.odometerMeters != null) {
            workScheduler?.evaluateOdometerReminders(entry.vehicleId, entry.odometerMeters)
        }
    }

    override suspend fun deleteEntry(id: Long) {
        entryDao.deleteEntryById(id)
    }

    override fun observeActiveSchedulesForVehicle(vehicleId: Long): Flow<List<MaintenanceSchedule>> {
        return scheduleDao.observeActiveSchedulesForVehicle(vehicleId).map { list -> list.map { it.toDomain() } }
    }

    override fun observeAllActiveSchedules(): Flow<List<MaintenanceSchedule>> {
        return scheduleDao.observeAllActiveSchedules().map { list -> list.map { it.toDomain() } }
    }

    override fun observeAllSchedulesForVehicle(vehicleId: Long): Flow<List<MaintenanceSchedule>> {
        return scheduleDao.observeAllSchedulesForVehicle(vehicleId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getScheduleById(id: Long): MaintenanceSchedule? {
        return scheduleDao.getScheduleById(id)?.toDomain()
    }

    override suspend fun insertSchedule(schedule: MaintenanceSchedule): Long {
        val id = scheduleDao.insertSchedule(MaintenanceScheduleEntity.fromDomain(schedule))
        val inserted = schedule.copy(id = id)
        workScheduler?.scheduleDateReminders(inserted)
        return id
    }

    override suspend fun updateSchedule(schedule: MaintenanceSchedule) {
        scheduleDao.updateSchedule(MaintenanceScheduleEntity.fromDomain(schedule))
        workScheduler?.scheduleDateReminders(schedule)
    }

    override suspend fun cancelSchedule(id: Long) {
        scheduleDao.updateStatus(id, ScheduleLifecycleStatus.CANCELLED.name)
        workScheduler?.cancelReminders(id)
    }

    override suspend fun archiveSchedule(id: Long) {
        scheduleDao.archiveSchedule(id)
        workScheduler?.cancelReminders(id)
    }

    override suspend fun deleteSchedule(id: Long) {
        scheduleDao.deleteScheduleById(id)
        workScheduler?.cancelReminders(id)
    }

    /**
     * Executes completing a schedule inside a single atomic database transaction.
     * Records maintenance history and updates the schedule.
     * If the schedule is recurring (intervalDays or intervalMeters != null),
     * computes the NEXT due date/ODO from the actual completion time and ODO.
     */
    override suspend fun completeScheduleWithEntry(
        scheduleId: Long,
        entry: MaintenanceEntry
    ): Pair<Long, MaintenanceSchedule?> {
        val result = database.withTransaction {
            val now = System.currentTimeMillis()
            // 1. Insert maintenance entry history
            val entryEntity = MaintenanceEntryEntity.fromDomain(entry.copy(createdAt = now, updatedAt = now))
            val entryId = entryDao.insertEntry(entryEntity)

            // 2. Fetch existing schedule
            val scheduleEntity = scheduleDao.getScheduleById(scheduleId)
            var updatedSchedule: MaintenanceSchedule? = null

            if (scheduleEntity != null) {
                val hasInterval = (scheduleEntity.intervalDays != null && scheduleEntity.intervalDays > 0) ||
                        (scheduleEntity.intervalMeters != null && scheduleEntity.intervalMeters > 0)

                if (hasInterval) {
                    // Recurring schedule: calculate next due based on ACTUAL performed date & ODO
                    val nextDueAt = if (scheduleEntity.intervalDays != null && scheduleEntity.intervalDays > 0) {
                        entry.performedAt + (scheduleEntity.intervalDays * 86_400_000L)
                    } else null

                    val nextDueOdoMeters = if (scheduleEntity.intervalMeters != null && scheduleEntity.intervalMeters > 0 && entry.odometerMeters != null) {
                        entry.odometerMeters + scheduleEntity.intervalMeters
                    } else if (scheduleEntity.intervalMeters != null && scheduleEntity.intervalMeters > 0 && scheduleEntity.dueOdometerMeters != null) {
                        // Fallback if user didn't enter actual ODO
                        scheduleEntity.dueOdometerMeters + scheduleEntity.intervalMeters
                    } else null

                    val nextSchedule = scheduleEntity.copy(
                        dueAt = nextDueAt,
                        dueOdometerMeters = nextDueOdoMeters,
                        lastCompletedAt = entry.performedAt,
                        lastCompletedOdometerMeters = entry.odometerMeters,
                        status = ScheduleLifecycleStatus.ACTIVE.name,
                        updatedAt = now
                    )
                    scheduleDao.updateSchedule(nextSchedule)
                    updatedSchedule = nextSchedule.toDomain()
                } else {
                    // Non-recurring schedule: mark as COMPLETED
                    val completedSchedule = scheduleEntity.copy(
                        lastCompletedAt = entry.performedAt,
                        lastCompletedOdometerMeters = entry.odometerMeters,
                        status = ScheduleLifecycleStatus.COMPLETED.name,
                        updatedAt = now
                    )
                    scheduleDao.updateSchedule(completedSchedule)
                    updatedSchedule = completedSchedule.toDomain()
                }
            }

            Pair(entryId, updatedSchedule)
        }

        workScheduler?.cancelReminders(scheduleId)
        if (result.second != null && result.second?.status == ScheduleLifecycleStatus.ACTIVE) {
            workScheduler?.scheduleDateReminders(result.second!!)
        }
        return result
    }

    override fun computeMaintenanceStats(entries: List<MaintenanceEntry>): MaintenanceStats {
        if (entries.isEmpty()) return MaintenanceStats()

        val validEntriesWithCost = entries.filter { it.cost != null && it.cost > 0 }
        val totalCost = validEntriesWithCost.fold(0L) { acc, e ->
            try {
                Math.addExact(acc, e.cost ?: 0L)
            } catch (_: ArithmeticException) {
                Long.MAX_VALUE
            }
        }

        val cal = Calendar.getInstance()
        val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

        val monthlyCost = validEntriesWithCost.filter {
            SimpleDateFormat("yyyy-MM", Locale.US).format(Date(it.performedAt)) == currentMonthKey
        }.fold(0L) { acc, e ->
            try {
                Math.addExact(acc, e.cost ?: 0L)
            } catch (_: ArithmeticException) {
                Long.MAX_VALUE
            }
        }

        // Group by category with overflow protection
        val costByCategory = validEntriesWithCost
            .groupBy { it.category }
            .mapValues { entry ->
                entry.value.fold(0L) { acc, e ->
                    try {
                        Math.addExact(acc, e.cost ?: 0L)
                    } catch (_: ArithmeticException) {
                        Long.MAX_VALUE
                    }
                }
            }

        // Group by month with overflow protection
        val monthFormatKey = SimpleDateFormat("yyyy-MM", Locale.US)
        val monthFormatDisplay = SimpleDateFormat("'Tháng' M, yyyy", Locale.forLanguageTag("vi-VN"))
        val costByMonth = validEntriesWithCost
            .groupBy { monthFormatKey.format(Date(it.performedAt)) }
            .map { (key, monthEntries) ->
                val display = monthFormatDisplay.format(Date(monthEntries.first().performedAt))
                val monthTotal = monthEntries.fold(0L) { acc, e ->
                    try {
                        Math.addExact(acc, e.cost ?: 0L)
                    } catch (_: ArithmeticException) {
                        Long.MAX_VALUE
                    }
                }
                MonthlyCostStat(
                    yearMonthKey = key,
                    displayName = display,
                    costVnd = monthTotal,
                    entriesCount = monthEntries.size
                )
            }
            .sortedByDescending { it.yearMonthKey }

        return MaintenanceStats(
            totalCostVnd = totalCost,
            monthlyCostVnd = monthlyCost,
            totalEntriesCount = entries.size,
            costByCategory = costByCategory,
            costByMonth = costByMonth
        )
    }
}

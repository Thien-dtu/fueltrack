package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.MaintenanceSchedule
import com.example.core.model.ScheduleLifecycleStatus

@Entity(
    tableName = "maintenance_schedules",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = MaintenanceTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["vehicleId"]),
        Index(value = ["templateId"]),
        Index(value = ["dueAt"]),
        Index(value = ["dueOdometerMeters"]),
        Index(value = ["status"]),
        Index(value = ["vehicleId", "status"])
    ]
)
data class MaintenanceScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "vehicleId")
    val vehicleId: Long,
    @ColumnInfo(name = "templateId")
    val templateId: Long? = null,
    val title: String,
    val category: String,
    @ColumnInfo(name = "dueAt")
    val dueAt: Long? = null,
    @ColumnInfo(name = "dueOdometerMeters")
    val dueOdometerMeters: Long? = null,
    @ColumnInfo(name = "intervalDays")
    val intervalDays: Int? = null,
    @ColumnInfo(name = "intervalMeters")
    val intervalMeters: Long? = null,
    @ColumnInfo(name = "lastCompletedAt")
    val lastCompletedAt: Long? = null,
    @ColumnInfo(name = "lastCompletedOdometerMeters")
    val lastCompletedOdometerMeters: Long? = null,
    val status: String, // ACTIVE, COMPLETED, CANCELLED
    @ColumnInfo(name = "isArchived")
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): MaintenanceSchedule {
        return MaintenanceSchedule(
            id = id,
            vehicleId = vehicleId,
            templateId = templateId,
            title = title,
            category = category,
            dueAt = dueAt,
            dueOdometerMeters = dueOdometerMeters,
            intervalDays = intervalDays,
            intervalMeters = intervalMeters,
            lastCompletedAt = lastCompletedAt,
            lastCompletedOdometerMeters = lastCompletedOdometerMeters,
            status = try {
                ScheduleLifecycleStatus.valueOf(status)
            } catch (e: IllegalArgumentException) {
                ScheduleLifecycleStatus.ACTIVE
            },
            isArchived = isArchived,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(domain: MaintenanceSchedule): MaintenanceScheduleEntity {
            return MaintenanceScheduleEntity(
                id = domain.id,
                vehicleId = domain.vehicleId,
                templateId = domain.templateId,
                title = domain.title,
                category = domain.category,
                dueAt = domain.dueAt,
                dueOdometerMeters = domain.dueOdometerMeters,
                intervalDays = domain.intervalDays,
                intervalMeters = domain.intervalMeters,
                lastCompletedAt = domain.lastCompletedAt,
                lastCompletedOdometerMeters = domain.lastCompletedOdometerMeters,
                status = domain.status.name,
                isArchived = domain.isArchived,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt
            )
        }
    }
}

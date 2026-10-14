package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.MaintenanceEntry

@Entity(
    tableName = "maintenance_entries",
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
        Index(value = ["performedAt"]),
        Index(value = ["vehicleId", "performedAt"])
    ]
)
data class MaintenanceEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "vehicleId")
    val vehicleId: Long,
    @ColumnInfo(name = "templateId")
    val templateId: Long? = null,
    val title: String,
    val category: String,
    val description: String = "",
    @ColumnInfo(name = "performedAt")
    val performedAt: Long,
    @ColumnInfo(name = "odometerMeters")
    val odometerMeters: Long? = null,
    val cost: Long? = null, // in VND, null if unrecorded/unknown
    val providerName: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): MaintenanceEntry {
        return MaintenanceEntry(
            id = id,
            vehicleId = vehicleId,
            templateId = templateId,
            title = title,
            category = category,
            description = description,
            performedAt = performedAt,
            odometerMeters = odometerMeters,
            cost = cost,
            providerName = providerName,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(domain: MaintenanceEntry): MaintenanceEntryEntity {
            return MaintenanceEntryEntity(
                id = domain.id,
                vehicleId = domain.vehicleId,
                templateId = domain.templateId,
                title = domain.title,
                category = domain.category,
                description = domain.description,
                performedAt = domain.performedAt,
                odometerMeters = domain.odometerMeters,
                cost = domain.cost,
                providerName = domain.providerName,
                note = domain.note,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt
            )
        }
    }
}

package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.MaintenanceTemplate

@Entity(
    tableName = "maintenance_templates",
    indices = [
        Index(value = ["isBuiltIn"]),
        Index(value = ["isArchived"])
    ]
)
data class MaintenanceTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    @ColumnInfo(name = "defaultIntervalDays")
    val defaultIntervalDays: Int?,
    @ColumnInfo(name = "defaultIntervalMeters")
    val defaultIntervalMeters: Long?,
    val description: String = "",
    @ColumnInfo(name = "isBuiltIn")
    val isBuiltIn: Boolean = false,
    @ColumnInfo(name = "isArchived")
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): MaintenanceTemplate {
        return MaintenanceTemplate(
            id = id,
            name = name,
            category = category,
            defaultIntervalDays = defaultIntervalDays,
            defaultIntervalMeters = defaultIntervalMeters,
            description = description,
            isBuiltIn = isBuiltIn,
            isArchived = isArchived,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(domain: MaintenanceTemplate): MaintenanceTemplateEntity {
            return MaintenanceTemplateEntity(
                id = domain.id,
                name = domain.name,
                category = domain.category,
                defaultIntervalDays = domain.defaultIntervalDays,
                defaultIntervalMeters = domain.defaultIntervalMeters,
                description = domain.description,
                isBuiltIn = domain.isBuiltIn,
                isArchived = domain.isArchived,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt
            )
        }
    }
}

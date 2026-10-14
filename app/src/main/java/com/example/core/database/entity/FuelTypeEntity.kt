package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.FuelType

@Entity(tableName = "fuel_types")
data class FuelTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
) {
    fun toDomain(): FuelType {
        return FuelType(
            id = id,
            name = name,
            description = description,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isArchived = isArchived
        )
    }

    companion object {
        fun fromDomain(domain: FuelType): FuelTypeEntity {
            return FuelTypeEntity(
                id = domain.id,
                name = domain.name,
                description = domain.description,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
                isArchived = domain.isArchived
            )
        }
    }
}

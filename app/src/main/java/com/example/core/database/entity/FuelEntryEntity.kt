package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.FuelEntry

@Entity(
    tableName = "fuel_entries",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = FuelTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["fuelTypeId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["vehicleId"]),
        Index(value = ["fuelTypeId"]),
        Index(value = ["timestamp"]),
        Index(value = ["odometerMeters"]),
        Index(value = ["vehicleId", "timestamp"]),
        Index(value = ["vehicleId", "odometerMeters"])
    ]
)
data class FuelEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "vehicleId")
    val vehicleId: Long,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "odometerMeters")
    val odometerMeters: Long,
    @ColumnInfo(name = "fuelTypeId")
    val fuelTypeId: Long,
    val pricePerLiter: Long, // VND
    val amountPaid: Long,   // VND
    val liters: Double,
    val isFullTank: Boolean,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): FuelEntry {
        return FuelEntry(
            id = id,
            vehicleId = vehicleId,
            timestamp = timestamp,
            odometerMeters = odometerMeters,
            fuelTypeId = fuelTypeId,
            pricePerLiter = pricePerLiter,
            amountPaid = amountPaid,
            liters = liters,
            isFullTank = isFullTank,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(domain: FuelEntry): FuelEntryEntity {
            return FuelEntryEntity(
                id = domain.id,
                vehicleId = domain.vehicleId,
                timestamp = domain.timestamp,
                odometerMeters = domain.odometerMeters,
                fuelTypeId = domain.fuelTypeId,
                pricePerLiter = domain.pricePerLiter,
                amountPaid = domain.amountPaid,
                liters = domain.liters,
                isFullTank = domain.isFullTank,
                note = domain.note,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt
            )
        }
    }
}

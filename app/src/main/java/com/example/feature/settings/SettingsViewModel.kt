package com.example.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.model.FuelEntry
import com.example.core.model.FuelType
import com.example.core.model.Vehicle
import com.example.core.model.VehicleType
import com.example.core.repository.FuelRepository
import com.example.core.repository.FuelTypeRepository
import com.example.core.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val fuelTypeRepository: FuelTypeRepository,
    private val fuelRepository: FuelRepository
) : ViewModel() {

    private val _demoDataLoaded = MutableStateFlow(false)
    val demoDataLoaded: StateFlow<Boolean> = _demoDataLoaded.asStateFlow()

    fun loadSampleData() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val oneDay = 86_400_000L

            // Check if demo vehicle already exists
            val existingVehicles = vehicleRepository.getActiveCount()
            if (existingVehicles > 0) {
                _demoDataLoaded.value = true
                return@launch
            }

            // Seed sample fuel types
            suspend fun getOrCreateFuelType(name: String, description: String): Long {
                return fuelTypeRepository.getByName(name)?.id
                    ?: fuelTypeRepository.insert(
                        FuelType(name = name, description = description, createdAt = now, updatedAt = now)
                    )
            }

            val ron95Id = getOrCreateFuelType("RON 95-III", "Xăng cao cấp")
            getOrCreateFuelType("E5 RON 92-II", "Xăng sinh học")
            getOrCreateFuelType("DO 0,05S-II (Diesel)", "Dầu Diesel")

            // Vehicle 1: Honda Vision (Motorcycle)
            // Initial: 10,000.0 km = 10,000,000 meters
            val visionId = vehicleRepository.insert(
                Vehicle(
                    name = "Honda Vision",
                    type = VehicleType.MOTORCYCLE,
                    brand = "Honda",
                    model = "Vision 110",
                    year = 2023,
                    licensePlate = "43-C1 889.99",
                    initialOdometerMeters = 10_000_000L,
                    currentOdometerMeters = 10_580_400L,
                    fuelTankCapacityLiters = 4.8,
                    createdAt = now - (30 * oneDay)
                )
            )

            // Fuel entries for Vision:
            // 1. Initial full tank at 10,000 km
            fuelRepository.insert(
                FuelEntry(
                    vehicleId = visionId,
                    timestamp = now - (20 * oneDay),
                    odometerMeters = 10_000_000L,
                    fuelTypeId = ron95Id,
                    pricePerLiter = 25000L,
                    amountPaid = 100_000L,
                    liters = 4.0,
                    isFullTank = true,
                    note = "Đầy bình lần đầu",
                    createdAt = now - (20 * oneDay)
                )
            )

            // 2. Full tank at 10,250.0 km (dist: 250km, liters: 5.3 -> 2.12 L/100km)
            fuelRepository.insert(
                FuelEntry(
                    vehicleId = visionId,
                    timestamp = now - (10 * oneDay),
                    odometerMeters = 10_250_000L,
                    fuelTypeId = ron95Id,
                    pricePerLiter = 25200L,
                    amountPaid = 133_560L,
                    liters = 5.3,
                    isFullTank = true,
                    note = "Đổ cây xăng Petrolimex số 12",
                    createdAt = now - (10 * oneDay)
                )
            )

            // 3. Full tank at 10,580.4 km (dist: 330.4km, liters: 7.1)
            fuelRepository.insert(
                FuelEntry(
                    vehicleId = visionId,
                    timestamp = now - (2 * oneDay),
                    odometerMeters = 10_580_400L,
                    fuelTypeId = ron95Id,
                    pricePerLiter = 24800L,
                    amountPaid = 176_080L,
                    liters = 7.1,
                    isFullTank = true,
                    note = "Chuẩn bị đi tour cuối tuần",
                    createdAt = now - (2 * oneDay)
                )
            )

            // Vehicle 2: Toyota Vios (Car)
            // Initial: 25,000.0 km = 25,000,000 meters
            val viosId = vehicleRepository.insert(
                Vehicle(
                    name = "Toyota Vios G",
                    type = VehicleType.CAR,
                    brand = "Toyota",
                    model = "Vios 1.5G CVT",
                    year = 2022,
                    licensePlate = "43A-688.23",
                    initialOdometerMeters = 25_000_000L,
                    currentOdometerMeters = 25_650_000L,
                    fuelTankCapacityLiters = 42.0,
                    createdAt = now - (40 * oneDay)
                )
            )

            // Fuel entries for Vios:
            fuelRepository.insert(
                FuelEntry(
                    vehicleId = viosId,
                    timestamp = now - (15 * oneDay),
                    odometerMeters = 25_000_000L,
                    fuelTypeId = ron95Id,
                    pricePerLiter = 25000L,
                    amountPaid = 800_000L,
                    liters = 32.0,
                    isFullTank = true,
                    note = "Đầy bình trước chuyến đi Hội An",
                    createdAt = now - (15 * oneDay)
                )
            )

            fuelRepository.insert(
                FuelEntry(
                    vehicleId = viosId,
                    timestamp = now - (5 * oneDay),
                    odometerMeters = 25_650_000L,
                    fuelTypeId = ron95Id,
                    pricePerLiter = 25300L,
                    amountPaid = 986_700L,
                    liters = 39.0,
                    isFullTank = true,
                    note = "650 km tiêu hao 6.0 L/100km",
                    createdAt = now - (5 * oneDay)
                )
            )

            _demoDataLoaded.value = true
        }
    }
}

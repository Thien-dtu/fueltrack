package com.example.core.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Vehicles : Screen("vehicles")
    data object Maintenance : Screen("maintenance")
    data object Statistics : Screen("statistics")
    data object Settings : Screen("settings")

    data object AddVehicle : Screen("vehicle/add")
    data object EditVehicle : Screen("vehicle/edit/{vehicleId}") {
        fun createRoute(vehicleId: Long) = "vehicle/edit/$vehicleId"
    }

    data object AddFuel : Screen("fuel/add?vehicleId={vehicleId}") {
        fun createRoute(vehicleId: Long? = null) = if (vehicleId != null) "fuel/add?vehicleId=$vehicleId" else "fuel/add"
    }

    data object EditFuel : Screen("fuel/edit/{entryId}") {
        fun createRoute(entryId: Long) = "fuel/edit/$entryId"
    }

    data object FuelHistory : Screen("fuel/history/{vehicleId}") {
        fun createRoute(vehicleId: Long) = "fuel/history/$vehicleId"
    }

    data object FuelTypes : Screen("settings/fuel_types")

    // Maintenance Screens
    data object AddMaintenanceSchedule : Screen("maintenance/schedule/add?vehicleId={vehicleId}") {
        fun createRoute(vehicleId: Long? = null) = if (vehicleId != null) "maintenance/schedule/add?vehicleId=$vehicleId" else "maintenance/schedule/add"
    }

    data object EditMaintenanceSchedule : Screen("maintenance/schedule/edit/{scheduleId}") {
        fun createRoute(scheduleId: Long) = "maintenance/schedule/edit/$scheduleId"
    }

    data object AddMaintenanceEntry : Screen("maintenance/entry/add?vehicleId={vehicleId}&scheduleId={scheduleId}") {
        fun createRoute(vehicleId: Long? = null, scheduleId: Long? = null): String {
            val params = mutableListOf<String>()
            if (vehicleId != null) params.add("vehicleId=$vehicleId")
            if (scheduleId != null) params.add("scheduleId=$scheduleId")
            return if (params.isNotEmpty()) "maintenance/entry/add?${params.joinToString("&")}" else "maintenance/entry/add"
        }
    }

    data object EditMaintenanceEntry : Screen("maintenance/entry/edit/{entryId}") {
        fun createRoute(entryId: Long) = "maintenance/entry/edit/$entryId"
    }

    data object MaintenanceHistory : Screen("maintenance/history/{vehicleId}") {
        fun createRoute(vehicleId: Long) = "maintenance/history/$vehicleId"
    }

    data object MaintenanceTemplates : Screen("maintenance/templates")
}

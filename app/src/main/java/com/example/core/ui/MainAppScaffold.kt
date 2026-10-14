package com.example.core.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.navigation.Screen
import com.example.core.ui.theme.AmberPrimary
import com.example.core.ui.theme.DarkBackground
import com.example.core.ui.theme.DarkSurface
import com.example.feature.dashboard.DashboardScreen
import com.example.feature.dashboard.DashboardViewModel
import com.example.feature.fuel.AddEditFuelScreen
import com.example.feature.fuel.AddEditFuelViewModel
import com.example.feature.fuel.FuelHistoryScreen
import com.example.feature.fuel.FuelHistoryViewModel
import com.example.feature.fueltype.FuelTypeManagementScreen
import com.example.feature.fueltype.FuelTypeViewModel
import com.example.feature.maintenance.AddEditMaintenanceEntryScreen
import com.example.feature.maintenance.AddEditMaintenanceEntryViewModel
import com.example.feature.maintenance.AddEditScheduleScreen
import com.example.feature.maintenance.AddEditScheduleViewModel
import com.example.feature.maintenance.MaintenanceDashboardScreen
import com.example.feature.maintenance.MaintenanceDashboardViewModel
import com.example.feature.maintenance.MaintenanceHistoryScreen
import com.example.feature.maintenance.MaintenanceHistoryViewModel
import com.example.feature.maintenance.TemplateManagementScreen
import com.example.feature.maintenance.TemplateManagementViewModel
import com.example.feature.settings.SettingsScreen
import com.example.feature.settings.SettingsViewModel
import com.example.feature.statistics.StatisticsScreen
import com.example.feature.statistics.StatisticsViewModel
import com.example.feature.vehicle.AddEditVehicleScreen
import com.example.feature.vehicle.AddEditVehicleViewModel
import com.example.feature.vehicle.VehicleListScreen
import com.example.feature.vehicle.VehicleListViewModel

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun MainAppScaffold(
    initialRoute: String? = null,
    initialVehicleId: Long? = null,
    initialScheduleId: Long? = null,
    navController: NavHostController = rememberNavController()
) {
    androidx.compose.runtime.LaunchedEffect(initialRoute) {
        if (!initialRoute.isNullOrBlank() && initialRoute != Screen.Dashboard.route) {
            navController.navigate(initialRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(
            route = Screen.Dashboard.route,
            label = "Trang chủ",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            testTag = "nav_home"
        ),
        BottomNavItem(
            route = Screen.Vehicles.route,
            label = "Garage",
            selectedIcon = Icons.Filled.DirectionsCar,
            unselectedIcon = Icons.Outlined.DirectionsCar,
            testTag = "nav_garage"
        ),
        BottomNavItem(
            route = Screen.Maintenance.route,
            label = "Bảo dưỡng",
            selectedIcon = Icons.Filled.Build,
            unselectedIcon = Icons.Outlined.Build,
            testTag = "nav_maintenance"
        ),
        BottomNavItem(
            route = Screen.Statistics.route,
            label = "Thống kê",
            selectedIcon = Icons.Filled.BarChart,
            unselectedIcon = Icons.Outlined.BarChart,
            testTag = "nav_statistics"
        ),
        BottomNavItem(
            route = Screen.Settings.route,
            label = "Cài đặt",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            testTag = "nav_settings"
        )
    )

    // Show bottom bar & FAB only on main 5 tabs
    val isMainTab = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        containerColor = DarkBackground,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        bottomBar = {
            if (isMainTab) {
                NavigationBar(
                    containerColor = DarkSurface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DarkBackground,
                                indicatorColor = AmberPrimary,
                                selectedTextColor = AmberPrimary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (isMainTab) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(Screen.AddFuel.createRoute(null))
                    },
                    containerColor = AmberPrimary,
                    contentColor = DarkBackground,
                    modifier = Modifier.testTag("main_add_fuel_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalGasStation,
                        contentDescription = "Thêm lần đổ xăng"
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Dashboard
            composable(Screen.Dashboard.route) {
                val viewModel: DashboardViewModel = hiltViewModel()
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAddFuel = { vehicleId ->
                        navController.navigate(Screen.AddFuel.createRoute(vehicleId))
                    },
                    onNavigateToAddVehicle = {
                        navController.navigate(Screen.AddVehicle.route)
                    },
                    onNavigateToHistory = { vehicleId ->
                        navController.navigate(Screen.FuelHistory.createRoute(vehicleId))
                    },
                    onNavigateToEditFuel = { entryId ->
                        navController.navigate(Screen.EditFuel.createRoute(entryId))
                    }
                )
            }

            // Vehicles / Garage
            composable(Screen.Vehicles.route) {
                val viewModel: VehicleListViewModel = hiltViewModel()
                VehicleListScreen(
                    viewModel = viewModel,
                    onNavigateToAddVehicle = {
                        navController.navigate(Screen.AddVehicle.route)
                    },
                    onNavigateToEditVehicle = { vehicleId ->
                        navController.navigate(Screen.EditVehicle.createRoute(vehicleId))
                    },
                    onNavigateToHistory = { vehicleId ->
                        navController.navigate(Screen.FuelHistory.createRoute(vehicleId))
                    }
                )
            }

            // Add Vehicle
            composable(Screen.AddVehicle.route) {
                val viewModel: AddEditVehicleViewModel = hiltViewModel()
                AddEditVehicleScreen(
                    viewModel = viewModel,
                    isEditMode = false,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Edit Vehicle
            composable(
                route = Screen.EditVehicle.route,
                arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
            ) {
                val viewModel: AddEditVehicleViewModel = hiltViewModel()
                AddEditVehicleScreen(
                    viewModel = viewModel,
                    isEditMode = true,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Add Fuel
            composable(
                route = Screen.AddFuel.route,
                arguments = listOf(
                    navArgument("vehicleId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                val viewModel: AddEditFuelViewModel = hiltViewModel()
                AddEditFuelScreen(
                    viewModel = viewModel,
                    isEditMode = false,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Edit Fuel
            composable(
                route = Screen.EditFuel.route,
                arguments = listOf(navArgument("entryId") { type = NavType.LongType })
            ) {
                val viewModel: AddEditFuelViewModel = hiltViewModel()
                AddEditFuelScreen(
                    viewModel = viewModel,
                    isEditMode = true,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Fuel History
            composable(
                route = Screen.FuelHistory.route,
                arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
            ) {
                val viewModel: FuelHistoryViewModel = hiltViewModel()
                FuelHistoryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditFuel = { entryId ->
                        navController.navigate(Screen.EditFuel.createRoute(entryId))
                    }
                )
            }

            // Statistics
            composable(Screen.Statistics.route) {
                val viewModel: StatisticsViewModel = hiltViewModel()
                StatisticsScreen(viewModel = viewModel)
            }

            // Settings
            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToFuelTypes = {
                        navController.navigate(Screen.FuelTypes.route)
                    },
                    onNavigateToMaintenanceTemplates = {
                        navController.navigate(Screen.MaintenanceTemplates.route)
                    }
                )
            }

            // Fuel Types Management
            composable(Screen.FuelTypes.route) {
                val viewModel: FuelTypeViewModel = hiltViewModel()
                FuelTypeManagementScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Maintenance Dashboard
            composable(Screen.Maintenance.route) {
                val viewModel: MaintenanceDashboardViewModel = hiltViewModel()
                androidx.compose.runtime.LaunchedEffect(initialVehicleId, initialScheduleId) {
                    if (initialVehicleId != null) {
                        viewModel.selectVehicle(initialVehicleId)
                    }
                    if (initialScheduleId != null) {
                        viewModel.focusSchedule(initialScheduleId)
                    }
                }
                MaintenanceDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAddSchedule = { vehicleId ->
                        navController.navigate(Screen.AddMaintenanceSchedule.createRoute(vehicleId))
                    },
                    onNavigateToAddEntry = { vehicleId ->
                        navController.navigate(Screen.AddMaintenanceEntry.createRoute(vehicleId))
                    },
                    onNavigateToCompleteSchedule = { scheduleId ->
                        navController.navigate(Screen.AddMaintenanceEntry.createRoute(scheduleId = scheduleId))
                    },
                    onNavigateToEditSchedule = { scheduleId ->
                        navController.navigate(Screen.EditMaintenanceSchedule.createRoute(scheduleId))
                    },
                    onNavigateToEditEntry = { entryId ->
                        navController.navigate(Screen.EditMaintenanceEntry.createRoute(entryId))
                    },
                    onNavigateToHistory = { vehicleId ->
                        navController.navigate(Screen.MaintenanceHistory.createRoute(vehicleId))
                    },
                    onNavigateToTemplates = {
                        navController.navigate(Screen.MaintenanceTemplates.route)
                    },
                    onNavigateToAddVehicle = {
                        navController.navigate(Screen.AddVehicle.route)
                    }
                )
            }

            // Add Maintenance Schedule
            composable(
                route = Screen.AddMaintenanceSchedule.route,
                arguments = listOf(
                    navArgument("vehicleId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                val viewModel: AddEditScheduleViewModel = hiltViewModel()
                AddEditScheduleScreen(
                    viewModel = viewModel,
                    isEditMode = false,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Edit Maintenance Schedule
            composable(
                route = Screen.EditMaintenanceSchedule.route,
                arguments = listOf(navArgument("scheduleId") { type = NavType.LongType })
            ) {
                val viewModel: AddEditScheduleViewModel = hiltViewModel()
                AddEditScheduleScreen(
                    viewModel = viewModel,
                    isEditMode = true,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Add Maintenance Entry (or Complete Schedule)
            composable(
                route = Screen.AddMaintenanceEntry.route,
                arguments = listOf(
                    navArgument("vehicleId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("scheduleId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val scheduleId = backStackEntry.arguments?.getString("scheduleId")?.toLongOrNull()
                val viewModel: AddEditMaintenanceEntryViewModel = hiltViewModel()
                AddEditMaintenanceEntryScreen(
                    viewModel = viewModel,
                    isEditMode = false,
                    isCompleteScheduleMode = scheduleId != null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Edit Maintenance Entry
            composable(
                route = Screen.EditMaintenanceEntry.route,
                arguments = listOf(navArgument("entryId") { type = NavType.LongType })
            ) {
                val viewModel: AddEditMaintenanceEntryViewModel = hiltViewModel()
                AddEditMaintenanceEntryScreen(
                    viewModel = viewModel,
                    isEditMode = true,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Maintenance History
            composable(
                route = Screen.MaintenanceHistory.route,
                arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
            ) {
                val viewModel: MaintenanceHistoryViewModel = hiltViewModel()
                MaintenanceHistoryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddEntry = {
                        navController.navigate(Screen.AddMaintenanceEntry.createRoute(viewModel.vehicleId))
                    },
                    onNavigateToEditEntry = { entryId ->
                        navController.navigate(Screen.EditMaintenanceEntry.createRoute(entryId))
                    }
                )
            }

            // Maintenance Templates
            composable(Screen.MaintenanceTemplates.route) {
                val viewModel: TemplateManagementViewModel = hiltViewModel()
                TemplateManagementScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

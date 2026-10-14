package com.example.feature.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.VehicleType
import com.example.core.ui.theme.AmberPrimary
import com.example.core.ui.theme.DarkBackground
import com.example.core.ui.theme.DarkSurface
import com.example.core.ui.theme.DarkSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditVehicleScreen(
    viewModel: AddEditVehicleViewModel,
    isEditMode: Boolean,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.formState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Chỉnh sửa xe" else "Thêm phương tiện mới",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vehicle Type Picker
            Text(
                text = "Loại phương tiện",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = state.type == VehicleType.MOTORCYCLE,
                    onClick = { viewModel.onTypeChanged(VehicleType.MOTORCYCLE) },
                    label = { Text("Xe máy") },
                    leadingIcon = {
                        Icon(Icons.Default.TwoWheeler, contentDescription = null)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberPrimary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f).testTag("vehicle_type_motorcycle")
                )

                FilterChip(
                    selected = state.type == VehicleType.CAR,
                    onClick = { viewModel.onTypeChanged(VehicleType.CAR) },
                    label = { Text("Ô tô") },
                    leadingIcon = {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberPrimary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f).testTag("vehicle_type_car")
                )
            }

            // Name Field
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Tên xe (ví dụ: Honda Vision, Toyota Vios)") },
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth().testTag("vehicle_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = AmberPrimary
                ),
                singleLine = true
            )

            // Brand & Model Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.brand,
                    onValueChange = viewModel::onBrandChanged,
                    label = { Text("Hãng (Honda, Toyota...)") },
                    modifier = Modifier.weight(1f).testTag("vehicle_brand_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = AmberPrimary
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = state.model,
                    onValueChange = viewModel::onModelChanged,
                    label = { Text("Model (Vision, Vios...)") },
                    modifier = Modifier.weight(1f).testTag("vehicle_model_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = AmberPrimary
                    ),
                    singleLine = true
                )
            }

            // Year & License Plate Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.yearString,
                    onValueChange = viewModel::onYearChanged,
                    label = { Text("Năm sx") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("vehicle_year_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = AmberPrimary
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = state.licensePlate,
                    onValueChange = viewModel::onLicensePlateChanged,
                    label = { Text("Biển số xe") },
                    modifier = Modifier.weight(1.5f).testTag("vehicle_plate_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = AmberPrimary
                    ),
                    singleLine = true
                )
            }

            // Odometer Input (Meters internally, km input with <=1 decimal place)
            OutlinedTextField(
                value = state.odoInput,
                onValueChange = viewModel::onOdoChanged,
                label = { Text("Chỉ số ODO hiện tại") },
                suffix = { Text("km", fontWeight = FontWeight.Bold, color = AmberPrimary) },
                supportingText = {
                    if (state.odoError != null) {
                        Text(state.odoError!!, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Nhập số nguyên hoặc tối đa 1 số thập phân (vd: 1222 hoặc 1222,4)")
                    }
                },
                isError = state.odoError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("vehicle_odo_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = AmberPrimary
                ),
                singleLine = true
            )

            // Fuel tank capacity (optional)
            OutlinedTextField(
                value = state.fuelTankCapacityString,
                onValueChange = viewModel::onTankCapacityChanged,
                label = { Text("Dung tích bình xăng (tùy chọn)") },
                suffix = { Text("lít") },
                isError = state.tankCapacityError != null,
                supportingText = state.tankCapacityError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("vehicle_tank_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = AmberPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button
            Button(
                onClick = viewModel::saveVehicle,
                enabled = !state.isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_vehicle_button")
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(
                        text = if (isEditMode) "CẬP NHẬT XE" else "LƯU XE MỚI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

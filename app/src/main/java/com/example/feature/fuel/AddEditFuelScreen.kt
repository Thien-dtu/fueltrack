package com.example.feature.fuel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.theme.AmberPrimary
import com.example.core.ui.theme.CyanAccent
import com.example.core.ui.theme.DarkBackground
import com.example.core.ui.theme.DarkSurface
import com.example.core.ui.theme.DarkSurfaceVariant
import com.example.core.ui.theme.SuccessGreen
import com.example.core.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFuelScreen(
    viewModel: AddEditFuelViewModel,
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

    var vehicleMenuExpanded by remember { mutableStateOf(false) }
    var fuelTypeMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Chỉnh sửa đổ xăng" else "Ghi nhận đổ xăng",
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
                actions = {
                    if (isEditMode) {
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.testTag("delete_fuel_entry_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Xóa lần đổ xăng",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
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
            // Vehicle Selector Dropdown
            ExposedDropdownMenuBox(
                expanded = vehicleMenuExpanded,
                onExpandedChange = { vehicleMenuExpanded = !vehicleMenuExpanded }
            ) {
                val selectedVehicleName = state.vehicles.find { it.id == state.selectedVehicleId }?.name ?: "Chọn xe"
                OutlinedTextField(
                    value = selectedVehicleName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Phương tiện") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .testTag("fuel_vehicle_selector"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface
                    )
                )

                ExposedDropdownMenu(
                    expanded = vehicleMenuExpanded,
                    onDismissRequest = { vehicleMenuExpanded = false }
                ) {
                    state.vehicles.forEach { vehicle ->
                        DropdownMenuItem(
                            text = { Text(vehicle.name) },
                            onClick = {
                                viewModel.onVehicleSelected(vehicle.id)
                                vehicleMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Odometer Input
            OutlinedTextField(
                value = state.odoInput,
                onValueChange = viewModel::onOdoChanged,
                label = { Text("Chỉ số ODO lúc đổ xăng") },
                suffix = { Text("km", fontWeight = FontWeight.Bold, color = AmberPrimary) },
                supportingText = {
                    if (state.odoError != null) {
                        Text(state.odoError!!, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Nhập số km (hỗ trợ tối đa 1 số thập phân, vd: 1222 hoặc 1222,4)")
                    }
                },
                isError = state.odoError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fuel_odo_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = AmberPrimary
                ),
                singleLine = true
            )

            // Warning if ODO decreased
            if (state.odoWarning != null) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.odoWarning!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Fuel Type Row + Quick Create Action
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Loại nhiên liệu",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = { viewModel.setShowQuickCreateFuelType(true) },
                        modifier = Modifier.testTag("quick_create_fuel_type_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Tạo loại mới", color = AmberPrimary, fontSize = 13.sp)
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = fuelTypeMenuExpanded,
                    onExpandedChange = { fuelTypeMenuExpanded = !fuelTypeMenuExpanded }
                ) {
                    val selectedFuelTypeName = state.fuelTypes.find { it.id == state.selectedFuelTypeId }?.name ?: "Chọn loại nhiên liệu"
                    OutlinedTextField(
                        value = selectedFuelTypeName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelTypeMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("fuel_type_selector"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = fuelTypeMenuExpanded,
                        onDismissRequest = { fuelTypeMenuExpanded = false }
                    ) {
                        state.fuelTypes.forEach { ft ->
                            DropdownMenuItem(
                                text = { Text(ft.name) },
                                onClick = {
                                    viewModel.onFuelTypeSelected(ft.id)
                                    fuelTypeMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Price / Liter & Amount Paid Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.pricePerLiterInput,
                    onValueChange = viewModel::onPriceChanged,
                    label = { Text("Giá / lít") },
                    suffix = { Text("đ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = state.priceError != null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("fuel_price_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = AmberPrimary
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = state.amountPaidInput,
                    onValueChange = viewModel::onAmountPaidChanged,
                    label = { Text("Số tiền đã đổ") },
                    suffix = { Text("đ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = state.amountError != null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("fuel_amount_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = AmberPrimary
                    ),
                    singleLine = true
                )
            }

            // Real-time Calculated Liters Display Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Số lít (tự động tính)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatLiters(state.calculatedLiters),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberPrimary,
                            modifier = Modifier.testTag("fuel_calculated_liters_text")
                        )
                    }

                    Text(
                        text = "= Số tiền / Giá mỗi lít",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Full Tank Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.isFullTank,
                    onCheckedChange = viewModel::onFullTankChanged,
                    colors = CheckboxDefaults.colors(checkedColor = AmberPrimary),
                    modifier = Modifier.testTag("full_tank_checkbox")
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Đổ đầy bình xăng",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Cần thiết để tính chính xác mức tiêu hao L/100km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Note Input
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChanged,
                label = { Text("Ghi chú (tùy chọn)") },
                modifier = Modifier.fillMaxWidth().testTag("fuel_note_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = AmberPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = viewModel::saveFuelEntry,
                enabled = !state.isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_fuel_entry_button")
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(
                        text = if (isEditMode) "CẬP NHẬT LẦN ĐỔ XĂNG" else "LƯU LẦN ĐỔ XĂNG",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Quick Create Fuel Type Dialog
        if (state.showQuickCreateFuelTypeDialog) {
            var newTypeName by remember { mutableStateOf("") }
            var newTypeDesc by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { viewModel.setShowQuickCreateFuelType(false) },
                title = { Text("Tạo loại nhiên liệu mới") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newTypeName,
                            onValueChange = { newTypeName = it },
                            label = { Text("Tên (ví dụ: RON 95-V, E10)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("quick_fuel_type_name_input")
                        )
                        OutlinedTextField(
                            value = newTypeDesc,
                            onValueChange = { newTypeDesc = it },
                            label = { Text("Mô tả (tùy chọn)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.quickCreateFuelType(newTypeName, newTypeDesc)
                        },
                        enabled = newTypeName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                        modifier = Modifier.testTag("quick_save_fuel_type_button")
                    ) {
                        Text("Tạo & Chọn")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setShowQuickCreateFuelType(false) }) {
                        Text("Hủy")
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Xác nhận xóa") },
                text = { Text("Bạn có chắc chắn muốn xóa bản ghi đổ xăng này không? Thống kê sẽ được tự động tính toán lại.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.deleteFuelEntry()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_delete_fuel_button")
                    ) {
                        Text("Xóa")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}

package com.example.feature.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.ui.theme.AmberPrimary
import com.example.core.ui.theme.DarkBackground
import com.example.core.ui.theme.DarkSurface
import com.example.core.ui.theme.DarkSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScheduleScreen(
    viewModel: AddEditScheduleViewModel,
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
    var templateMenuExpanded by remember { mutableStateOf(false) }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) {
                            stringResource(R.string.maint_schedule_title_edit)
                        } else {
                            stringResource(R.string.maint_schedule_title_new)
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.maint_schedule_back)
                        )
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(onClick = { showCancelConfirmDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.maint_schedule_cancel_action),
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
            // Vehicle Selector
            ExposedDropdownMenuBox(
                expanded = vehicleMenuExpanded,
                onExpandedChange = { vehicleMenuExpanded = !vehicleMenuExpanded }
            ) {
                val selectedVehicleName = state.vehicles.find { it.id == state.selectedVehicleId }?.name
                    ?: stringResource(R.string.maint_schedule_vehicle_placeholder)
                OutlinedTextField(
                    value = selectedVehicleName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.maint_schedule_vehicle_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleMenuExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
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

            // Quick Template Picker
            if (state.templates.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = templateMenuExpanded,
                    onExpandedChange = { templateMenuExpanded = !templateMenuExpanded }
                ) {
                    val templateName = state.templates.find { it.id == state.selectedTemplateId }?.name
                        ?: stringResource(R.string.maint_schedule_template_placeholder)
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.maint_schedule_template_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateMenuExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = templateMenuExpanded,
                        onDismissRequest = { templateMenuExpanded = false }
                    ) {
                        state.templates.forEach { tmpl ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            R.string.maint_schedule_template_item,
                                            tmpl.name,
                                            tmpl.category
                                        )
                                    )
                                },
                                onClick = {
                                    viewModel.onTemplateSelected(tmpl)
                                    templateMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Title Field
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChanged,
                label = { Text(stringResource(R.string.maint_schedule_title_label)) },
                isError = state.titleError != null,
                supportingText = state.titleError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth().testTag("schedule_title_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = AmberPrimary
                ),
                singleLine = true
            )

            // Category Field
            OutlinedTextField(
                value = state.category,
                onValueChange = viewModel::onCategoryChanged,
                label = { Text(stringResource(R.string.maint_schedule_category_label)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                singleLine = true
            )

            if (state.conditionError != null) {
                Text(
                    text = state.conditionError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Condition 1: By Date
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.hasDateCondition,
                            onCheckedChange = viewModel::onHasDateToggled,
                            colors = CheckboxDefaults.colors(checkedColor = AmberPrimary)
                        )
                        Text(
                            text = stringResource(R.string.maint_schedule_date_condition),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (state.hasDateCondition) {
                        OutlinedTextField(
                            value = state.dueDaysFromNow.toString(),
                            onValueChange = { days -> days.toIntOrNull()?.let { viewModel.onDueDaysChanged(it) } },
                            label = { Text(stringResource(R.string.maint_schedule_due_days_label)) },
                            suffix = { Text(stringResource(R.string.maint_schedule_due_days_suffix)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // Condition 2: By Odometer
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.hasOdoCondition,
                            onCheckedChange = viewModel::onHasOdoToggled,
                            colors = CheckboxDefaults.colors(checkedColor = AmberPrimary)
                        )
                        Text(
                            text = stringResource(R.string.maint_schedule_odo_condition),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (state.hasOdoCondition) {
                        OutlinedTextField(
                            value = state.dueOdoInput,
                            onValueChange = viewModel::onDueOdoChanged,
                            label = { Text(stringResource(R.string.maint_schedule_due_odo_label)) },
                            suffix = { Text(stringResource(R.string.maint_schedule_km_suffix)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // Condition 3: Recurring Interval
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.isRecurring,
                            onCheckedChange = viewModel::onIsRecurringToggled,
                            colors = CheckboxDefaults.colors(checkedColor = AmberPrimary)
                        )
                        Text(
                            text = stringResource(R.string.maint_schedule_recurring_condition),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (state.isRecurring) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = state.intervalDaysInput,
                                onValueChange = viewModel::onIntervalDaysChanged,
                                label = { Text(stringResource(R.string.maint_schedule_interval_days_label)) },
                                suffix = { Text(stringResource(R.string.maint_schedule_days_suffix)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.intervalKmInput,
                                onValueChange = viewModel::onIntervalKmChanged,
                                label = { Text(stringResource(R.string.maint_schedule_interval_km_label)) },
                                suffix = { Text(stringResource(R.string.maint_schedule_km_suffix)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = viewModel::saveSchedule,
                enabled = !state.isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_schedule_button")
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(
                        text = if (isEditMode) {
                            stringResource(R.string.maint_schedule_save_update)
                        } else {
                            stringResource(R.string.maint_schedule_save_new)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showCancelConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showCancelConfirmDialog = false },
                title = { Text(stringResource(R.string.maint_schedule_cancel_title)) },
                text = { Text(stringResource(R.string.maint_schedule_cancel_message)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showCancelConfirmDialog = false
                            viewModel.cancelSchedule()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.maint_schedule_cancel_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelConfirmDialog = false }) {
                        Text(stringResource(R.string.maint_schedule_close))
                    }
                }
            )
        }
    }
}

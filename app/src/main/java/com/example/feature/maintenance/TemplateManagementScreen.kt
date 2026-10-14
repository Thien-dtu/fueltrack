package com.example.feature.maintenance

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.MaintenanceTemplate
import com.example.core.ui.theme.AmberPrimary
import com.example.core.ui.theme.CyanAccent
import com.example.core.ui.theme.DarkBackground
import com.example.core.ui.theme.DarkSurface
import com.example.core.ui.theme.DarkSurfaceVariant
import com.example.core.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateManagementScreen(
    viewModel: TemplateManagementViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editingTemplate by remember { mutableStateOf<MaintenanceTemplate?>(null) }
    var isCreateDialogOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mẫu bảo dưỡng xe", fontWeight = FontWeight.Bold) },
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
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AmberPrimary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Danh sách mẫu (${uiState.builtInTemplates.size + uiState.userTemplates.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = { isCreateDialogOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("create_custom_template_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tạo mẫu mới", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Custom User Templates Section
            if (uiState.userTemplates.isNotEmpty()) {
                item {
                    Text(
                        text = "Mẫu tùy chỉnh của bạn",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AmberPrimary
                    )
                }

                items(uiState.userTemplates) { tmpl ->
                    TemplateItemCard(
                        template = tmpl,
                        onEdit = { editingTemplate = tmpl },
                        onArchive = { viewModel.archiveTemplate(tmpl.id) }
                    )
                }
            }

            // Built-in Templates Section
            item {
                Text(
                    text = "Mẫu chuẩn mặc định",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
            }

            items(uiState.builtInTemplates) { tmpl ->
                TemplateItemCard(
                    template = tmpl,
                    onEdit = null, // Built-in templates are read-only
                    onArchive = null
                )
            }

            // Archived Templates
            if (uiState.archivedTemplates.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FilterChip(
                            selected = uiState.showArchived,
                            onClick = { viewModel.toggleShowArchived() },
                            label = { Text("Đã lưu trữ (${uiState.archivedTemplates.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DarkSurfaceVariant,
                                selectedLabelColor = AmberPrimary
                            )
                        )
                    }
                }

                if (uiState.showArchived) {
                    items(uiState.archivedTemplates) { tmpl ->
                        TemplateItemCard(
                            template = tmpl,
                            onEdit = null,
                            onArchive = { viewModel.unarchiveTemplate(tmpl.id) },
                            isArchived = true
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Create Dialog
        if (isCreateDialogOpen) {
            TemplateEditDialog(
                title = "Tạo mẫu bảo dưỡng mới",
                initialName = "",
                initialCategory = "Bảo dưỡng",
                initialDays = "90",
                initialKm = "2000",
                initialDesc = "",
                onDismiss = { isCreateDialogOpen = false },
                onConfirm = { name, cat, days, km, desc ->
                    viewModel.saveTemplate(null, name, cat, days, km, desc)
                    isCreateDialogOpen = false
                }
            )
        }

        // Edit Dialog
        editingTemplate?.let { tmpl ->
            TemplateEditDialog(
                title = "Chỉnh sửa mẫu bảo dưỡng",
                initialName = tmpl.name,
                initialCategory = tmpl.category,
                initialDays = tmpl.defaultIntervalDays?.toString() ?: "",
                initialKm = tmpl.defaultIntervalMeters?.let { (it / 1000).toString() } ?: "",
                initialDesc = tmpl.description,
                onDismiss = { editingTemplate = null },
                onConfirm = { name, cat, days, km, desc ->
                    viewModel.saveTemplate(tmpl.id, name, cat, days, km, desc)
                    editingTemplate = null
                }
            )
        }
    }
}

@Composable
fun TemplateItemCard(
    template: MaintenanceTemplate,
    onEdit: (() -> Unit)?,
    onArchive: (() -> Unit)?,
    isArchived: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Build, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val intervals = buildString {
                    append(template.category)
                    val parts = mutableListOf<String>()
                    if (template.defaultIntervalDays != null) parts.add("${template.defaultIntervalDays} ngày")
                    if (template.defaultIntervalMeters != null) parts.add(Formatters.formatDistanceKm(template.defaultIntervalMeters))
                    if (parts.isNotEmpty()) append(" • Gợi ý: ${parts.joinToString(" / ")}")
                }
                Text(text = intervals, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (onArchive != null) {
                IconButton(onClick = onArchive) {
                    Icon(
                        imageVector = if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = "Lưu trữ",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TemplateEditDialog(
    title: String,
    initialName: String,
    initialCategory: String,
    initialDays: String,
    initialKm: String,
    initialDesc: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, days: Int?, km: Long?, desc: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var category by remember { mutableStateOf(initialCategory) }
    var daysText by remember { mutableStateOf(initialDays) }
    var kmText by remember { mutableStateOf(initialKm) }
    var desc by remember { mutableStateOf(initialDesc) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên mẫu *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Danh mục") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = daysText,
                        onValueChange = { daysText = it.filter { c -> c.isDigit() } },
                        label = { Text("Mỗi X ngày") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = kmText,
                        onValueChange = { kmText = it.filter { c -> c.isDigit() } },
                        label = { Text("Mỗi X km") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Mô tả") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name,
                        category,
                        daysText.toIntOrNull(),
                        kmText.toLongOrNull(),
                        desc
                    )
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

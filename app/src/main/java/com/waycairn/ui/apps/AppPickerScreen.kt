package com.waycairn.ui.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waycairn.data.model.AppSetting
import com.waycairn.data.model.TriggerMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    modifier: Modifier = Modifier,
    viewModel: AppPickerViewModel = viewModel(factory = AppPickerViewModel.Factory)
) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val gated by viewModel.gated.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    var configPackage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Gated apps",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    val setting = gated[app.packageName]
                    AppRow(
                        app = app,
                        setting = setting,
                        onToggle = { checked -> viewModel.setGated(app.packageName, checked) },
                        onOpenConfig = { if (setting != null) configPackage = app.packageName }
                    )
                }
            }
        }
    }

    val pkg = configPackage
    if (pkg != null) {
        val setting = gated[pkg] ?: AppSetting(packageName = pkg)
        val label = apps.firstOrNull { it.packageName == pkg }?.label ?: pkg
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { configPackage = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AppConfigSheet(
                label = label,
                setting = setting,
                onSave = { mode, delay, showAfter ->
                    viewModel.updateConfig(pkg, mode, delay, showAfter)
                    configPackage = null
                }
            )
        }
    }
}

@Composable
private fun AppRow(
    app: LaunchableApp,
    setting: AppSetting?,
    onToggle: (Boolean) -> Unit,
    onOpenConfig: () -> Unit
) {
    val isGated = setting != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = isGated, onClick = onOpenConfig)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (app.icon != null) {
                Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(32.dp))
            } else {
                Text(app.label.take(1).uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isGated) {
                val mode = if (setting!!.triggerMode == TriggerMode.ON_OPEN) {
                    "On open"
                } else {
                    "After ${setting.delayMinutes} min"
                }
                Text(
                    text = "$mode · tap to configure",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = isGated, onCheckedChange = onToggle)
    }
}

@Composable
private fun AppConfigSheet(
    label: String,
    setting: AppSetting,
    onSave: (TriggerMode, Int, Boolean) -> Unit
) {
    var mode by remember { mutableStateOf(setting.triggerMode) }
    var delay by remember { mutableStateOf(setting.delayMinutes.coerceAtLeast(1)) }
    var showAfter by remember { mutableStateOf(setting.showAfterComplete) }

    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(16.dp))

        Text("Trigger", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == TriggerMode.ON_OPEN,
                onClick = { mode = TriggerMode.ON_OPEN },
                label = { Text("On open") }
            )
            FilterChip(
                selected = mode == TriggerMode.AFTER_DELAY,
                onClick = { mode = TriggerMode.AFTER_DELAY },
                label = { Text("After delay") }
            )
        }

        if (mode == TriggerMode.AFTER_DELAY) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Delay: $delay min",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stepper(text = "−") { if (delay > 1) delay-- }
                    Stepper(text = "+") { if (delay < 120) delay++ }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Show even when all done",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Pure-friction: overlay still fires with nothing left.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = showAfter, onCheckedChange = { showAfter = it })
        }

        Spacer(Modifier.height(24.dp))
        androidx.compose.material3.Button(
            onClick = { onSave(mode, delay, showAfter) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Stepper(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

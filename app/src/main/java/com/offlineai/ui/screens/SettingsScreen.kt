package com.offlineai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.offlineai.data.model.UsbConnectionState
import com.offlineai.viewmodel.ChatViewModel
import kotlin.math.roundToInt

/**
 * SettingsScreen
 * --------------
 * Allows the user to configure:
 *  - Dark mode toggle.
 *  - Temperature slider (0.0 – 2.0).
 *  - Max tokens input (1 – 4096).
 *  - Model file selection from USB storage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel : ChatViewModel,
    onBack    : () -> Unit
) {
    val settings  by viewModel.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = com.offlineai.data.model.AppSettings()
    )
    val usbState  by viewModel.usbState.collectAsStateWithLifecycle()

    // Local transient state for the max-tokens text field
    var maxTokensText by remember(settings.maxTokens) {
        mutableStateOf(settings.maxTokens.toString())
    }

    // List of model files found on USB
    var modelFiles by remember { mutableStateOf<List<String>>(emptyList()) }
    var showModelPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier        = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding  = PaddingValues(vertical = 16.dp)
        ) {

            // ── Section: Appearance ──────────────────────────────────────
            item { SectionHeader("Appearance") }

            item {
                SettingsRow(
                    icon  = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Use dark colour scheme"
                ) {
                    Switch(
                        checked        = settings.darkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) }
                    )
                }
            }

            // ── Section: AI Parameters ───────────────────────────────────
            item { SectionHeader("AI Parameters") }

            item {
                SettingsCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Thermostat,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Temperature", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Controls randomness · ${
                                    (settings.temperature * 10).roundToInt() / 10f
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Slider(
                        value         = settings.temperature,
                        onValueChange = { viewModel.setTemperature(it) },
                        valueRange    = 0f..2f,
                        steps         = 19,
                        modifier      = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0.0 — Precise", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("2.0 — Creative", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }

            item {
                SettingsCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Token,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Max Tokens", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Maximum response length (1–4096)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value         = maxTokensText,
                        onValueChange = { text ->
                            maxTokensText = text
                            text.toIntOrNull()?.let { viewModel.setMaxTokens(it) }
                        },
                        label         = { Text("Tokens") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Section: USB & Model ─────────────────────────────────────
            item { SectionHeader("USB & Model") }

            item {
                SettingsCard {
                    // Connection status row
                    Row(
                        verticalAlignment   = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Coloured status dot
                        val dotColor = when (usbState) {
                            is UsbConnectionState.Connected    -> androidx.compose.ui.graphics.Color(0xFF22C55E)
                            is UsbConnectionState.Connecting   -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
                            is UsbConnectionState.Error        -> androidx.compose.ui.graphics.Color(0xFFEF4444)
                            UsbConnectionState.Disconnected    -> MaterialTheme.colorScheme.outline
                        }
                        Surface(
                            modifier = Modifier.size(10.dp),
                            shape    = RoundedCornerShape(50),
                            color    = dotColor
                        ) {}

                        Column(Modifier.weight(1f)) {
                            Text(
                                text  = when (usbState) {
                                    is UsbConnectionState.Connected  -> "USB Connected"
                                    is UsbConnectionState.Connecting -> "Connecting…"
                                    is UsbConnectionState.Error      -> "Error"
                                    UsbConnectionState.Disconnected  -> "USB Disconnected"
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (usbState is UsbConnectionState.Connected) {
                                Text(
                                    text  = (usbState as UsbConnectionState.Connected).deviceName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (usbState is UsbConnectionState.Connected)
                                    viewModel.disconnectUsb()
                                else
                                    viewModel.connectUsb()
                            }
                        ) {
                            Text(
                                if (usbState is UsbConnectionState.Connected)
                                    "Disconnect" else "Connect"
                            )
                        }
                    }

                    // Model path display
                    if (settings.modelPath.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text     = settings.modelPath,
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 2
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Scan for model files button
                    OutlinedButton(
                        onClick  = {
                            modelFiles = viewModel.scanUsbForModels()
                            showModelPicker = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Scan USB for Model Files")
                    }
                }
            }

            // ── Model file picker list ───────────────────────────────────
            item {
                AnimatedVisibility(visible = showModelPicker && modelFiles.isNotEmpty()) {
                    SettingsCard {
                        Text("Select Model File", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        modelFiles.forEach { path ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setModelPath(path)
                                        showModelPicker = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Memory, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Column {
                                    Text(path.substringAfterLast('/'), style = MaterialTheme.typography.bodyMedium)
                                    Text(path, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1)
                                }
                            }
                            if (path != modelFiles.last()) HorizontalDivider()
                        }
                    }
                }

                AnimatedVisibility(visible = showModelPicker && modelFiles.isEmpty()) {
                    Surface(
                        color  = MaterialTheme.colorScheme.errorContainer,
                        shape  = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            "No model files found on USB. Make sure your drive is connected and contains a .gguf / .bin / .onnx file.",
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Reusable layout helpers
// ──────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text     = title.uppercase(),
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsRow(
    icon     : androidx.compose.ui.graphics.vector.ImageVector,
    title    : String,
    subtitle : String,
    trailing : @Composable () -> Unit
) {
    SettingsCard {
        Row(
            modifier            = Modifier.fillMaxWidth(),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            trailing()
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier       = Modifier.fillMaxWidth(),
        shape          = RoundedCornerShape(12.dp),
        color          = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content  = content
        )
    }
}

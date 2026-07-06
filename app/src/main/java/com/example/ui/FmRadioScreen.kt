package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FmStation
import com.example.hardware.AudioOutputMode
import com.example.hardware.HardwareScanResult
import com.example.ui.components.DiagnosticsModal
import com.example.ui.components.DirectFrequencyDialog
import com.example.ui.components.RenamePresetDialog
import com.example.ui.components.TunerDial
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.TunerCardSurface
import com.example.ui.theme.TunerDarkBackground

@Composable
fun FmRadioScreen(
    uiState: FmRadioUiState,
    favoriteStations: List<FmStation>,
    allStations: List<FmStation>,
    onTuneFrequency: (Float) -> Unit,
    onTogglePower: () -> Unit,
    onToggleMute: () -> Unit,
    onSetAudioMode: (AudioOutputMode) -> Unit,
    onStepDown: () -> Unit,
    onStepUp: () -> Unit,
    onSeekDown: () -> Unit,
    onSeekUp: () -> Unit,
    onStartScan: () -> Unit,
    onCancelScan: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDeleteStation: (FmStation) -> Unit,
    onUpdateStationName: (Float, String) -> Unit,
    onShowDiagnostics: (Boolean) -> Unit,
    onShowDirectEntry: (Boolean) -> Unit
) {
    var stationToRename by remember { mutableStateOf<FmStation?>(null) }
    var selectedTabIndex by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = TunerDarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header Bar
            item {
                HeaderSection(
                    isPowerOn = uiState.isPowerOn,
                    onTogglePower = onTogglePower,
                    onOpenDiagnostics = { onShowDiagnostics(true) }
                )
            }

            // Digital VFD Tuner Display
            item {
                VfdDigitalDisplay(
                    uiState = uiState,
                    onToggleFavorite = onToggleFavorite,
                    onToggleMute = onToggleMute,
                    onOpenDirectEntry = { onShowDirectEntry(true) }
                )
            }

            // Scanning progress indicator
            if (uiState.isScanning) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "HARDWARE BAND SCANNING (/dev/radio0)...",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberPrimary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${(uiState.scanProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { uiState.scanProgress },
                            color = AmberPrimary,
                            trackColor = Color(0xFF2A2E38),
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }

            // Audio Routing Switcher (Headphones vs Speaker)
            item {
                AudioRoutingCard(
                    isPowerOn = uiState.isPowerOn,
                    currentMode = uiState.audioOutputMode,
                    isHeadsetConnected = uiState.isHeadsetConnected,
                    onSelectMode = onSetAudioMode
                )
            }

            // Analog Frequency Ruler Dial
            item {
                TunerDial(
                    currentFrequency = uiState.currentFrequency,
                    isPowerOn = uiState.isPowerOn,
                    onFrequencyChange = onTuneFrequency,
                    onStepDown = onStepDown,
                    onStepUp = onStepUp,
                    onSeekDown = onSeekDown,
                    onSeekUp = onSeekUp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Quick Actions Bar (Scan All, Direct Entry, Rename)
            item {
                QuickActionsBar(
                    isPowerOn = uiState.isPowerOn,
                    isScanning = uiState.isScanning,
                    onStartScan = onStartScan,
                    onCancelScan = onCancelScan,
                    onDirectEntry = { onShowDirectEntry(true) },
                    onRenameCurrent = {
                        val current = allStations.find { Math.abs(it.frequencyMHz - uiState.currentFrequency) < 0.05f }
                            ?: FmStation(uiState.currentFrequency, uiState.currentStationName)
                        stationToRename = current
                    }
                )
            }

            // Station Presets Tabs
            item {
                Spacer(modifier = Modifier.height(12.dp))
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color(0xFF131519),
                    contentColor = AmberPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = AmberPrimary
                        )
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("FAVORITES (${favoriteStations.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("DISCOVERED (${allStations.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text("SCAN AUDIT (${uiState.scanDiagnostics.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Station List / Audit List
            if (selectedTabIndex == 2) {
                if (uiState.scanDiagnostics.isEmpty()) {
                    item {
                        EmptyScanAuditBox(onScan = onStartScan)
                    }
                } else {
                    items(uiState.scanDiagnostics, key = { it.frequencyMHz }) { audit ->
                        ScanDiagnosticItemCard(audit = audit, onTune = { onTuneFrequency(audit.frequencyMHz) })
                    }
                }
            } else {
                val displayList = if (selectedTabIndex == 0) favoriteStations else allStations
                if (displayList.isEmpty()) {
                    item {
                        EmptyPresetsBox(isFavoritesTab = selectedTabIndex == 0, onScan = onStartScan)
                    }
                } else {
                    items(displayList, key = { it.frequencyMHz }) { station ->
                        StationItemCard(
                            station = station,
                            isActive = Math.abs(station.frequencyMHz - uiState.currentFrequency) < 0.05f && uiState.isPowerOn,
                            onTune = { onTuneFrequency(station.frequencyMHz) },
                            onEditName = { stationToRename = station },
                            onDelete = { onDeleteStation(station) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (uiState.showDirectEntryDialog) {
        DirectFrequencyDialog(
            initialFrequency = uiState.currentFrequency,
            onDismiss = { onShowDirectEntry(false) },
            onConfirm = { freq -> onTuneFrequency(freq) }
        )
    }

    if (stationToRename != null) {
        RenamePresetDialog(
            currentName = stationToRename!!.name,
            frequency = stationToRename!!.frequencyMHz,
            onDismiss = { stationToRename = null },
            onConfirm = { newName ->
                onUpdateStationName(stationToRename!!.frequencyMHz, newName)
                stationToRename = null
            }
        )
    }

    if (uiState.showDiagnosticsModal) {
        DiagnosticsModal(
            driverPath = uiState.driverPath,
            driverStatus = uiState.driverStatusText,
            chipsetInfo = uiState.chipsetInfo,
            isPowerOn = uiState.isPowerOn,
            currentRssi = uiState.currentRssi,
            isStereo = uiState.isStereo,
            audioOutputMode = uiState.audioOutputMode,
            isHeadsetConnected = uiState.isHeadsetConnected,
            audioPipelineStatus = uiState.audioPipelineStatus,
            audioRoutingLogs = uiState.audioRoutingLogs,
            scanDiagnostics = uiState.scanDiagnostics,
            onDismiss = { onShowDiagnostics(false) }
        )
    }
}

@Composable
private fun HeaderSection(
    isPowerOn: Boolean,
    onTogglePower: () -> Unit,
    onOpenDiagnostics: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = AmberPrimary,
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Radio,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.padding(7.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "QUALCOMM FM TUNER",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Snapdragon 695 • Tab A9+ 5G",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onOpenDiagnostics,
                modifier = Modifier
                    .background(Color(0xFF23262C), CircleShape)
                    .border(1.dp, Color(0xFF323843), CircleShape)
                    .testTag("diagnostics_button")
            ) {
                Icon(Icons.Default.DeveloperBoard, contentDescription = "Hardware Diagnostics", tint = AmberPrimary)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                color = if (isPowerOn) SignalGreen else Color(0xFF3D161A),
                shape = CircleShape,
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onTogglePower)
                    .testTag("power_button")
            ) {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = "Power Switch",
                    tint = if (isPowerOn) Color.Black else Color(0xFFFF1744),
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun VfdDigitalDisplay(
    uiState: FmRadioUiState,
    onToggleFavorite: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenDirectEntry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0E11)),
        border = androidx.compose.foundation.BorderStroke(2.dp, if (uiState.isPowerOn) AmberPrimary.copy(alpha = 0.7f) else Color(0xFF2C313B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Top status line inside VFD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (uiState.isPowerOn && uiState.isStereo) Color(0xFF2979FF) else Color(0xFF2C313B),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (uiState.isStereo) " STEREO " else " MONO ",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.isPowerOn && uiState.isStereo) Color.White else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isPowerOn) "V4L2 LOCK /dev/radio0" else "STANDBY",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.isPowerOn) SignalGreen else Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        enabled = uiState.isPowerOn,
                        modifier = Modifier.size(32.dp).testTag("favorite_button")
                    ) {
                        Icon(
                            if (uiState.isCurrentFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (uiState.isCurrentFavorite) Color(0xFFFF1744) else Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onToggleMute,
                        enabled = uiState.isPowerOn,
                        modifier = Modifier.size(32.dp).testTag("mute_button")
                    ) {
                        Icon(
                            if (uiState.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Mute",
                            tint = if (uiState.isMuted) Color(0xFFFF1744) else AmberPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Big Numerals
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = uiState.isPowerOn, onClick = onOpenDirectEntry),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (uiState.isPowerOn) String.format("%.1f", uiState.currentFrequency) else "--.-",
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Black,
                    color = if (uiState.isPowerOn) (if (uiState.isMuted) Color.Gray else AmberPrimary) else Color(0xFF2C313B),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MHz",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.isPowerOn) AmberSecondary else Color(0xFF2C313B),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Signal Strength RSSI Meter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.isPowerOn) uiState.currentStationName else "Tuner Off",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (uiState.isPowerOn) "${uiState.currentRssi} dBm" else "0 dBm",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    SignalBars(rssi = if (uiState.isPowerOn) uiState.currentRssi else 0)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrolling RDS broadcast marquee
            Surface(
                color = Color(0xFF161A20),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RDS:",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberPrimary,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isPowerOn) uiState.currentRdsText else "Power on hardware driver to receive RDS broadcast",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD0D3D9),
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SignalBars(rssi: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(16.dp)
    ) {
        val thresholds = listOf(20, 35, 48, 58, 65)
        for ((index, threshold) in thresholds.withIndex()) {
            val active = rssi >= threshold
            val barHeight = ((index + 1) * 3).dp.coerceAtLeast(4.dp)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .background(
                        color = if (active) (if (index >= 3) SignalGreen else AmberSecondary) else Color(0xFF2C313B),
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

@Composable
private fun AudioRoutingCard(
    isPowerOn: Boolean,
    currentMode: AudioOutputMode,
    isHeadsetConnected: Boolean,
    onSelectMode: (AudioOutputMode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TunerCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF323843))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AUDIO OUTPUT ROUTING",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isHeadsetConnected) "✓ Wired Antenna OK" else "⚠️ Headset Cord Needed",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isHeadsetConnected) SignalGreen else Color.Yellow
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Headset Button
                val headsetSelected = currentMode == AudioOutputMode.WIRED_HEADSET
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (headsetSelected) AmberPrimary else Color(0xFF131519),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = isPowerOn) { onSelectMode(AudioOutputMode.WIRED_HEADSET) }
                        .border(1.dp, if (headsetSelected) AmberPrimary else Color(0xFF323843), RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Headset,
                            contentDescription = null,
                            tint = if (headsetSelected) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Headphones",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (headsetSelected) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Tablet Speakers Button
                val speakerSelected = currentMode == AudioOutputMode.TABLET_SPEAKERS
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (speakerSelected) AmberPrimary else Color(0xFF131519),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = isPowerOn) { onSelectMode(AudioOutputMode.TABLET_SPEAKERS) }
                        .border(1.dp, if (speakerSelected) AmberPrimary else Color(0xFF323843), RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Speaker,
                            contentDescription = null,
                            tint = if (speakerSelected) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tab Speakers",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (speakerSelected) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsBar(
    isPowerOn: Boolean,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onCancelScan: () -> Unit,
    onDirectEntry: () -> Unit,
    onRenameCurrent: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Auto-Scan Button
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isScanning) Color(0xFFFF1744) else Color(0xFF2E333D),
            modifier = Modifier
                .weight(1.2f)
                .clickable(enabled = isPowerOn) { if (isScanning) onCancelScan() else onStartScan() }
                .border(1.dp, if (isScanning) Color(0xFFFF1744) else Color(0xFF3D4452), RoundedCornerShape(12.dp))
                .testTag("scan_button")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Cached,
                    contentDescription = null,
                    tint = if (isScanning) Color.White else AmberPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isScanning) "STOP SCAN" else "FULL SCAN",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isScanning) Color.White else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Direct Entry Keypad
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF2E333D),
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = isPowerOn, onClick = onDirectEntry)
                .border(1.dp, Color(0xFF3D4452), RoundedCornerShape(12.dp))
                .testTag("direct_entry_button")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Dialpad, contentDescription = null, tint = AmberSecondary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("KEYPAD", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Rename Current
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF2E333D),
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = isPowerOn, onClick = onRenameCurrent)
                .border(1.dp, Color(0xFF3D4452), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = AmberSecondary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("TAG", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StationItemCard(
    station: FmStation,
    isActive: Boolean,
    onTune: () -> Unit,
    onEditName: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onTune)
            .testTag("station_card_${station.formattedFrequency}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isActive) Color(0xFF2A2100) else TunerCardSurface),
        border = androidx.compose.foundation.BorderStroke(
            if (isActive) 1.5.dp else 1.dp,
            if (isActive) AmberPrimary else Color(0xFF323843)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (isActive) AmberPrimary else Color(0xFF131519),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${station.formattedFrequency}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isActive) Color.Black else AmberPrimary,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        if (isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• PLAYING",
                                style = MaterialTheme.typography.labelSmall,
                                color = SignalGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = station.rdsText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEditName, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = Color.Gray)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Station", tint = Color(0xFF804040))
                }
            }
        }
    }
}

@Composable
private fun EmptyPresetsBox(isFavoritesTab: Boolean, onScan: () -> Unit) {
    Surface(
        color = Color(0xFF131519),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, Color(0xFF2C313B), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Radio, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isFavoritesTab) "No Favorite Presets Yet" else "No Discovered Stations",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isFavoritesTab) "Tap the heart icon on any station to save it here for instant one-touch tuning." else "Run a Full Band Hardware Scan to automatically discover all local FM carriers.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun ScanDiagnosticItemCard(audit: HardwareScanResult, onTune: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onTune),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (audit.isLocked) Color(0xFF182A1A) else TunerCardSurface),
        border = androidx.compose.foundation.BorderStroke(
            if (audit.isLocked) 1.5.dp else 1.dp,
            if (audit.isLocked) SignalGreen else Color(0xFF323843)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (audit.isLocked) SignalGreen else Color(0xFF131519),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = String.format("%.1f MHz", audit.frequencyMHz),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (audit.isLocked) Color.Black else AmberPrimary,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (audit.isLocked) "CARRIER LOCKED" else "REJECTED BY V4L2",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (audit.isLocked) SignalGreen else Color(0xFFFF8080),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${audit.rssi} dBm",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Stereo 19kHz: ${if (audit.isStereo) "LOCKED" else "MONO"} | RDS: ${if (audit.rdsAvailable) "YES" else "NONE"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFC0C5D0),
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Reason: ${audit.reason}",
                style = MaterialTheme.typography.bodySmall,
                color = if (audit.isLocked) SignalGreen else Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun EmptyScanAuditBox(onScan: () -> Unit) {
    Surface(
        color = Color(0xFF131519),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, Color(0xFF2C313B), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No V4L2 Hardware Scan Executed",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap 'FULL SCAN' to step from 87.5 to 108.0 MHz and inspect raw V4L2 ioctl metrics, RSSI threshold tests, and stereo pilot verifications for every single frequency step.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

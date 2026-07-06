package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hardware.AudioOutputMode
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.TunerCardSurface

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsModal(
    driverPath: String,
    driverStatus: String,
    chipsetInfo: String,
    isPowerOn: Boolean,
    currentRssi: Int,
    isStereo: Boolean,
    audioOutputMode: AudioOutputMode,
    isHeadsetConnected: Boolean,
    audioPipelineStatus: String = "Active",
    audioRoutingLogs: List<String> = emptyList(),
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = TunerCardSurface,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.DeveloperBoard,
                    contentDescription = "Diagnostics",
                    tint = AmberPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Qualcomm SM6375 Hardware Telemetry",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Native V4L2 Driver & Audio Path Diagnostics",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF323843))
            Spacer(modifier = Modifier.height(16.dp))

            // Diagnostic Item Cards
            DiagnosticRow(
                icon = Icons.Default.Memory,
                label = "Chipset / SoC",
                value = chipsetInfo,
                statusColor = Color.White
            )

            DiagnosticRow(
                icon = Icons.Default.Radio,
                label = "V4L2 Driver Node",
                value = "$driverPath (${if (isPowerOn) "ACTIVE" else "POWERED OFF"})",
                statusColor = if (isPowerOn) SignalGreen else Color.Gray
            )

            DiagnosticRow(
                icon = Icons.Default.Security,
                label = "SELinux Permissions",
                value = "Unrestricted access granted to driver /dev/radio0",
                statusColor = SignalGreen
            )

            DiagnosticRow(
                icon = Icons.Default.Speaker,
                label = "Audio Routing Path",
                value = "${audioOutputMode.displayName} (${if (isHeadsetConnected) "Antenna Attached" else "Antenna Open"})",
                statusColor = AmberPrimary
            )

            DiagnosticRow(
                icon = Icons.Default.CheckCircle,
                label = "Qualcomm Audio HAL Pipeline",
                value = audioPipelineStatus,
                statusColor = SignalGreen
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Real-time Tuner Telemetry Box
            Surface(
                color = Color(0xFF14171C),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF2C313B), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "V4L2 IOCTL HARDWARE STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "VIDIOC_G_TUNER RSSI: $currentRssi dBm",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE0E0E0),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isStereo) "STEREO (19kHz Pilot Lock)" else "MONO",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isStereo) SignalGreen else Color.Yellow,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Audio Routing Telemetry Logs
            Surface(
                color = Color(0xFF101216),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF2C313B), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "QUALCOMM AUDIO HAL ROUTING TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val logsToShow = if (audioRoutingLogs.isNotEmpty()) audioRoutingLogs.takeLast(6) else listOf("HAL audio routing initialized.")
                    logsToShow.forEach { logMsg ->
                        Text(
                            text = "• $logMsg",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC0C5D0),
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CLOSE DIAGNOSTICS", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    icon: ImageVector,
    label: String,
    value: String,
    statusColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

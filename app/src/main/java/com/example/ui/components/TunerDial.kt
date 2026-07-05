package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hardware.V4l2RadioDriver
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.TunerCardSurface

@Composable
fun TunerDial(
    currentFrequency: Float,
    isPowerOn: Boolean,
    onFrequencyChange: (Float) -> Unit,
    onStepDown: () -> Unit,
    onStepUp: () -> Unit,
    onSeekDown: () -> Unit,
    onSeekUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, if (isPowerOn) AmberPrimary.copy(alpha = 0.5f) else Color.DarkGray, RoundedCornerShape(16.dp)),
        color = TunerCardSurface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Retro frequency scale header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FM MHz",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isPowerOn) AmberSecondary else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val scaleMarks = listOf(88, 92, 96, 100, 104, 108)
                    for (mhz in scaleMarks) {
                        Text(
                            text = "$mhz",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPowerOn) Color(0xFFCCCCCC) else Color.DarkGray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Simulated Analog Ruler Scale Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFF0F1114), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF2C313B), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .pointerInput(isPowerOn) {
                            if (!isPowerOn) return@pointerInput
                            detectHorizontalDragGestures { change, dragAmount ->
                                change.consume()
                                val deltaMhz = -(dragAmount / size.width) * (V4l2RadioDriver.BAND_MAX_MHZ - V4l2RadioDriver.BAND_MIN_MHZ)
                                onFrequencyChange(currentFrequency + deltaMhz)
                            }
                        }
                ) {
                    val totalRange = V4l2RadioDriver.BAND_MAX_MHZ - V4l2RadioDriver.BAND_MIN_MHZ
                    val tickSteps = 41 // Every 0.5 MHz
                    val stepWidth = size.width / (tickSteps - 1)

                    for (i in 0 until tickSteps) {
                        val freq = V4l2RadioDriver.BAND_MIN_MHZ + (i * 0.5f)
                        val x = i * stepWidth
                        val isMajor = (freq % 2.0f == 0.0f)
                        val tickHeight = if (isMajor) size.height * 0.6f else size.height * 0.3f
                        val tickColor = if (isPowerOn) {
                            if (isMajor) AmberSecondary.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.4f)
                        } else Color.DarkGray

                        drawLine(
                            color = tickColor,
                            start = Offset(x, size.height - tickHeight),
                            end = Offset(x, size.height),
                            strokeWidth = if (isMajor) 3f else 1.5f
                        )
                    }

                    // Illuminated Needle for currentFrequency
                    if (isPowerOn) {
                        val ratio = (currentFrequency - V4l2RadioDriver.BAND_MIN_MHZ) / totalRange
                        val needleX = ratio * size.width
                        drawLine(
                            color = Color(0xFFFF1744),
                            start = Offset(needleX, 0f),
                            end = Offset(needleX, size.height),
                            strokeWidth = 5f
                        )
                        drawCircle(
                            color = AmberPrimary,
                            radius = 6f,
                            center = Offset(needleX, 6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Slider for quick tuning
            Slider(
                value = currentFrequency,
                onValueChange = { if (isPowerOn) onFrequencyChange(it) },
                valueRange = V4l2RadioDriver.BAND_MIN_MHZ..V4l2RadioDriver.BAND_MAX_MHZ,
                enabled = isPowerOn,
                colors = SliderDefaults.colors(
                    thumbColor = AmberPrimary,
                    activeTrackColor = AmberSecondary,
                    inactiveTrackColor = Color(0xFF2A2E38)
                ),
                modifier = Modifier.fillMaxWidth().testTag("tuner_slider")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tuning & Seek Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Seek Down Button
                Button(
                    onClick = onSeekDown,
                    enabled = isPowerOn,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E333D), contentColor = AmberPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("seek_down_button")
                ) {
                    Icon(Icons.Default.FastRewind, contentDescription = "Seek Down")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SEEK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                // Fine Step Buttons (-0.1 / +0.1)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onStepDown,
                        enabled = isPowerOn,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF2A2E38), CircleShape)
                            .border(1.dp, Color(0xFF3D4452), CircleShape)
                            .testTag("step_down_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Step Down 0.1 MHz",
                            tint = if (isPowerOn) Color.White else Color.Gray
                        )
                    }

                    IconButton(
                        onClick = onStepUp,
                        enabled = isPowerOn,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF2A2E38), CircleShape)
                            .border(1.dp, Color(0xFF3D4452), CircleShape)
                            .testTag("step_up_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Step Up 0.1 MHz",
                            tint = if (isPowerOn) Color.White else Color.Gray
                        )
                    }
                }

                // Seek Up Button
                Button(
                    onClick = onSeekUp,
                    enabled = isPowerOn,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E333D), contentColor = AmberPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("seek_up_button")
                ) {
                    Text("SEEK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.FastForward, contentDescription = "Seek Up")
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.hardware.V4l2RadioDriver
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.TunerCardSurface

@Composable
fun DirectFrequencyDialog(
    initialFrequency: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var textValue by remember { mutableStateOf(String.format("%.1f", initialFrequency)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = TunerCardSurface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().testTag("direct_entry_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Direct Frequency Entry",
                    style = MaterialTheme.typography.titleMedium,
                    color = AmberPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter frequency between ${V4l2RadioDriver.BAND_MIN_MHZ} and ${V4l2RadioDriver.BAND_MAX_MHZ} MHz:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        textValue = input
                        errorMessage = null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberPrimary,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("frequency_input")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF1744)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            val parsed = textValue.toFloatOrNull()
                            if (parsed != null && parsed >= V4l2RadioDriver.BAND_MIN_MHZ && parsed <= V4l2RadioDriver.BAND_MAX_MHZ) {
                                onConfirm((Math.round(parsed * 10.0) / 10.0).toFloat())
                                onDismiss()
                            } else {
                                errorMessage = "Invalid frequency ($textValue)"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = Color.Black),
                        modifier = Modifier.testTag("confirm_tune_button")
                    ) {
                        Text("TUNE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RenamePresetDialog(
    currentName: String,
    frequency: Float,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nameValue by remember { mutableStateOf(currentName) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = TunerCardSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Edit Preset (${String.format("%.1f", frequency)} MHz)",
                    style = MaterialTheme.typography.titleMedium,
                    color = AmberPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameValue,
                    onValueChange = { nameValue = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberPrimary,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            if (nameValue.isNotBlank()) {
                                onConfirm(nameValue.trim())
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = Color.Black)
                    ) {
                        Text("SAVE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

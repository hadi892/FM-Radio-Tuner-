package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = AmberPrimary,
  onPrimary = Color.Black,
  primaryContainer = Color(0xFF3B2500),
  onPrimaryContainer = AmberSecondary,
  secondary = AmberSecondary,
  onSecondary = Color.Black,
  tertiary = AmberTertiary,
  background = TunerDarkBackground,
  onBackground = Color(0xFFE4E6EB),
  surface = TunerSurface,
  onSurface = Color(0xFFE4E6EB),
  surfaceVariant = TunerCardSurface,
  onSurfaceVariant = Color(0xFFB0B3B8),
  outline = TunerBorder
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}

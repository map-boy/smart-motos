package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
  primary = SmartYellow,
  onPrimary = Color.White,
  primaryContainer = SmartYellowLight,
  onPrimaryContainer = SmartYellowDark,
  secondary = SleekDeepPurple,
  onSecondary = Color.White,
  background = SmartBlackDark,
  onBackground = SmartBlack,
  surface = SmartBlackCard,
  onSurface = SmartBlack,
  surfaceVariant = SmartBlackPaper,
  onSurfaceVariant = SmartGrayMedium,
  outline = SmartGrayDark,
  error = SmartError,
  onError = Color.White
)

private val DarkScheme = darkColorScheme(
  primary = SmartPrimaryDark,
  onPrimary = SmartOnPrimaryDark,
  primaryContainer = SmartPrimaryContainerDark,
  onPrimaryContainer = SmartOnPrimaryContainerDark,
  secondary = SmartPrimaryDark,
  onSecondary = SmartOnPrimaryDark,
  background = SmartBackgroundDark,
  onBackground = SmartOnBackgroundDark,
  surface = SmartSurfaceDark,
  onSurface = SmartOnSurfaceDark,
  surfaceVariant = SmartSurfaceVariantDark,
  onSurfaceVariant = SmartOnSurfaceVariantDark,
  outline = SmartOutlineDark,
  error = SmartError,
  onError = Color.White
)

@Composable
fun SmartMotosTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkScheme else LightScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  SmartMotosTheme(darkTheme = darkTheme, content = content)
}

package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Emerald500,
    onPrimary = Emerald950,
    primaryContainer = Emerald900,
    onPrimaryContainer = Emerald100,
    secondary = Amber500,
    onSecondary = Slate950,
    secondaryContainer = Color(0xFF451A03),
    onSecondaryContainer = Amber100,
    tertiary = Rose500,
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    error = Rose500,
    errorContainer = Color(0xFF4C0519),
    onErrorContainer = Rose100,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Emerald700,
    onPrimary = Color.White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald900,
    secondary = Amber500,
    onSecondary = Color.White,
    secondaryContainer = Amber100,
    onSecondaryContainer = Amber800,
    tertiary = Teal700,
    onTertiary = Color.White,
    tertiaryContainer = Teal100,
    onTertiaryContainer = Emerald950,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate800,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500,
    outline = Slate200,
    error = Rose500,
    errorContainer = Rose100,
    onErrorContainer = Rose800,
  )

@Composable
fun KitchenSolutionTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

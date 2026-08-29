package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.model.VybeAccent
import com.example.data.model.VybeThemeMode

data class VybeCustomColors(
  val accent: Color = VybePink,
  val surfaceBorder: Color = VybeSurfaceBorder,
  val textMuted: Color = VybeTextMuted
)

val LocalVybeColors = staticCompositionLocalOf { VybeCustomColors() }

@Composable
fun MyApplicationTheme(
  themeMode: VybeThemeMode = VybeThemeMode.DARK,
  accent: VybeAccent = VybeAccent.PINK,
  content: @Composable () -> Unit
) {
  val isDark = when (themeMode) {
    VybeThemeMode.DARK -> true
    VybeThemeMode.LIGHT -> false
    VybeThemeMode.SYSTEM -> isSystemInDarkTheme()
  }

  val accentColor = getAccentColor(accent)

  val colorScheme = if (isDark) {
    darkColorScheme(
      primary = accentColor,
      onPrimary = Color.White,
      primaryContainer = accentColor.copy(alpha = 0.2f),
      onPrimaryContainer = Color.White,
      secondary = VybePurple,
      onSecondary = Color.White,
      background = VybeBackground,
      onBackground = VybeTextPrimary,
      surface = VybeSurface,
      onSurface = VybeTextPrimary,
      surfaceVariant = VybeSurfaceLight,
      onSurfaceVariant = VybeTextSecondary,
      outline = VybeSurfaceBorder,
      outlineVariant = VybeSurfaceBorderSubtle
    )
  } else {
    lightColorScheme(
      primary = accentColor,
      onPrimary = Color.White,
      primaryContainer = accentColor.copy(alpha = 0.15f),
      onPrimaryContainer = accentColor,
      secondary = VybePurple,
      onSecondary = Color.White,
      background = VybeLightBackground,
      onBackground = VybeLightTextPrimary,
      surface = VybeLightSurface,
      onSurface = VybeLightTextPrimary,
      surfaceVariant = Color(0xFFEBEBF5),
      onSurfaceVariant = VybeLightTextSecondary,
      outline = VybeLightSurfaceBorder,
      outlineVariant = Color(0x22000000)
    )
  }

  val customColors = VybeCustomColors(
    accent = accentColor,
    surfaceBorder = if (isDark) VybeSurfaceBorder else VybeLightSurfaceBorder,
    textMuted = if (isDark) VybeTextMuted else Color(0xFF8E8B9E)
  )

  CompositionLocalProvider(LocalVybeColors provides customColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
  }
}

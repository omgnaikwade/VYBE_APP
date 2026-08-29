package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.model.VybeAccent

// Dark Theme Core Palette
val VybeBackground = Color(0xFF08070C)
val VybeBackgroundSecondary = Color(0xFF0F0E17)
val VybeSurface = Color(0xFF161420)
val VybeSurfaceLight = Color(0xFF1F1C2B)
val VybeSurfaceBorder = Color(0xFF2B273A)
val VybeSurfaceBorderSubtle = Color(0x33FFFFFF)

// Accent Colors
val VybePink = Color(0xFFFF2D75)
val VybePinkDark = Color(0xFF9E1442)
val VybePurple = Color(0xFFA855F7)
val VybePurpleDark = Color(0xFF6B21A8)
val VybeGreen = Color(0xFF10B981)
val VybeBlue = Color(0xFF3B82F6)
val VybeOrange = Color(0xFFF97316)

// Text Colors
val VybeTextPrimary = Color(0xFFFFFFFF)
val VybeTextSecondary = Color(0xFFA3A0B3)
val VybeTextMuted = Color(0xFF6E6A80)

// Light Theme Alternates
val VybeLightBackground = Color(0xFFF7F7FA)
val VybeLightSurface = Color(0xFFFFFFFF)
val VybeLightSurfaceBorder = Color(0xFFE2E2EA)
val VybeLightTextPrimary = Color(0xFF121118)
val VybeLightTextSecondary = Color(0xFF5E5C6C)

// Gradients
val VybeBrandGradient = Brush.horizontalGradient(
  listOf(VybePink, Color(0xFFD946EF), VybePurple)
)

val VybePlayerGlowGradient = Brush.verticalGradient(
  listOf(Color(0x55FF2D75), Color(0x1108070C), Color(0xFF08070C))
)

fun getAccentColor(accent: VybeAccent): Color {
  return when (accent) {
    VybeAccent.PINK -> VybePink
    VybeAccent.PURPLE -> VybePurple
    VybeAccent.GREEN -> VybeGreen
    VybeAccent.BLUE -> VybeBlue
    VybeAccent.ORANGE -> VybeOrange
  }
}

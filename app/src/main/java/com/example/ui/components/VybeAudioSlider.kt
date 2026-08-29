package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun VybeAudioSlider(
  value: Float, // 0f to 1f
  onValueChange: (Float) -> Unit,
  onValueChangeFinished: () -> Unit,
  accentColor: Color,
  modifier: Modifier = Modifier,
  trackHeight: Dp = 3.5.dp,
  inactiveTrackColor: Color = Color(0xFF282535),
  glowColor: Color = accentColor.copy(alpha = 0.35f)
) {
  var isDragging by remember { mutableStateOf(false) }
  var dragRatio by remember { mutableFloatStateOf(value) }

  val currentProgress = if (isDragging) dragRatio else value.coerceIn(0f, 1f)

  val thumbRadiusDp by animateDpAsState(
    targetValue = if (isDragging) 7.dp else 4.5.dp,
    animationSpec = tween(150),
    label = "thumbRadius"
  )

  val glowRadiusDp by animateDpAsState(
    targetValue = if (isDragging) 15.dp else 0.dp,
    animationSpec = tween(150),
    label = "glowRadius"
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(36.dp) // Generous touch target
      .testTag("player_progress_slider")
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = { offset ->
            val ratio = (offset.x / size.width).coerceIn(0f, 1f)
            dragRatio = ratio
            onValueChange(ratio)
            onValueChangeFinished()
          }
        )
      }
      .pointerInput(Unit) {
        detectHorizontalDragGestures(
          onDragStart = { offset ->
            isDragging = true
            val ratio = (offset.x / size.width).coerceIn(0f, 1f)
            dragRatio = ratio
            onValueChange(ratio)
          },
          onDragEnd = {
            isDragging = false
            onValueChangeFinished()
          },
          onDragCancel = {
            isDragging = false
          },
          onHorizontalDrag = { change, _ ->
            change.consume()
            val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
            dragRatio = ratio
            onValueChange(ratio)
          }
        )
      },
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxWidth().height(36.dp)) {
      val width = size.width
      val height = size.height
      val centerY = height / 2f
      val trackHeightPx = trackHeight.toPx()
      val cornerRadiusPx = trackHeightPx / 2f
      val progressX = (currentProgress * width).coerceIn(0f, width)

      // 1. Draw Inactive Track (sleek slim line)
      drawRoundRect(
        color = inactiveTrackColor,
        topLeft = Offset(0f, centerY - trackHeightPx / 2f),
        size = Size(width, trackHeightPx),
        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
      )

      // 2. Draw Active Track (vibrant sleek neon gradient)
      if (progressX > 0f) {
        drawRoundRect(
          brush = Brush.horizontalGradient(
            colors = listOf(
              accentColor.copy(alpha = 0.9f),
              accentColor
            ),
            startX = 0f,
            endX = progressX.coerceAtLeast(1f)
          ),
          topLeft = Offset(0f, centerY - trackHeightPx / 2f),
          size = Size(progressX, trackHeightPx),
          cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )
      }

      // 3. Ambient Glow during dragging
      if (glowRadiusDp > 0.dp) {
        drawCircle(
          color = glowColor,
          radius = glowRadiusDp.toPx(),
          center = Offset(progressX, centerY)
        )
      }

      // 4. Draw Thumb (Ultra-sleek modern circle)
      val thumbRadiusPx = thumbRadiusDp.toPx()

      // Outer accent border
      drawCircle(
        color = accentColor,
        radius = thumbRadiusPx + 1.5.dp.toPx(),
        center = Offset(progressX, centerY)
      )

      // Inner white / accent core
      drawCircle(
        color = Color.White,
        radius = thumbRadiusPx,
        center = Offset(progressX, centerY)
      )
    }
  }
}

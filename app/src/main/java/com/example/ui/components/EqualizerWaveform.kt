package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalVybeColors

@Composable
fun EqualizerWaveform(
  isPlaying: Boolean,
  modifier: Modifier = Modifier,
  color: Color = LocalVybeColors.current.accent,
  barCount: Int = 4,
  maxHeight: Dp = 16.dp
) {
  val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

  val height1 by infiniteTransition.animateFloat(
    initialValue = 0.2f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "h1"
  )

  val height2 by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = 0.3f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 350, delayMillis = 100, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "h2"
  )

  val height3 by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 0.9f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 450, delayMillis = 200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "h3"
  )

  val height4 by infiniteTransition.animateFloat(
    initialValue = 0.8f,
    targetValue = 0.2f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 320, delayMillis = 50, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "h4"
  )

  val heights = listOf(height1, height2, height3, height4)

  Row(
    modifier = modifier.height(maxHeight),
    horizontalArrangement = Arrangement.spacedBy(2.5.dp),
    verticalAlignment = Alignment.Bottom
  ) {
    for (i in 0 until barCount) {
      val animatedFraction = if (isPlaying) heights[i % heights.size] else 0.3f
      Box(
        modifier = Modifier
          .width(2.5.dp)
          .height(maxHeight * animatedFraction)
          .clip(RoundedCornerShape(1.dp))
          .background(color)
      )
    }
  }
}

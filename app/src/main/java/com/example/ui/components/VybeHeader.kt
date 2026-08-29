package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalVybeColors
import com.example.ui.theme.VybePink
import com.example.ui.theme.VybePurple

@Composable
fun VybeHeader(
  modifier: Modifier = Modifier,
  onNotificationClick: () -> Unit = {},
  onCastClick: () -> Unit = {}
) {
  val accent = LocalVybeColors.current.accent

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Modern VYBE Gradient Logo
    VybeLogoText(fontSize = 24)

    // Right Action Icons
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box {
        IconButton(
          onClick = onNotificationClick,
          modifier = Modifier
            .size(40.dp)
            .testTag("header_notification_button")
        ) {
          Icon(
            imageVector = Icons.Outlined.Notifications,
            contentDescription = "Notifications",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(24.dp)
          )
        }
        // Small active notification dot
        Box(
          modifier = Modifier
            .size(7.dp)
            .align(Alignment.TopEnd)
            .padding(top = 8.dp, end = 8.dp)
            .clip(CircleShape)
            .background(accent)
        )
      }

      Spacer(modifier = Modifier.width(4.dp))

      IconButton(
        onClick = onCastClick,
        modifier = Modifier
          .size(40.dp)
          .testTag("header_cast_button")
      ) {
        Icon(
          imageVector = Icons.Outlined.Cast,
          contentDescription = "Cast to Device",
          tint = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.size(23.dp)
        )
      }
    }
  }
}

@Composable
fun VybeLogoText(
  fontSize: Int = 24,
  modifier: Modifier = Modifier
) {
  val accent = LocalVybeColors.current.accent

  Text(
    text = "VYBE",
    style = TextStyle(
      fontSize = fontSize.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.SansSerif,
      letterSpacing = 1.5.sp,
      brush = Brush.horizontalGradient(
        listOf(
          accent,
          Color(0xFFE879F9),
          VybePurple
        )
      )
    ),
    modifier = modifier.testTag("vybe_logo_text")
  )
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.QueueMusic
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.PlaybackState
import com.example.ui.theme.LocalVybeColors

@Composable
fun VybeMiniPlayer(
  playbackState: PlaybackState,
  onPlayPauseClick: () -> Unit,
  onQueueClick: () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val song = playbackState.currentSong ?: return
  val accent = LocalVybeColors.current.accent

  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 10.dp, vertical = 4.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(
        width = 0.75.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp)
      )
      .clickable { onClick() }
      .testTag("mini_player_container")
  ) {
    Column {
      // Top Thin Progress Line
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(2.dp)
          .background(MaterialTheme.colorScheme.outlineVariant)
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth(playbackState.progress.coerceIn(0f, 1f))
            .height(2.dp)
            .background(accent)
        )
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Thumbnail + Info
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(
                Brush.linearGradient(
                  listOf(
                    Color(song.primaryColor),
                    Color(song.secondaryColor)
                  )
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            if (song.artworkUrl.isNotEmpty()) {
              AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                  .data(song.artworkUrl)
                  .crossfade(true)
                  .build(),
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
            } else {
              Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = song.title,
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
              ),
              color = MaterialTheme.colorScheme.onBackground,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = song.artist,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Action Buttons: Play/Pause and Queue
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
              .size(40.dp)
              .testTag("mini_player_play_pause")
          ) {
            Icon(
              imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
              tint = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.size(26.dp)
            )
          }

          IconButton(
            onClick = onQueueClick,
            modifier = Modifier
              .size(40.dp)
              .testTag("mini_player_queue")
          ) {
            Icon(
              imageVector = Icons.Outlined.QueueMusic,
              contentDescription = "Queue",
              tint = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.size(22.dp)
            )
          }
        }
      }
    }
  }
}

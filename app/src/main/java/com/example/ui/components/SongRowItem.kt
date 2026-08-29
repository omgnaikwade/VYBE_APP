package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Song
import com.example.ui.theme.LocalVybeColors

@Composable
fun SongRowItem(
  song: Song,
  isCurrentPlaying: Boolean,
  isPlaying: Boolean,
  onClick: () -> Unit,
  onMenuClick: () -> Unit,
  modifier: Modifier = Modifier,
  imageSize: Dp = 48.dp,
  showMenu: Boolean = true
) {
  val accent = LocalVybeColors.current.accent

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .testTag("song_item_${song.id}"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f)
    ) {
      // Artwork thumbnail
      Box(
        modifier = Modifier
          .size(imageSize)
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
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(22.dp)
          )
        }

        // Overlay equalizer if active
        if (isCurrentPlaying) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
          ) {
            EqualizerWaveform(
              isPlaying = isPlaying,
              color = accent,
              maxHeight = 14.dp
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Song Title & Artist
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = song.title,
          style = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = if (isCurrentPlaying) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
          ),
          color = if (isCurrentPlaying) accent else MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = song.artist,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    if (showMenu) {
      IconButton(
        onClick = onMenuClick,
        modifier = Modifier
          .size(36.dp)
          .testTag("song_menu_${song.id}")
      ) {
        Icon(
          imageVector = Icons.Default.MoreVert,
          contentDescription = "Options",
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}

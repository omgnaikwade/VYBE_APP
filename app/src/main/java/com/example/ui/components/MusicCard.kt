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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.ui.theme.LocalVybeColors

@Composable
fun LargeHitMusicCard(
  song: Song,
  onClick: () -> Unit,
  onPlayClick: () -> Unit,
  modifier: Modifier = Modifier,
  cardWidth: Dp = 140.dp
) {
  val accent = LocalVybeColors.current.accent

  Column(
    modifier = modifier
      .width(cardWidth)
      .clickable { onClick() }
      .testTag("hit_card_${song.id}")
  ) {
    Box(
      modifier = Modifier
        .size(cardWidth)
        .clip(RoundedCornerShape(14.dp))
        .background(
          Brush.linearGradient(
            listOf(
              Color(song.primaryColor),
              Color(song.secondaryColor)
            )
          )
        )
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
      }

      // Subtle gradient vignette at bottom
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.4f)
              )
            )
          )
      )

      // Circular Play Button on bottom right
      Box(
        modifier = Modifier
          .padding(8.dp)
          .size(32.dp)
          .align(Alignment.BottomEnd)
          .clip(CircleShape)
          .background(Color.Black.copy(alpha = 0.65f))
          .clickable { onPlayClick() }
          .padding(4.dp),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.PlayArrow,
          contentDescription = "Play",
          tint = Color.White,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = song.title,
      style = MaterialTheme.typography.bodyMedium.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp
      ),
      color = MaterialTheme.colorScheme.onBackground,
      maxLines = 2,
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

@Composable
fun TrendingPlaylistCard(
  playlist: Playlist,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  cardWidth: Dp = 110.dp
) {
  Box(
    modifier = modifier
      .width(cardWidth)
      .height(110.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(
        Brush.linearGradient(
          listOf(
            Color(playlist.gradientStart),
            Color(playlist.gradientEnd)
          )
        )
      )
      .clickable { onClick() }
      .testTag("playlist_card_${playlist.id}")
  ) {
    if (playlist.coverUrl.isNotEmpty()) {
      AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
          .data(playlist.coverUrl)
          .crossfade(true)
          .build(),
        contentDescription = playlist.title,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              Color.Transparent,
              Color.Black.copy(alpha = 0.7f)
            )
          )
        )
        .padding(8.dp),
      contentAlignment = Alignment.BottomStart
    ) {
      Text(
        text = playlist.title,
        style = MaterialTheme.typography.bodySmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp
        ),
        color = Color.White,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

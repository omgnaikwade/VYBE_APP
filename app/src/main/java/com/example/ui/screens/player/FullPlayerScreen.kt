package com.example.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AudioQuality
import com.example.data.model.Song
import com.example.ui.components.VybeAudioSlider
import com.example.ui.theme.LocalVybeColors
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
  viewModel: MainViewModel,
  onCollapse: () -> Unit,
  onQueueClick: () -> Unit,
  onCastClick: () -> Unit,
  onSongOptionsClick: (Song) -> Unit,
  onQualityClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val playbackState by viewModel.playbackState.collectAsState()
  val favorites by viewModel.favoriteSongs.collectAsState()
  val song = playbackState.currentSong ?: return
  val isFavorite = favorites.any { it.id == song.id }
  val accent = LocalVybeColors.current.accent

  var isUserDragging by remember { mutableStateOf(false) }
  var dragPosition by remember { mutableFloatStateOf(0f) }

  Surface(
    modifier = modifier
      .fillMaxSize()
      .testTag("full_player_screen"),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = 24.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // 1. Top Bar: Down Chevron | "PLAYING FROM" Playlist | Three-Dot Menu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onCollapse,
          modifier = Modifier
            .size(40.dp)
            .testTag("player_collapse_button")
        ) {
          Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Collapse Player",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(30.dp)
          )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "PLAYING FROM",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = playbackState.currentPlaylistName,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
          )
        }

        IconButton(
          onClick = { onSongOptionsClick(song) },
          modifier = Modifier.size(40.dp)
        ) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Song options",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(22.dp)
          )
        }
      }

      // 2. Large Album Artwork Card with Ambient Neon Back-Glow
      Box(
        modifier = Modifier
          .fillMaxWidth(0.88f)
          .aspectRatio(1f),
        contentAlignment = Alignment.Center
      ) {
        // Ambient soft color backglow
        Box(
          modifier = Modifier
            .fillMaxSize(0.92f)
            .clip(RoundedCornerShape(32.dp))
            .background(
              Brush.radialGradient(
                listOf(
                  Color(song.primaryColor).copy(alpha = 0.45f),
                  Color(song.secondaryColor).copy(alpha = 0.25f),
                  Color.Transparent
                )
              )
            )
        )

        // Artwork Container
        Box(
          modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
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
              modifier = Modifier.size(80.dp)
            )
          }

          // Inner subtle gradient vignette
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.35f)
                  )
                )
              )
          )
        }
      }

      // 3. Track Info (Title, Artist, Heart Favorite Toggle)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = song.title,
            style = MaterialTheme.typography.displayMedium.copy(
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = song.artist,
            style = MaterialTheme.typography.bodyLarge.copy(
              fontSize = 15.sp,
              letterSpacing = 0.1.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        IconButton(
          onClick = { viewModel.toggleFavorite(song) },
          modifier = Modifier
            .size(44.dp)
            .testTag("player_favorite_button")
        ) {
          Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Favorite",
            tint = if (isFavorite) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(26.dp)
          )
        }
      }

      // 4. Sleek Ultra-Thin Audio Progress Scrubber & Timestamps
      Column(modifier = Modifier.fillMaxWidth()) {
        val sliderValue = if (isUserDragging) {
          dragPosition
        } else {
          if (playbackState.durationSeconds > 0) {
            playbackState.positionSeconds.toFloat() / playbackState.durationSeconds.toFloat()
          } else 0f
        }

        VybeAudioSlider(
          value = sliderValue.coerceIn(0f, 1f),
          onValueChange = {
            isUserDragging = true
            dragPosition = it
          },
          onValueChangeFinished = {
            isUserDragging = false
            val newSec = (dragPosition * playbackState.durationSeconds).toInt()
            viewModel.seekTo(newSec)
          },
          accentColor = accent,
          modifier = Modifier.fillMaxWidth()
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = if (isUserDragging) {
              val sec = (dragPosition * playbackState.durationSeconds).toInt()
              String.format("%d:%02d", sec / 60, sec % 60)
            } else {
              playbackState.formattedPosition
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = playbackState.formattedDuration,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // 5. Playback Controls (Shuffle, Prev, Play/Pause 64dp, Next, Repeat)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = { viewModel.toggleShuffle() },
          modifier = Modifier.size(44.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Shuffle,
            contentDescription = "Shuffle",
            tint = if (playbackState.isShuffle) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
          )
        }

        IconButton(
          onClick = { viewModel.previous() },
          modifier = Modifier.size(48.dp)
        ) {
          Icon(
            imageVector = Icons.Default.SkipPrevious,
            contentDescription = "Previous Track",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(32.dp)
          )
        }

        // Circular Big Play/Pause Button
        Box(
          modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(accent)
            .clickable { viewModel.togglePlayPause() }
            .testTag("player_play_pause_big"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(38.dp)
          )
        }

        IconButton(
          onClick = { viewModel.next() },
          modifier = Modifier.size(48.dp)
        ) {
          Icon(
            imageVector = Icons.Default.SkipNext,
            contentDescription = "Next Track",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(32.dp)
          )
        }

        IconButton(
          onClick = { viewModel.toggleRepeat() },
          modifier = Modifier.size(44.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Repeat,
            contentDescription = "Repeat",
            tint = if (playbackState.isRepeat) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
          )
        }
      }

      // 6. Bottom Utility Bar: Cast | Quality Pill ("High v") | Queue Button
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onCastClick,
          modifier = Modifier.size(40.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.Devices,
            contentDescription = "Devices",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
          )
        }

        // Audio Quality Selector Pill
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onQualityClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("player_quality_pill"),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.MusicNote,
              contentDescription = null,
              tint = accent,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = playbackState.streamingQuality.title,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.Default.KeyboardArrowDown,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp)
            )
          }
        }

        IconButton(
          onClick = onQueueClick,
          modifier = Modifier
            .size(40.dp)
            .testTag("player_queue_button")
        ) {
          Icon(
            imageVector = Icons.Outlined.QueueMusic,
            contentDescription = "Queue",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }
  }
}

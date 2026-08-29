package com.example.ui.screens.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.data.model.Song
import com.example.ui.components.EqualizerWaveform
import com.example.ui.theme.LocalVybeColors
import com.example.ui.viewmodel.MainViewModel

@Composable
fun QueueScreen(
  viewModel: MainViewModel,
  onBack: () -> Unit,
  onSongOptionsClick: (Song) -> Unit,
  modifier: Modifier = Modifier
) {
  val playbackState by viewModel.playbackState.collectAsState()
  val accent = LocalVybeColors.current.accent

  Surface(
    modifier = modifier
      .fillMaxSize()
      .testTag("queue_screen"),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
      // 1. Header: Back Arrow | "Queue" | More Vert
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("queue_back_button")
        ) {
          Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(28.dp)
          )
        }

        Text(
          text = "Queue",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )

        IconButton(onClick = {}) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Options",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(22.dp)
          )
        }
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
      ) {
        // 2. Now Playing Section
        val currentSong = playbackState.currentSong
        if (currentSong != null) {
          item {
            Text(
              text = "Now Playing",
              style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              ),
              color = accent,
              modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
          }

          item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Box(
                  modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                      Brush.linearGradient(
                        listOf(
                          Color(currentSong.primaryColor),
                          Color(currentSong.secondaryColor)
                        )
                      )
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  if (currentSong.artworkUrl.isNotEmpty()) {
                    AsyncImage(
                      model = ImageRequest.Builder(LocalContext.current)
                        .data(currentSong.artworkUrl)
                        .crossfade(true)
                        .build(),
                      contentDescription = currentSong.title,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize()
                    )
                  } else {
                    Icon(
                      imageVector = Icons.Default.MusicNote,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(22.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = currentSong.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = currentSong.artist,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }

              // Dancing equalizer waveform indicator
              EqualizerWaveform(
                isPlaying = playbackState.isPlaying,
                color = accent,
                maxHeight = 16.dp,
                modifier = Modifier.padding(end = 8.dp)
              )

              IconButton(onClick = { onSongOptionsClick(currentSong) }) {
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

        // 3. Up Next Section
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Up Next",
              style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              ),
              color = MaterialTheme.colorScheme.onBackground
            )

            Text(
              text = "Clear",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
              color = accent,
              modifier = Modifier
                .clickable { viewModel.clearQueue() }
                .testTag("clear_queue_button")
            )
          }
        }

        // Up next songs list (excluding currently playing or starting after current index)
        val queueItems = playbackState.queue.filterIndexed { index, _ -> index != playbackState.currentQueueIndex }

        if (queueItems.isEmpty()) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 30.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Queue is empty",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        } else {
          itemsIndexed(queueItems) { index, song ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  viewModel.playSong(song, playbackState.currentPlaylistName, playbackState.queue)
                }
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("queue_item_${song.id}"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Box(
                  modifier = Modifier
                    .size(44.dp)
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

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
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

              // Reorder Handle & Menu
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.DragHandle,
                  contentDescription = "Reorder",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                  modifier = Modifier.size(20.dp)
                )

                IconButton(
                  onClick = { onSongOptionsClick(song) },
                  modifier = Modifier.size(36.dp)
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
        }
      }
    }
  }
}

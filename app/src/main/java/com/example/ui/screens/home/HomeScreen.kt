package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.ui.components.LargeHitMusicCard
import com.example.ui.components.SongRowItem
import com.example.ui.components.TrendingPlaylistCard
import com.example.ui.components.VybeHeader
import com.example.ui.theme.LocalVybeColors
import com.example.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
  viewModel: MainViewModel,
  onSongOptionsClick: (Song) -> Unit,
  onNotificationClick: () -> Unit,
  onCastClick: () -> Unit,
  onSeeAllClick: (String, List<Song>) -> Unit,
  modifier: Modifier = Modifier
) {
  val discoverSongs by viewModel.discoverSongs.collectAsState()
  val biggestHits by viewModel.biggestHits.collectAsState()
  val danceHits by viewModel.danceHits.collectAsState()
  val trendingPlaylists by viewModel.trendingPlaylists.collectAsState()
  val playbackState by viewModel.playbackState.collectAsState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    VybeHeader(
      onNotificationClick = onNotificationClick,
      onCastClick = onCastClick
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("home_scroll_list"),
      contentPadding = PaddingValues(bottom = 120.dp)
    ) {
      // 1. Discover Section (2 columns of songs)
      item {
        SectionHeader(
          title = "Discover",
          onSeeAllClick = { onSeeAllClick("Discover", discoverSongs) }
        )
      }

      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
        ) {
          if (discoverSongs.isEmpty()) {
            // Sleek loading skeleton for discover items
            repeat(3) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                DiscoverSongSkeleton(modifier = Modifier.weight(1f))
                DiscoverSongSkeleton(modifier = Modifier.weight(1f))
              }
            }
          } else {
            val chunked = discoverSongs.chunked(2)
            chunked.forEach { pair ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                pair.forEach { song ->
                  Box(modifier = Modifier.weight(1f)) {
                    SongRowItem(
                      song = song,
                      isCurrentPlaying = playbackState.currentSong?.id == song.id,
                      isPlaying = playbackState.isPlaying,
                      onClick = { viewModel.playSong(song, "Discover", discoverSongs) },
                      onMenuClick = { onSongOptionsClick(song) },
                      imageSize = 44.dp
                    )
                  }
                }
                if (pair.size == 1) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(20.dp)) }

      // 2. India's biggest hits Section
      item {
        SectionHeader(
          title = "India's biggest hits",
          onSeeAllClick = { onSeeAllClick("India's biggest hits", biggestHits) }
        )
      }

      item {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          modifier = Modifier.testTag("biggest_hits_row")
        ) {
          if (biggestHits.isEmpty()) {
            items(4) {
              CardSkeleton(width = 140.dp, height = 140.dp)
            }
          } else {
            items(biggestHits) { song ->
              LargeHitMusicCard(
                song = song,
                onClick = { viewModel.playSong(song, "India's biggest hits", biggestHits) },
                onPlayClick = { viewModel.playSong(song, "India's biggest hits", biggestHits) }
              )
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(24.dp)) }

      // 3. Dancing on your own Section
      item {
        SectionHeader(
          title = "Dancing on your own",
          onSeeAllClick = { onSeeAllClick("Dancing on your own", danceHits) }
        )
      }

      item {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          modifier = Modifier.testTag("dance_hits_row")
        ) {
          if (danceHits.isEmpty()) {
            items(4) {
              CardSkeleton(width = 140.dp, height = 140.dp)
            }
          } else {
            items(danceHits) { song ->
              LargeHitMusicCard(
                song = song,
                onClick = { viewModel.playSong(song, "Dancing on your own", danceHits) },
                onPlayClick = { viewModel.playSong(song, "Dancing on your own", danceHits) }
              )
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(24.dp)) }

      // 4. Trending community playlists Section
      item {
        SectionHeader(
          title = "Trending community playlists",
          onSeeAllClick = { /* See all trending playlists */ }
        )
      }

      item {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.testTag("trending_playlists_row")
        ) {
          if (trendingPlaylists.isEmpty()) {
            items(3) {
              CardSkeleton(width = 110.dp, height = 110.dp)
            }
          } else {
            items(trendingPlaylists) { playlist ->
              TrendingPlaylistCard(
                playlist = playlist,
                onClick = {
                  val firstSong = discoverSongs.firstOrNull()
                  if (firstSong != null) {
                    viewModel.playSong(firstSong, playlist.title, discoverSongs)
                  }
                }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun DiscoverSongSkeleton(modifier: Modifier = Modifier) {
  Row(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
      .padding(6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Column(modifier = Modifier.weight(1f)) {
      Box(
        modifier = Modifier
          .fillMaxWidth(0.85f)
          .height(12.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant)
      )
      Spacer(modifier = Modifier.height(6.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth(0.55f)
          .height(10.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
      )
    }
  }
}

@Composable
fun CardSkeleton(
  width: androidx.compose.ui.unit.Dp,
  height: androidx.compose.ui.unit.Dp,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.width(width)) {
    Box(
      modifier = Modifier
        .size(width, height)
        .clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
    )
    Spacer(modifier = Modifier.height(6.dp))
    Box(
      modifier = Modifier
        .fillMaxWidth(0.8f)
        .height(12.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
    )
    Spacer(modifier = Modifier.height(4.dp))
    Box(
      modifier = Modifier
        .fillMaxWidth(0.5f)
        .height(10.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    )
  }
}

@Composable
fun SectionHeader(
  title: String,
  onSeeAllClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
      ),
      color = MaterialTheme.colorScheme.onBackground
    )

    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .clickable { onSeeAllClick() }
        .padding(horizontal = 12.dp, vertical = 5.dp)
        .testTag("see_all_${title.lowercase().replace(" ", "_")}")
    ) {
      Text(
        text = "See all",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

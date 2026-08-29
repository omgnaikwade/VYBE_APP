package com.example.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.ui.components.SongRowItem
import com.example.ui.components.VybeHeader
import com.example.ui.theme.LocalVybeColors
import com.example.ui.viewmodel.MainViewModel

enum class LibraryTab {
  PLAYLISTS,
  FAVOURITES,
  HISTORY
}

@Composable
fun LibraryScreen(
  viewModel: MainViewModel,
  onCreatePlaylistClick: () -> Unit,
  onSongOptionsClick: (Song) -> Unit,
  onNotificationClick: () -> Unit,
  onCastClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableStateOf(LibraryTab.PLAYLISTS) }
  val playlists by viewModel.userPlaylists.collectAsState()
  val favorites by viewModel.favoriteSongs.collectAsState()
  val history by viewModel.historySongs.collectAsState()
  val playbackState by viewModel.playbackState.collectAsState()
  val accent = LocalVybeColors.current.accent

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    VybeHeader(
      onNotificationClick = onNotificationClick,
      onCastClick = onCastClick
    )

    Text(
      text = "Library",
      style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp),
      color = MaterialTheme.colorScheme.onBackground,
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )

    // Segmented Tabs: Playlists | Favourites | History
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
      LibraryTabItem(
        title = "Playlists",
        isSelected = selectedTab == LibraryTab.PLAYLISTS,
        onClick = { selectedTab = LibraryTab.PLAYLISTS }
      )
      LibraryTabItem(
        title = "Favourites",
        isSelected = selectedTab == LibraryTab.FAVOURITES,
        onClick = { selectedTab = LibraryTab.FAVOURITES }
      )
      LibraryTabItem(
        title = "History",
        isSelected = selectedTab == LibraryTab.HISTORY,
        onClick = { selectedTab = LibraryTab.HISTORY }
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    when (selectedTab) {
      LibraryTab.PLAYLISTS -> {
        LazyColumn(
          contentPadding = PaddingValues(bottom = 120.dp),
          modifier = Modifier
            .fillMaxSize()
            .testTag("playlists_list")
        ) {
          // "+ Create Playlist" Button Row
          item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onCreatePlaylistClick() }
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("create_playlist_button"),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(52.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Add,
                  contentDescription = "Create Playlist",
                  tint = MaterialTheme.colorScheme.onBackground,
                  modifier = Modifier.size(26.dp)
                )
              }

              Spacer(modifier = Modifier.width(14.dp))

              Text(
                text = "Create Playlist",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onBackground
              )
            }
          }

          // Playlists List
          items(playlists) { playlist ->
            PlaylistItemRow(
              playlist = playlist,
              onClick = {
                // Play songs in playlist or discover
                val first = favorites.firstOrNull() ?: playbackState.currentSong
                if (first != null) {
                  viewModel.playSong(first, playlist.title, favorites)
                }
              }
            )
          }
        }
      }

      LibraryTab.FAVOURITES -> {
        if (favorites.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(bottom = 100.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = accent.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "No favourite songs yet",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        } else {
          LazyColumn(
            contentPadding = PaddingValues(bottom = 120.dp),
            modifier = Modifier.fillMaxSize()
          ) {
            items(favorites) { song ->
              SongRowItem(
                song = song,
                isCurrentPlaying = playbackState.currentSong?.id == song.id,
                isPlaying = playbackState.isPlaying,
                onClick = { viewModel.playSong(song, "Favourites", favorites) },
                onMenuClick = { onSongOptionsClick(song) }
              )
            }
          }
        }
      }

      LibraryTab.HISTORY -> {
        if (history.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(bottom = 100.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "No listening history yet",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        } else {
          LazyColumn(
            contentPadding = PaddingValues(bottom = 120.dp),
            modifier = Modifier.fillMaxSize()
          ) {
            items(history) { song ->
              SongRowItem(
                song = song,
                isCurrentPlaying = playbackState.currentSong?.id == song.id,
                isPlaying = playbackState.isPlaying,
                onClick = { viewModel.playSong(song, "History", history) },
                onMenuClick = { onSongOptionsClick(song) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun LibraryTabItem(
  title: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val accent = LocalVybeColors.current.accent

  Column(
    modifier = Modifier
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
      ) { onClick() }
      .testTag("lib_tab_${title.lowercase()}")
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium.copy(
        fontSize = 14.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
      ),
      color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(6.dp))
    Box(
      modifier = Modifier
        .width(if (isSelected) 30.dp else 0.dp)
        .height(2.dp)
        .background(if (isSelected) accent else Color.Transparent)
    )
  }
}

@Composable
fun PlaylistItemRow(
  playlist: Playlist,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .testTag("playlist_row_${playlist.id}"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f)
    ) {
      Box(
        modifier = Modifier
          .size(52.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(
            Brush.linearGradient(
              listOf(
                Color(playlist.gradientStart),
                Color(playlist.gradientEnd)
              )
            )
          ),
        contentAlignment = Alignment.Center
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
        } else {
          Icon(
            imageVector = Icons.Outlined.Folder,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(24.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column {
        Text(
          text = playlist.title,
          style = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
          ),
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "${playlist.songCount} songs",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    IconButton(onClick = {}) {
      Icon(
        imageVector = Icons.Default.MoreVert,
        contentDescription = "Options",
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.size(18.dp)
      )
    }
  }
}

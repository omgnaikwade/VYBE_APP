package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Song
import com.example.ui.components.CastDevicesDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.NotificationsDialog
import com.example.ui.components.QualitySelectorDialog
import com.example.ui.components.SongOptionsBottomSheet
import com.example.ui.components.VybeBottomNav
import com.example.ui.components.VybeMiniPlayer
import com.example.ui.components.VybeTab
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.SeeAllSongsScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.player.FullPlayerScreen
import com.example.ui.screens.queue.QueueScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

@Composable
fun VybeApp(
  viewModel: MainViewModel = viewModel()
) {
  val themeMode by viewModel.themeMode.collectAsState()
  val accentColor by viewModel.accentColor.collectAsState()
  val playbackState by viewModel.playbackState.collectAsState()
  val favorites by viewModel.favoriteSongs.collectAsState()
  val userPlaylists by viewModel.userPlaylists.collectAsState()
  val streamingQuality by viewModel.streamingQuality.collectAsState()

  var currentTab by remember { mutableStateOf(VybeTab.HOME) }
  var isFullPlayerVisible by remember { mutableStateOf(false) }
  var isQueueVisible by remember { mutableStateOf(false) }

  // Dialog & Sheet states
  var activeSongOptions by remember { mutableStateOf<Song?>(null) }
  var isCreatePlaylistVisible by remember { mutableStateOf(false) }
  var isCastDialogVisible by remember { mutableStateOf(false) }
  var isNotificationsVisible by remember { mutableStateOf(false) }
  var isQualityDialogVisible by remember { mutableStateOf(false) }

  // See all screen state
  var seeAllData by remember { mutableStateOf<Pair<String, List<Song>>?>(null) }

  // Handle back button presses
  BackHandler(enabled = isQueueVisible || isFullPlayerVisible || seeAllData != null) {
    if (isQueueVisible) {
      isQueueVisible = false
    } else if (isFullPlayerVisible) {
      isFullPlayerVisible = false
    } else if (seeAllData != null) {
      seeAllData = null
    }
  }

  MyApplicationTheme(
    themeMode = themeMode,
    accent = accentColor
  ) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = MaterialTheme.colorScheme.background,
      bottomBar = {
        // Only show bottom nav if full player / queue are not expanded
        if (!isFullPlayerVisible && !isQueueVisible) {
          Column {
            // Persistent Mini Player (shows if song is loaded)
            if (playbackState.currentSong != null) {
              VybeMiniPlayer(
                playbackState = playbackState,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onQueueClick = { isQueueVisible = true },
                onClick = { isFullPlayerVisible = true }
              )
            }

            VybeBottomNav(
              currentTab = currentTab,
              onTabSelected = {
                currentTab = it
                seeAllData = null
              }
            )
          }
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        // Main Screen Tabs
        if (seeAllData != null) {
          SeeAllSongsScreen(
            title = seeAllData!!.first,
            songs = seeAllData!!.second,
            viewModel = viewModel,
            onBack = { seeAllData = null },
            onSongOptionsClick = { activeSongOptions = it }
          )
        } else {
          when (currentTab) {
            VybeTab.HOME -> {
              HomeScreen(
                viewModel = viewModel,
                onSongOptionsClick = { activeSongOptions = it },
                onNotificationClick = { isNotificationsVisible = true },
                onCastClick = { isCastDialogVisible = true },
                onSeeAllClick = { title, songs -> seeAllData = Pair(title, songs) }
              )
            }
            VybeTab.SEARCH -> {
              SearchScreen(
                viewModel = viewModel,
                onSongOptionsClick = { activeSongOptions = it },
                onNotificationClick = { isNotificationsVisible = true },
                onCastClick = { isCastDialogVisible = true }
              )
            }
            VybeTab.LIBRARY -> {
              LibraryScreen(
                viewModel = viewModel,
                onCreatePlaylistClick = { isCreatePlaylistVisible = true },
                onSongOptionsClick = { activeSongOptions = it },
                onNotificationClick = { isNotificationsVisible = true },
                onCastClick = { isCastDialogVisible = true }
              )
            }
            VybeTab.SETTINGS -> {
              SettingsScreen(
                viewModel = viewModel,
                onNotificationClick = { isNotificationsVisible = true },
                onCastClick = { isCastDialogVisible = true },
                onQualitySelectorClick = { isQualityDialogVisible = true }
              )
            }
          }
        }

        // Full Player Modal Screen
        AnimatedVisibility(
          visible = isFullPlayerVisible,
          enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
          exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
          modifier = Modifier.fillMaxSize()
        ) {
          FullPlayerScreen(
            viewModel = viewModel,
            onCollapse = { isFullPlayerVisible = false },
            onQueueClick = { isQueueVisible = true },
            onCastClick = { isCastDialogVisible = true },
            onSongOptionsClick = { activeSongOptions = it },
            onQualityClick = { isQualityDialogVisible = true }
          )
        }

        // Queue Screen Modal
        AnimatedVisibility(
          visible = isQueueVisible,
          enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
          exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
          modifier = Modifier.fillMaxSize()
        ) {
          QueueScreen(
            viewModel = viewModel,
            onBack = { isQueueVisible = false },
            onSongOptionsClick = { activeSongOptions = it }
          )
        }
      }

      // Dialogs & Bottom Sheets
      if (activeSongOptions != null) {
        val song = activeSongOptions!!
        val isFav = favorites.any { it.id == song.id }
        SongOptionsBottomSheet(
          song = song,
          isFavorite = isFav,
          playlists = userPlaylists,
          onDismiss = { activeSongOptions = null },
          onPlayNext = { viewModel.playNext(song) },
          onAddToQueue = { viewModel.addToQueue(song) },
          onToggleFavorite = { viewModel.toggleFavorite(song) },
          onAddToPlaylist = { playlistId -> viewModel.addSongToPlaylist(playlistId, song) }
        )
      }

      if (isCreatePlaylistVisible) {
        CreatePlaylistDialog(
          onDismiss = { isCreatePlaylistVisible = false },
          onCreate = { title, desc ->
            viewModel.createPlaylist(title, desc)
            isCreatePlaylistVisible = false
          }
        )
      }

      if (isCastDialogVisible) {
        CastDevicesDialog(
          onDismiss = { isCastDialogVisible = false }
        )
      }

      if (isNotificationsVisible) {
        NotificationsDialog(
          onDismiss = { isNotificationsVisible = false }
        )
      }

      if (isQualityDialogVisible) {
        QualitySelectorDialog(
          currentQuality = streamingQuality,
          onQualitySelected = { viewModel.setStreamingQuality(it) },
          onDismiss = { isQualityDialogVisible = false }
        )
      }
    }
  }
}

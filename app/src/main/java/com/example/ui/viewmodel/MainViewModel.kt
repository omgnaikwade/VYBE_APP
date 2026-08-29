package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Artist
import com.example.data.model.AudioQuality
import com.example.data.model.EqualizerPreset
import com.example.data.model.EqualizerState
import com.example.data.model.MusicCategory
import com.example.data.model.PlaybackState
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.model.VybeAccent
import com.example.data.model.VybeThemeMode
import com.example.data.repository.MusicRepository
import com.example.data.repository.RealMusicRepository
import com.example.data.repository.SettingsRepository
import com.example.service.player.AudioPlayerManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val musicRepository: MusicRepository = RealMusicRepository(application, viewModelScope)
  private val settingsRepository: SettingsRepository = SettingsRepository(application)
  private val playerManager: AudioPlayerManager = AudioPlayerManager(application, viewModelScope)

  val discoverSongs: StateFlow<List<Song>> = musicRepository.getDiscoverSongs()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val biggestHits: StateFlow<List<Song>> = musicRepository.getBiggestHits()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val danceHits: StateFlow<List<Song>> = musicRepository.getDanceHits()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val trendingPlaylists: StateFlow<List<Playlist>> = musicRepository.getTrendingPlaylists()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val categories: StateFlow<List<MusicCategory>> = musicRepository.getCategories()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val topArtists: StateFlow<List<Artist>> = musicRepository.getTopArtists()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val userPlaylists: StateFlow<List<Playlist>> = musicRepository.getUserPlaylists()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val favoriteSongs: StateFlow<List<Song>> = musicRepository.getFavoriteSongs()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val historySongs: StateFlow<List<Song>> = musicRepository.getListeningHistory()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  @OptIn(ExperimentalCoroutinesApi::class)
  val searchResults: StateFlow<List<Song>> = _searchQuery
    .flatMapLatest { query -> musicRepository.search(query) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val recentSearches: StateFlow<List<String>> = musicRepository.getRecentSearches()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val playbackState: StateFlow<PlaybackState> = playerManager.playbackState

  val backendServerUrl: StateFlow<String> = settingsRepository.backendServerUrl
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.api.MusicBackendApi.DEFAULT_BASE_URL)

  val themeMode: StateFlow<VybeThemeMode> = settingsRepository.themeMode
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VybeThemeMode.DARK)

  val accentColor: StateFlow<VybeAccent> = settingsRepository.accentColor
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VybeAccent.PINK)

  val streamingQuality: StateFlow<AudioQuality> = settingsRepository.streamingQuality
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioQuality.HIGH)

  val downloadQuality: StateFlow<AudioQuality> = settingsRepository.downloadQuality
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioQuality.HIGH)

  val downloadOnlyWifi: StateFlow<Boolean> = settingsRepository.downloadOnlyWifi
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  val equalizerState: StateFlow<EqualizerState> = settingsRepository.equalizerState
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EqualizerState())

  val equalizerPresets: List<EqualizerPreset> = settingsRepository.equalizerPresets

  init {
    viewModelScope.launch {
      discoverSongs.collect { songs ->
        if (songs.isNotEmpty() && playbackState.value.currentSong == null) {
          playerManager.initializeWithSong(
            song = songs.first(),
            playlistName = "Trending Hits",
            initialQueue = songs
          )
        }
      }
    }
  }

  fun playSong(song: Song, playlistName: String = "Discover", queue: List<Song> = emptyList()) {
    val targetQueue = if (queue.isNotEmpty()) queue else listOf(song)
    playerManager.playSong(song, playlistName, targetQueue)
    viewModelScope.launch {
      musicRepository.addToHistory(song)
    }
  }

  fun togglePlayPause() {
    playerManager.togglePlayPause()
  }

  fun seekTo(positionSeconds: Int) {
    playerManager.seekTo(positionSeconds)
  }

  fun next() {
    playerManager.next()
    playbackState.value.currentSong?.let { song ->
      viewModelScope.launch { musicRepository.addToHistory(song) }
    }
  }

  fun previous() {
    playerManager.previous()
  }

  fun toggleShuffle() {
    playerManager.toggleShuffle()
  }

  fun toggleRepeat() {
    playerManager.toggleRepeat()
  }

  fun toggleFavorite(song: Song) {
    viewModelScope.launch {
      musicRepository.toggleFavorite(song)
    }
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
    if (query.isNotBlank()) {
      viewModelScope.launch {
        musicRepository.addRecentSearch(query)
      }
    }
  }

  fun clearRecentSearches() {
    viewModelScope.launch {
      musicRepository.clearRecentSearches()
    }
  }

  fun createPlaylist(title: String, description: String = "") {
    viewModelScope.launch {
      musicRepository.createPlaylist(title, description)
    }
  }

  fun addToQueue(song: Song) {
    playerManager.addToQueue(song)
  }

  fun playNext(song: Song) {
    playerManager.playNextInQueue(song)
  }

  fun removeFromQueue(index: Int) {
    playerManager.removeFromQueue(index)
  }

  fun clearQueue() {
    playerManager.clearQueue()
  }

  fun reorderQueue(from: Int, to: Int) {
    playerManager.reorderQueue(from, to)
  }

  fun setThemeMode(mode: VybeThemeMode) {
    settingsRepository.setThemeMode(mode)
  }

  fun setBackendServerUrl(url: String) {
    settingsRepository.setBackendServerUrl(url)
  }

  fun setAccentColor(accent: VybeAccent) {
    settingsRepository.setAccentColor(accent)
  }

  fun setStreamingQuality(quality: AudioQuality) {
    settingsRepository.setStreamingQuality(quality)
    playerManager.setStreamingQuality(quality)
  }

  fun setDownloadQuality(quality: AudioQuality) {
    settingsRepository.setDownloadQuality(quality)
  }

  fun setDownloadOnlyWifi(enabled: Boolean) {
    settingsRepository.setDownloadOnlyWifi(enabled)
  }

  fun setEqualizerEnabled(enabled: Boolean) {
    settingsRepository.setEqualizerEnabled(enabled)
  }

  fun setEqualizerPreset(presetName: String) {
    settingsRepository.setEqualizerPreset(presetName)
  }

  fun setEqualizerBandGain(bandIndex: Int, gain: Float) {
    settingsRepository.setEqualizerBandGain(bandIndex, gain)
  }

  fun addSongToPlaylist(playlistId: String, song: Song) {
    viewModelScope.launch {
      musicRepository.addSongToPlaylist(playlistId, song)
    }
  }

  override fun onCleared() {
    super.onCleared()
    playerManager.release()
  }
}

package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.MusicBackendApi
import com.example.data.model.Artist
import com.example.data.model.MusicCategory
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.storage.LocalMusicStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class RealMusicRepository(
  private val context: Context,
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : MusicRepository {

  private val musicBackendApi = MusicBackendApi()
  private val storage = LocalMusicStorage(context)

  private val defaultArtists = listOf(
    Artist("artist_arijit", "Arijit Singh", "", "", "Popular Artist"),
    Artist("artist_shreya", "Shreya Ghoshal", "", "", "Popular Artist"),
    Artist("artist_diljit", "Diljit Dosanjh", "", "", "Popular Artist"),
    Artist("artist_anirudh", "Anirudh Ravichander", "", "", "Popular Artist"),
    Artist("artist_pritam", "Pritam", "", "", "Popular Artist"),
    Artist("artist_sid", "Sid Sriram", "", "", "Popular Artist")
  )

  private val _discoverSongs = MutableStateFlow<List<Song>>(storage.loadDiscoverCache())
  private val _biggestHits = MutableStateFlow<List<Song>>(storage.loadBiggestHitsCache())
  private val _danceHits = MutableStateFlow<List<Song>>(storage.loadDanceHitsCache())
  private val _trendingPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
  private val _topArtists = MutableStateFlow<List<Artist>>(defaultArtists)

  private val _userPlaylists = MutableStateFlow<List<Playlist>>(storage.loadPlaylists())
  private val _favoriteSongs = MutableStateFlow<List<Song>>(storage.loadFavorites())
  private val _listeningHistory = MutableStateFlow<List<Song>>(storage.loadHistory())
  private val _recentSearches = MutableStateFlow<List<String>>(storage.loadRecentSearches())

  init {
    updatePlaylistsAndArtists()
    loadHomeData()
  }

  private fun loadHomeData() {
    // 1. Fetch Discover Songs concurrently
    scope.launch {
      try {
        val songs = musicBackendApi.searchMusic(
          query = "Top Trending Hits",
          limit = 16
        )
        if (songs.isNotEmpty()) {
          val marked = markFavorites(songs)
          _discoverSongs.value = marked
          storage.saveDiscoverCache(marked)
          updatePlaylistsAndArtists()
        }
      } catch (e: Exception) {
        Log.e("RealMusicRepo", "Error loading discover songs from backend", e)
      }
    }

    // 2. Fetch India's Biggest Hits concurrently
    scope.launch {
      try {
        val songs = musicBackendApi.searchMusic(
          query = "Top Bollywood Hits",
          limit = 12
        )
        if (songs.isNotEmpty()) {
          val marked = markFavorites(songs)
          _biggestHits.value = marked
          storage.saveBiggestHitsCache(marked)
          updatePlaylistsAndArtists()
        }
      } catch (e: Exception) {
        Log.e("RealMusicRepo", "Error loading biggest hits from backend", e)
      }
    }

    // 3. Fetch Dance Hits concurrently
    scope.launch {
      try {
        val songs = musicBackendApi.searchMusic(
          query = "Dance Party EDM",
          limit = 12
        )
        if (songs.isNotEmpty()) {
          val marked = markFavorites(songs)
          _danceHits.value = marked
          storage.saveDanceHitsCache(marked)
          updatePlaylistsAndArtists()
        }
      } catch (e: Exception) {
        Log.e("RealMusicRepo", "Error loading dance hits from backend", e)
      }
    }
  }

  @Synchronized
  private fun updatePlaylistsAndArtists() {
    val discoverList = _discoverSongs.value
    val hitsList = _biggestHits.value
    val danceList = _danceHits.value

    val playlists = mutableListOf<Playlist>()
    if (discoverList.isNotEmpty()) {
      playlists.add(
        Playlist(
          id = "pl_trending_now",
          title = "VYBE Top 50",
          description = "The hottest global and trending chartbusters updated daily.",
          coverUrl = discoverList.firstOrNull()?.artworkUrl ?: "",
          songCount = discoverList.size,
          songs = discoverList,
          gradientStart = 0xFF8B5CF6,
          gradientEnd = 0xFFFF2D75
        )
      )
    }
    if (hitsList.isNotEmpty()) {
      playlists.add(
        Playlist(
          id = "pl_bollywood_butter",
          title = "Bollywood Butter",
          description = "Pure Bollywood magic featuring top chart hits and golden melodies.",
          coverUrl = hitsList.firstOrNull()?.artworkUrl ?: "",
          songCount = hitsList.size,
          songs = hitsList,
          gradientStart = 0xFFFF2D75,
          gradientEnd = 0xFFE11D48
        )
      )
    }
    if (danceList.isNotEmpty()) {
      playlists.add(
        Playlist(
          id = "pl_club_dance",
          title = "Club Hyperdrive",
          description = "High octane EDM, club rhythms and heart-pumping beats.",
          coverUrl = danceList.firstOrNull()?.artworkUrl ?: "",
          songCount = danceList.size,
          songs = danceList,
          gradientStart = 0xFF06B6D4,
          gradientEnd = 0xFF3B82F6
        )
      )
    }
    _trendingPlaylists.value = playlists

    // Extract dynamic top artists from loaded real songs
    val allSongs = discoverList + hitsList + danceList
    val artistMap = mutableMapOf<String, String>()
    allSongs.forEach { song ->
      val mainArtist = song.artist.split(",", "&", "feat.", "ft.").firstOrNull()?.trim() ?: song.artist
      if (mainArtist.isNotBlank() && !artistMap.containsKey(mainArtist) && song.artworkUrl.isNotBlank()) {
        artistMap[mainArtist] = song.artworkUrl
      }
    }
    val dynamicArtists = artistMap.entries.take(8).map { (name, artUrl) ->
      Artist(
        id = "artist_${Math.abs(name.hashCode())}",
        name = name,
        avatarUrl = artUrl,
        monthlyListeners = "",
        genre = "Popular Artist"
      )
    }
    if (dynamicArtists.isNotEmpty()) {
      _topArtists.value = dynamicArtists
    }
  }

  private fun markFavorites(songs: List<Song>): List<Song> {
    val favIds = _favoriteSongs.value.map { it.id }.toSet()
    return songs.map { it.copy(isLiked = favIds.contains(it.id)) }
  }

  override fun getDiscoverSongs(): Flow<List<Song>> = _discoverSongs.asStateFlow()

  override fun getBiggestHits(): Flow<List<Song>> = _biggestHits.asStateFlow()

  override fun getDanceHits(): Flow<List<Song>> = _danceHits.asStateFlow()

  override fun getTrendingPlaylists(): Flow<List<Playlist>> = _trendingPlaylists.asStateFlow()

  override fun getCategories(): Flow<List<MusicCategory>> = flow {
    emit(
      listOf(
        MusicCategory("cat_bollywood", "Bollywood", 0xFFFF2D75, 0xFFE11D48, "heart"),
        MusicCategory("cat_punjabi", "Punjabi", 0xFFF97316, 0xFFEA580C, "headphones"),
        MusicCategory("cat_pop", "Pop", 0xFFA855F7, 0xFF7C3AED, "headphones"),
        MusicCategory("cat_dance", "Dance & EDM", 0xFF06B6D4, 0xFF0284C7, "disco"),
        MusicCategory("cat_chill", "Chill & Lo-Fi", 0xFF10B981, 0xFF059669, "headphones"),
        MusicCategory("cat_rock", "Rock & Indie", 0xFFEC4899, 0xFFDB2777, "headphones"),
        MusicCategory("cat_devotional", "Devotional", 0xFFF59E0B, 0xFFD97706, "heart"),
        MusicCategory("cat_hiphop", "Hip-Hop", 0xFF3B82F6, 0xFF2563EB, "headphones")
      )
    )
  }

  override fun getTopArtists(): Flow<List<Artist>> = _topArtists.asStateFlow()

  override fun search(query: String): Flow<List<Song>> = flow {
    val clean = query.trim()
    if (clean.isBlank()) {
      emit(emptyList())
      return@flow
    }
    try {
      val results = musicBackendApi.searchMusic(
        query = clean,
        limit = 25
      )
      emit(markFavorites(results))
    } catch (e: Exception) {
      Log.e("RealMusicRepo", "Search failed for $query", e)
      emit(emptyList())
    }
  }.flowOn(Dispatchers.IO)

  override fun getRecentSearches(): Flow<List<String>> = _recentSearches.asStateFlow()

  override suspend fun addRecentSearch(query: String) {
    val clean = query.trim()
    if (clean.isBlank()) return
    val current = _recentSearches.value.toMutableList()
    current.remove(clean)
    current.add(0, clean)
    _recentSearches.value = current
    storage.saveRecentSearches(current)
  }

  override suspend fun clearRecentSearches() {
    _recentSearches.value = emptyList()
    storage.saveRecentSearches(emptyList())
  }

  override fun getUserPlaylists(): Flow<List<Playlist>> = _userPlaylists.asStateFlow()

  override suspend fun createPlaylist(title: String, description: String) {
    val cleanTitle = title.trim()
    if (cleanTitle.isBlank()) return
    val newPlaylist = Playlist(
      id = "user_pl_${System.currentTimeMillis()}",
      title = cleanTitle,
      description = description.trim(),
      coverUrl = "",
      songCount = 0,
      songs = emptyList(),
      isUserCreated = true,
      gradientStart = 0xFF3B82F6,
      gradientEnd = 0xFF8B5CF6
    )
    val updated = listOf(newPlaylist) + _userPlaylists.value
    _userPlaylists.value = updated
    storage.savePlaylists(updated)
  }

  override fun getFavoriteSongs(): Flow<List<Song>> = _favoriteSongs.asStateFlow()

  override suspend fun toggleFavorite(song: Song): Boolean {
    val currentFavs = _favoriteSongs.value.toMutableList()
    val existingIndex = currentFavs.indexOfFirst { it.id == song.id }
    val isNowLiked: Boolean

    if (existingIndex >= 0) {
      currentFavs.removeAt(existingIndex)
      isNowLiked = false
    } else {
      currentFavs.add(0, song.copy(isLiked = true))
      isNowLiked = true
    }

    _favoriteSongs.value = currentFavs
    storage.saveFavorites(currentFavs)

    _discoverSongs.value = markFavorites(_discoverSongs.value)
    _biggestHits.value = markFavorites(_biggestHits.value)
    _danceHits.value = markFavorites(_danceHits.value)

    return isNowLiked
  }

  override fun getListeningHistory(): Flow<List<Song>> = _listeningHistory.asStateFlow()

  override suspend fun addToHistory(song: Song) {
    val currentHistory = _listeningHistory.value.toMutableList()
    currentHistory.removeAll { it.id == song.id }
    currentHistory.add(0, song)
    val trimmed = currentHistory.take(50)
    _listeningHistory.value = trimmed
    storage.saveHistory(trimmed)
  }

  override suspend fun addSongToPlaylist(playlistId: String, song: Song) {
    val currentPlaylists = _userPlaylists.value.toMutableList()
    val index = currentPlaylists.indexOfFirst { it.id == playlistId }
    if (index >= 0) {
      val pl = currentPlaylists[index]
      val songs = pl.songs.toMutableList()
      if (songs.none { it.id == song.id }) {
        songs.add(song)
        val updatedPl = pl.copy(
          songs = songs,
          songCount = songs.size,
          coverUrl = if (pl.coverUrl.isBlank()) song.artworkUrl else pl.coverUrl
        )
        currentPlaylists[index] = updatedPl
        _userPlaylists.value = currentPlaylists
        storage.savePlaylists(currentPlaylists)
      }
    }
  }

  override suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
    val currentPlaylists = _userPlaylists.value.toMutableList()
    val index = currentPlaylists.indexOfFirst { it.id == playlistId }
    if (index >= 0) {
      val pl = currentPlaylists[index]
      val songs = pl.songs.filterNot { it.id == songId }
      val updatedPl = pl.copy(
        songs = songs,
        songCount = songs.size
      )
      currentPlaylists[index] = updatedPl
      _userPlaylists.value = currentPlaylists
      storage.savePlaylists(currentPlaylists)
    }
  }
}

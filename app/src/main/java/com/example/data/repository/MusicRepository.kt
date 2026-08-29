package com.example.data.repository

import com.example.data.model.Artist
import com.example.data.model.MusicCategory
import com.example.data.model.Playlist
import com.example.data.model.Song
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
  fun getDiscoverSongs(): Flow<List<Song>>
  fun getBiggestHits(): Flow<List<Song>>
  fun getDanceHits(): Flow<List<Song>>
  fun getTrendingPlaylists(): Flow<List<Playlist>>
  fun getCategories(): Flow<List<MusicCategory>>
  fun getTopArtists(): Flow<List<Artist>>
  fun search(query: String): Flow<List<Song>>
  fun getRecentSearches(): Flow<List<String>>
  suspend fun addRecentSearch(query: String)
  suspend fun clearRecentSearches()
  fun getUserPlaylists(): Flow<List<Playlist>>
  suspend fun createPlaylist(title: String, description: String = "")
  fun getFavoriteSongs(): Flow<List<Song>>
  suspend fun toggleFavorite(song: Song): Boolean
  fun getListeningHistory(): Flow<List<Song>>
  suspend fun addToHistory(song: Song)
  suspend fun removeSongFromPlaylist(playlistId: String, songId: String)
  suspend fun addSongToPlaylist(playlistId: String, song: Song)
}

package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.Playlist
import com.example.data.model.Song
import org.json.JSONArray
import org.json.JSONObject

class LocalMusicStorage(context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("vybe_local_music_storage", Context.MODE_PRIVATE)

  private val KEY_FAVORITES = "key_favorite_songs"
  private val KEY_PLAYLISTS = "key_user_playlists"
  private val KEY_HISTORY = "key_listening_history"
  private val KEY_RECENT_SEARCHES = "key_recent_searches"
  private val KEY_DISCOVER_CACHE = "key_discover_cache"
  private val KEY_BIGGEST_HITS_CACHE = "key_biggest_hits_cache"
  private val KEY_DANCE_HITS_CACHE = "key_dance_hits_cache"

  fun loadDiscoverCache(): List<Song> {
    val jsonString = prefs.getString(KEY_DISCOVER_CACHE, null) ?: return emptyList()
    return deserializeSongs(jsonString)
  }

  fun saveDiscoverCache(songs: List<Song>) {
    val jsonString = serializeSongs(songs)
    prefs.edit().putString(KEY_DISCOVER_CACHE, jsonString).apply()
  }

  fun loadBiggestHitsCache(): List<Song> {
    val jsonString = prefs.getString(KEY_BIGGEST_HITS_CACHE, null) ?: return emptyList()
    return deserializeSongs(jsonString)
  }

  fun saveBiggestHitsCache(songs: List<Song>) {
    val jsonString = serializeSongs(songs)
    prefs.edit().putString(KEY_BIGGEST_HITS_CACHE, jsonString).apply()
  }

  fun loadDanceHitsCache(): List<Song> {
    val jsonString = prefs.getString(KEY_DANCE_HITS_CACHE, null) ?: return emptyList()
    return deserializeSongs(jsonString)
  }

  fun saveDanceHitsCache(songs: List<Song>) {
    val jsonString = serializeSongs(songs)
    prefs.edit().putString(KEY_DANCE_HITS_CACHE, jsonString).apply()
  }

  fun loadFavorites(): List<Song> {
    val jsonString = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
    return deserializeSongs(jsonString)
  }

  fun saveFavorites(songs: List<Song>) {
    val jsonString = serializeSongs(songs)
    prefs.edit().putString(KEY_FAVORITES, jsonString).apply()
  }

  fun loadPlaylists(): List<Playlist> {
    val jsonString = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
    return deserializePlaylists(jsonString)
  }

  fun savePlaylists(playlists: List<Playlist>) {
    val jsonString = serializePlaylists(playlists)
    prefs.edit().putString(KEY_PLAYLISTS, jsonString).apply()
  }

  fun loadHistory(): List<Song> {
    val jsonString = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
    return deserializeSongs(jsonString)
  }

  fun saveHistory(history: List<Song>) {
    val jsonString = serializeSongs(history.take(50)) // keep latest 50
    prefs.edit().putString(KEY_HISTORY, jsonString).apply()
  }

  fun loadRecentSearches(): List<String> {
    val jsonString = prefs.getString(KEY_RECENT_SEARCHES, null) ?: return emptyList()
    return try {
      val array = JSONArray(jsonString)
      val list = mutableListOf<String>()
      for (i in 0 until array.length()) {
        list.add(array.getString(i))
      }
      list
    } catch (e: Exception) {
      emptyList()
    }
  }

  fun saveRecentSearches(searches: List<String>) {
    val array = JSONArray()
    searches.take(20).forEach { array.put(it) }
    prefs.edit().putString(KEY_RECENT_SEARCHES, array.toString()).apply()
  }

  private fun serializeSongs(songs: List<Song>): String {
    val array = JSONArray()
    for (song in songs) {
      val obj = JSONObject().apply {
        put("id", song.id)
        put("title", song.title)
        put("artist", song.artist)
        put("album", song.album)
        put("durationSeconds", song.durationSeconds)
        put("artworkUrl", song.artworkUrl)
        put("audioStreamUrl", song.audioStreamUrl)
        put("genre", song.genre)
        put("isLiked", song.isLiked)
        put("playCount", song.playCount)
        put("releaseYear", song.releaseYear)
        put("primaryColor", song.primaryColor)
        put("secondaryColor", song.secondaryColor)
      }
      array.put(obj)
    }
    return array.toString()
  }

  private fun deserializeSongs(jsonString: String): List<Song> {
    val list = mutableListOf<Song>()
    try {
      val array = JSONArray(jsonString)
      for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: continue
        list.add(
          Song(
            id = obj.optString("id", ""),
            title = obj.optString("title", ""),
            artist = obj.optString("artist", ""),
            album = obj.optString("album", ""),
            durationSeconds = obj.optInt("durationSeconds", 240),
            artworkUrl = obj.optString("artworkUrl", ""),
            audioStreamUrl = obj.optString("audioStreamUrl", ""),
            genre = obj.optString("genre", "Pop"),
            isLiked = obj.optBoolean("isLiked", false),
            playCount = obj.optString("playCount", ""),
            releaseYear = obj.optString("releaseYear", "2024"),
            primaryColor = obj.optLong("primaryColor", 0xFFFF2D75),
            secondaryColor = obj.optLong("secondaryColor", 0xFF8B5CF6)
          )
        )
      }
    } catch (e: Exception) {
      Log.e("LocalMusicStorage", "Error deserializing songs", e)
    }
    return list
  }

  private fun serializePlaylists(playlists: List<Playlist>): String {
    val array = JSONArray()
    for (p in playlists) {
      val obj = JSONObject().apply {
        put("id", p.id)
        put("title", p.title)
        put("description", p.description)
        put("coverUrl", p.coverUrl)
        put("songCount", p.songCount)
        put("isUserCreated", p.isUserCreated)
        put("isCommunity", p.isCommunity)
        put("gradientStart", p.gradientStart)
        put("gradientEnd", p.gradientEnd)
        put("songsJson", serializeSongs(p.songs))
      }
      array.put(obj)
    }
    return array.toString()
  }

  private fun deserializePlaylists(jsonString: String): List<Playlist> {
    val list = mutableListOf<Playlist>()
    try {
      val array = JSONArray(jsonString)
      for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: continue
        val songsJson = obj.optString("songsJson", "[]")
        val songs = deserializeSongs(songsJson)
        list.add(
          Playlist(
            id = obj.optString("id", ""),
            title = obj.optString("title", ""),
            description = obj.optString("description", ""),
            coverUrl = obj.optString("coverUrl", ""),
            songCount = if (songs.isNotEmpty()) songs.size else obj.optInt("songCount", 0),
            songs = songs,
            isUserCreated = obj.optBoolean("isUserCreated", true),
            isCommunity = obj.optBoolean("isCommunity", false),
            gradientStart = obj.optLong("gradientStart", 0xFF1E1B29),
            gradientEnd = obj.optLong("gradientEnd", 0xFF121118)
          )
        )
      }
    } catch (e: Exception) {
      Log.e("LocalMusicStorage", "Error deserializing playlists", e)
    }
    return list
  }
}

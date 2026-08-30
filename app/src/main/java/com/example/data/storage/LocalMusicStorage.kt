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
    context.applicationContext.getSharedPreferences(
      "vybe_local_music_storage",
      Context.MODE_PRIVATE
    )

  private companion object {
    const val KEY_FAVORITES = "key_favorite_songs"
    const val KEY_PLAYLISTS = "key_user_playlists"
    const val KEY_HISTORY = "key_listening_history"
    const val KEY_RECENT_SEARCHES = "key_recent_searches"

    const val KEY_DISCOVER_CACHE = "key_discover_cache"
    const val KEY_BIGGEST_HITS_CACHE = "key_biggest_hits_cache"
    const val KEY_DANCE_HITS_CACHE = "key_dance_hits_cache"

    const val MAX_HISTORY = 50
    const val MAX_RECENT_SEARCHES = 20
  }

  // ------------------------------------------------------------
  // DISCOVER CACHE
  // ------------------------------------------------------------

  fun loadDiscoverCache(): List<Song> {
    val jsonString = prefs.getString(KEY_DISCOVER_CACHE, null)
      ?: return emptyList()

    return deserializeSongs(jsonString)
  }

  fun saveDiscoverCache(songs: List<Song>) {
    prefs.edit()
      .putString(KEY_DISCOVER_CACHE, serializeSongs(songs))
      .apply()
  }

  // ------------------------------------------------------------
  // BIGGEST HITS CACHE
  // ------------------------------------------------------------

  fun loadBiggestHitsCache(): List<Song> {
    val jsonString = prefs.getString(KEY_BIGGEST_HITS_CACHE, null)
      ?: return emptyList()

    return deserializeSongs(jsonString)
  }

  fun saveBiggestHitsCache(songs: List<Song>) {
    prefs.edit()
      .putString(KEY_BIGGEST_HITS_CACHE, serializeSongs(songs))
      .apply()
  }

  // ------------------------------------------------------------
  // DANCE HITS CACHE
  // ------------------------------------------------------------

  fun loadDanceHitsCache(): List<Song> {
    val jsonString = prefs.getString(KEY_DANCE_HITS_CACHE, null)
      ?: return emptyList()

    return deserializeSongs(jsonString)
  }

  fun saveDanceHitsCache(songs: List<Song>) {
    prefs.edit()
      .putString(KEY_DANCE_HITS_CACHE, serializeSongs(songs))
      .apply()
  }

  // ------------------------------------------------------------
  // FAVORITES
  // ------------------------------------------------------------

  fun loadFavorites(): List<Song> {
    val jsonString = prefs.getString(KEY_FAVORITES, null)
      ?: return emptyList()

    return deserializeSongs(jsonString)
  }

  fun saveFavorites(songs: List<Song>) {
    prefs.edit()
      .putString(KEY_FAVORITES, serializeSongs(songs))
      .apply()
  }

  // ------------------------------------------------------------
  // PLAYLISTS
  // ------------------------------------------------------------

  fun loadPlaylists(): List<Playlist> {
    val jsonString = prefs.getString(KEY_PLAYLISTS, null)
      ?: return emptyList()

    return deserializePlaylists(jsonString)
  }

  fun savePlaylists(playlists: List<Playlist>) {
    prefs.edit()
      .putString(KEY_PLAYLISTS, serializePlaylists(playlists))
      .apply()
  }

  // ------------------------------------------------------------
  // LISTENING HISTORY
  // ------------------------------------------------------------

  fun loadHistory(): List<Song> {
    val jsonString = prefs.getString(KEY_HISTORY, null)
      ?: return emptyList()

    return deserializeSongs(jsonString)
  }

  fun saveHistory(history: List<Song>) {
    val limitedHistory = history.take(MAX_HISTORY)

    prefs.edit()
      .putString(KEY_HISTORY, serializeSongs(limitedHistory))
      .apply()
  }

  // ------------------------------------------------------------
  // RECENT SEARCHES
  // ------------------------------------------------------------

  fun loadRecentSearches(): List<String> {
    val jsonString = prefs.getString(KEY_RECENT_SEARCHES, null)
      ?: return emptyList()

    return try {
      val array = JSONArray(jsonString)
      val result = mutableListOf<String>()

      for (i in 0 until array.length()) {
        val value = array.optString(i, "")

        if (value.isNotBlank()) {
          result.add(value)
        }
      }

      result
    } catch (e: Exception) {
      Log.e(
        "LocalMusicStorage",
        "Error loading recent searches",
        e
      )

      emptyList()
    }
  }

  fun saveRecentSearches(searches: List<String>) {
    val array = JSONArray()

    searches
      .filter { it.isNotBlank() }
      .distinct()
      .take(MAX_RECENT_SEARCHES)
      .forEach { search ->
        array.put(search)
      }

    prefs.edit()
      .putString(KEY_RECENT_SEARCHES, array.toString())
      .apply()
  }

  // ------------------------------------------------------------
  // SONG SERIALIZATION
  // ------------------------------------------------------------

  private fun serializeSongs(songs: List<Song>): String {
    val array = JSONArray()

    songs.forEach { song ->
      val obj = JSONObject()

      obj.put("id", song.id)
      obj.put("title", song.title)
      obj.put("artist", song.artist)
      obj.put("album", song.album)
      obj.put("durationSeconds", song.durationSeconds)
      obj.put("artworkUrl", song.artworkUrl)
      obj.put("audioStreamUrl", song.audioStreamUrl)
      obj.put("genre", song.genre)
      obj.put("isLiked", song.isLiked)
      obj.put("playCount", song.playCount)
      obj.put("releaseYear", song.releaseYear)
      obj.put("primaryColor", song.primaryColor)
      obj.put("secondaryColor", song.secondaryColor)

      array.put(obj)
    }

    return array.toString()
  }

  private fun deserializeSongs(jsonString: String): List<Song> {
    val songs = mutableListOf<Song>()

    try {
      val array = JSONArray(jsonString)

      for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: continue

        songs.add(
          Song(
            id = obj.optString("id", ""),
            title = obj.optString("title", ""),
            artist = obj.optString("artist", ""),
            album = obj.optString("album", ""),
            durationSeconds = obj.optInt(
              "durationSeconds",
              240
            ),
            artworkUrl = obj.optString(
              "artworkUrl",
              ""
            ),
            audioStreamUrl = obj.optString(
              "audioStreamUrl",
              ""
            ),
            genre = obj.optString(
              "genre",
              "Pop"
            ),
            isLiked = obj.optBoolean(
              "isLiked",
              false
            ),
            playCount = obj.optString(
              "playCount",
              ""
            ),
            releaseYear = obj.optString(
              "releaseYear",
              "2024"
            ),
            primaryColor = obj.optLong(
              "primaryColor",
              0xFFFF2D75L
            ),
            secondaryColor = obj.optLong(
              "secondaryColor",
              0xFF8B5CF6L
            )
          )
        )
      }
    } catch (e: Exception) {
      Log.e(
        "LocalMusicStorage",
        "Error deserializing songs",
        e
      )
    }

    return songs
  }

  // ------------------------------------------------------------
  // PLAYLIST SERIALIZATION
  // ------------------------------------------------------------

  private fun serializePlaylists(
    playlists: List<Playlist>
  ): String {
    val array = JSONArray()

    playlists.forEach { playlist ->
      val obj = JSONObject()

      obj.put("id", playlist.id)
      obj.put("title", playlist.title)
      obj.put("description", playlist.description)
      obj.put("coverUrl", playlist.coverUrl)
      obj.put("songCount", playlist.songCount)
      obj.put("isUserCreated", playlist.isUserCreated)
      obj.put("isCommunity", playlist.isCommunity)
      obj.put("gradientStart", playlist.gradientStart)
      obj.put("gradientEnd", playlist.gradientEnd)
      obj.put(
        "songsJson",
        serializeSongs(playlist.songs)
      )

      array.put(obj)
    }

    return array.toString()
  }

  // ------------------------------------------------------------
  // PLAYLIST DESERIALIZATION
  // ------------------------------------------------------------

  private fun deserializePlaylists(
    jsonString: String
  ): List<Playlist> {
    val playlists = mutableListOf<Playlist>()

    try {
      val array = JSONArray(jsonString)

      for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: continue

        val songsJson = obj.optString(
          "songsJson",
          "[]"
        )

        val songs = deserializeSongs(songsJson)

        val storedSongCount = obj.optInt(
          "songCount",
          0
        )

        val songCount =
          if (songs.isNotEmpty()) {
            songs.size
          } else {
            storedSongCount
          }

        playlists.add(
          Playlist(
            id = obj.optString(
              "id",
              ""
            ),
            title = obj.optString(
              "title",
              ""
            ),
            description = obj.optString(
              "description",
              ""
            ),
            coverUrl = obj.optString(
              "coverUrl",
              ""
            ),
            songCount = songCount,
            songs = songs,
            isUserCreated = obj.optBoolean(
              "isUserCreated",
              true
            ),
            isCommunity = obj.optBoolean(
              "isCommunity",
              false
            ),
            gradientStart = obj.optLong(
              "gradientStart",
              0xFF1E1B29L
            ),
            gradientEnd = obj.optLong(
              "gradientEnd",
              0xFF121118L
            )
          )
        )
      }
    } catch (e: Exception) {
      Log.e(
        "LocalMusicStorage",
        "Error deserializing playlists",
        e
      )
    }

    return playlists
  }
}

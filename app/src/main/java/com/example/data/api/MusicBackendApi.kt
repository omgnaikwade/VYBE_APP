package com.example.data.api

import android.util.Log
import com.example.data.model.Song
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Data contracts for the FastAPI Music Backend
 */
data class SearchResponse(
  val videoId: String,
  val title: String,
  val artist: String = "Unknown",
  val album: String? = null,
  val thumbnail: String = "",
  val duration: Int? = null
) {
  fun toSong(): Song {
    return Song(
      id = videoId,
      title = title.ifBlank { "Track $videoId" },
      artist = artist.ifBlank { "Unknown Artist" },
      album = album ?: "",
      durationSeconds = duration ?: 240,
      artworkUrl = thumbnail,
      audioStreamUrl = "", // Intentionally empty; resolved on-demand via /stream/{videoId}
      genre = "Music",
      isLiked = false
    )
  }
}

data class StreamResponse(
  val videoId: String,
  val title: String,
  val audioUrl: String
)

data class PlaylistResponse(
  val playlistName: String?,
  val songs: List<SearchResponse>
)

interface MusicBackendApiService {

  @GET("search")
  suspend fun searchSongs(
    @Query("query") query: String,
    @Query("limit") limit: Int = 20
  ): List<SearchResponse>

  @GET("stream/{video_id}")
  suspend fun getStream(
    @Path("video_id") videoId: String
  ): StreamResponse

  @GET("playlist/{playlist_id}")
  suspend fun getPlaylist(
    @Path("playlist_id") playlistId: String
  ): PlaylistResponse
}

/**
 * Client for interacting with the FastAPI ytmusicapi / yt_dlp backend.
 * Supports dynamic base URL configuration for emulator (10.0.2.2) and physical devices (LAN IP).
 */
class MusicBackendApi(
  initialBaseUrl: String = DEFAULT_BASE_URL
) {

  companion object {
    const val TAG = "MusicBackendApi"
    const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/"

    @Volatile
    private var globalBaseUrl: String = DEFAULT_BASE_URL

    fun getGlobalBaseUrl(): String = globalBaseUrl

    fun setGlobalBaseUrl(newUrl: String) {
      val sanitized = if (!newUrl.endsWith("/")) "$newUrl/" else newUrl
      globalBaseUrl = sanitized
      Log.i(TAG, "Updated global backend Base URL to: $globalBaseUrl")
    }
  }

  private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

  private val loggingInterceptor = HttpLoggingInterceptor { message ->
    Log.d(TAG, "[HTTP] $message")
  }.apply {
    level = HttpLoggingInterceptor.Level.BASIC
  }

  private val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(20, TimeUnit.SECONDS)
    .build()

  private fun createService(baseUrl: String): MusicBackendApiService {
    val sanitized = if (!baseUrl.endsWith("/")) "$baseUrl/" else baseUrl
    return Retrofit.Builder()
      .baseUrl(sanitized)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(MusicBackendApiService::class.java)
  }

  private fun getService(): MusicBackendApiService {
    return createService(getGlobalBaseUrl())
  }

  suspend fun searchMusic(query: String, limit: Int = 20): List<Song> = withContext(Dispatchers.IO) {
    val cleanQuery = query.trim()
    if (cleanQuery.isBlank()) return@withContext emptyList()

    val currentUrl = getGlobalBaseUrl()
    Log.i(TAG, "GET $currentUrl/search?query=$cleanQuery&limit=$limit")

    try {
      val service = getService()
      val responses = service.searchSongs(query = cleanQuery, limit = limit)
      Log.i(TAG, "Search success: received ${responses.size} songs for query '$cleanQuery'")
      responses.map { it.toSong() }
    } catch (e: Exception) {
      Log.e(TAG, "Search failed for query '$cleanQuery' on backend $currentUrl: ${e.message}", e)
      emptyList()
    }
  }

  suspend fun getStream(videoId: String): StreamResponse? = withContext(Dispatchers.IO) {
    val cleanId = videoId.trim()
    if (cleanId.isBlank()) return@withContext null

    val currentUrl = getGlobalBaseUrl()
    Log.i(TAG, "GET $currentUrl/stream/$cleanId (Resolving fresh audio stream)")

    try {
      val service = getService()
      val response = service.getStream(videoId = cleanId)
      Log.i(TAG, "Stream resolved successfully for videoId=$cleanId: title='${response.title}', url=${response.audioUrl.take(60)}...")
      response
    } catch (e: Exception) {
      Log.e(TAG, "Stream resolution failed for videoId=$cleanId on backend $currentUrl: ${e.message}", e)
      null
    }
  }

  suspend fun getPlaylist(playlistId: String): List<Song> = withContext(Dispatchers.IO) {
    val cleanId = playlistId.trim()
    if (cleanId.isBlank()) return@withContext emptyList()

    val currentUrl = getGlobalBaseUrl()
    Log.i(TAG, "GET $currentUrl/playlist/$cleanId")

    try {
      val service = getService()
      val response = service.getPlaylist(playlistId = cleanId)
      Log.i(TAG, "Playlist '${response.playlistName}' fetched with ${response.songs.size} tracks")
      response.songs.map { it.toSong() }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to get playlist $cleanId on backend $currentUrl: ${e.message}", e)
      emptyList()
    }
  }
}

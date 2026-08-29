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
            audioStreamUrl = "",
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

class MusicBackendApi(
    initialBaseUrl: String = DEFAULT_BASE_URL
) {

    companion object {

        private const val TAG = "MusicBackendApi"

        /*
         * ANDROID EMULATOR:
         * http://10.0.2.2:8000/
         *
         * PHYSICAL PHONE:
         * Replace YOUR_PC_IP with the computer's local IP.
         * Example:
         * http://192.168.1.5:8000/
         */
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/"

        @Volatile
        private var globalBaseUrl: String =
            sanitizeBaseUrl(initialBaseUrl)

        private fun sanitizeBaseUrl(url: String): String {
            val cleanUrl = url.trim()
            return if (cleanUrl.endsWith("/")) {
                cleanUrl
            } else {
                "$cleanUrl/"
            }
        }

        fun getGlobalBaseUrl(): String {
            return globalBaseUrl
        }

        fun setGlobalBaseUrl(newUrl: String) {
            val sanitized = sanitizeBaseUrl(newUrl)

            globalBaseUrl = sanitized

            Log.i(
                TAG,
                "Backend Base URL updated to: $globalBaseUrl"
            )
        }
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor =
        HttpLoggingInterceptor { message ->
            Log.d(TAG, "[HTTP] $message")
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

    private val okHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

    private fun createService(
        baseUrl: String
    ): MusicBackendApiService {

        val sanitizedUrl = sanitizeBaseUrl(baseUrl)

        return Retrofit.Builder()
            .baseUrl(sanitizedUrl)
            .client(okHttpClient)
            .addConverterFactory(
                MoshiConverterFactory.create(moshi)
            )
            .build()
            .create(MusicBackendApiService::class.java)
    }

    private fun getService(): MusicBackendApiService {
        return createService(getGlobalBaseUrl())
    }

    suspend fun searchMusic(
        query: String,
        limit: Int = 20
    ): List<Song> = withContext(Dispatchers.IO) {

        val cleanQuery = query.trim()

        if (cleanQuery.isBlank()) {
            return@withContext emptyList()
        }

        val currentUrl = getGlobalBaseUrl()

        Log.i(
            TAG,
            "GET $currentUrl/search?query=$cleanQuery&limit=$limit"
        )

        try {

            val service = getService()

            val responses = service.searchSongs(
                query = cleanQuery,
                limit = limit
            )

            Log.i(
                TAG,
                "Search successful: ${responses.size} songs"
            )

            responses.map { it.toSong() }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Search failed on backend $currentUrl: ${e.message}",
                e
            )

            emptyList()
        }
    }

    suspend fun getStream(
        videoId: String
    ): StreamResponse? = withContext(Dispatchers.IO) {

        val cleanId = videoId.trim()

        if (cleanId.isBlank()) {
            return@withContext null
        }

        val currentUrl = getGlobalBaseUrl()

        Log.i(
            TAG,
            "GET $currentUrl/stream/$cleanId"
        )

        try {

            val service = getService()

            val response = service.getStream(
                videoId = cleanId
            )

            if (response.audioUrl.isBlank()) {

                Log.e(
                    TAG,
                    "Backend returned empty audio URL for $cleanId"
                )

                return@withContext null
            }

            Log.i(
                TAG,
                "Stream resolved successfully: " +
                        "videoId=$cleanId, " +
                        "title='${response.title}'"
            )

            response

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Stream resolution failed for videoId=$cleanId " +
                        "on backend $currentUrl: ${e.message}",
                e
            )

            null
        }
    }

    suspend fun getPlaylist(
        playlistId: String
    ): List<Song> = withContext(Dispatchers.IO) {

        val cleanId = playlistId.trim()

        if (cleanId.isBlank()) {
            return@withContext emptyList()
        }

        val currentUrl = getGlobalBaseUrl()

        Log.i(
            TAG,
            "GET $currentUrl/playlist/$cleanId"
        )

        try {

            val service = getService()

            val response = service.getPlaylist(
                playlistId = cleanId
            )

            Log.i(
                TAG,
                "Playlist '${response.playlistName}' " +
                        "fetched with ${response.songs.size} tracks"
            )

            response.songs.map {
                it.toSong()
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Playlist request failed for $cleanId " +
                        "on backend $currentUrl: ${e.message}",
                e
            )

            emptyList()
        }
    }
}

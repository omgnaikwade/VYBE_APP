package com.example.service.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log

import com.example.data.api.MusicBackendApi
import com.example.data.model.AudioQuality
import com.example.data.model.PlaybackState
import com.example.data.model.Song
import com.example.service.MusicNotificationReceiver
import com.example.service.NotificationHelper
import com.example.service.PlayerHolder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioPlayerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {

    init {
        PlayerHolder.manager = this
    }

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressTickerJob: Job? = null
    private var playbackJob: Job? = null

    private val musicBackendApi = MusicBackendApi()

    private var currentSongRetryCount = 0
    private var currentPlayingSongId: String? = null

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .build()

    private val streamHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36",
        "Accept" to "*/*",
        "Accept-Encoding" to "identity;q=1, *;q=0"
    )

    fun initializeWithSong(
        song: Song,
        playlistName: String = "Discover",
        initialQueue: List<Song> = emptyList()
    ) {
        val q = if (initialQueue.isNotEmpty()) {
            initialQueue
        } else {
            listOf(song)
        }

        val index = q.indexOfFirst { it.id == song.id }
            .let { if (it >= 0) it else 0 }

        _playbackState.value = _playbackState.value.copy(
            currentSong = song,
            isPlaying = false,
            positionSeconds = 0,
            durationSeconds = song.durationSeconds,
            currentPlaylistName = playlistName,
            queue = q,
            currentQueueIndex = index,
            errorMessage = null
        )
    }

    fun playSong(
        song: Song,
        playlistName: String = "Discover",
        queue: List<Song> = emptyList()
    ) {
        logPipeline(
            "Request to play song: '${song.title}' by '${song.artist}' (videoId: ${song.id})"
        )

        val fullQueue = if (queue.isNotEmpty()) {
            queue
        } else {
            listOf(song)
        }

        val index

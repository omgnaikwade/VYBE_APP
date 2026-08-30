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

        val index = fullQueue.indexOfFirst { it.id == song.id }
            .let { if (it >= 0) it else 0 }

        if (currentPlayingSongId != song.id) {
            currentSongRetryCount = 0
            currentPlayingSongId = song.id
        }

        _playbackState.update {
            it.copy(
                currentSong = song,
                isPlaying = true,
                positionSeconds = 0,
                durationSeconds = song.durationSeconds,
                currentPlaylistName = playlistName,
                queue = fullQueue,
                currentQueueIndex = index,
                errorMessage = null
            )
        }

        createMusicNotification(song)

        startAudioStream(song, isRetry = false)
    }

    private fun createMusicNotification(song: Song) {

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val playPauseIntent = PendingIntent.getBroadcast(
            context,
            1001,
            Intent(
                context,
                MusicNotificationReceiver::class.java
            ).apply {
                action = NotificationHelper.ACTION_PLAY_PAUSE
            },
            flags
        )

        val previousIntent = PendingIntent.getBroadcast(
            context,
            1002,
            Intent(
                context,
                MusicNotificationReceiver::class.java
            ).apply {
                action = NotificationHelper.ACTION_PREVIOUS
            },
            flags
        )

        val nextIntent = PendingIntent.getBroadcast(
            context,
            1003,
            Intent(
                context,
                MusicNotificationReceiver::class.java
            ).apply {
                action = NotificationHelper.ACTION_NEXT
            },
            flags
        )

        NotificationHelper.showMusicNotification(
            context = context,
            song = song,
            isPlaying = true,
            playPauseIntent = playPauseIntent,
            previousIntent = previousIntent,
            nextIntent = nextIntent
        )
    }

    private fun startAudioStream(
        song: Song,
        isRetry: Boolean = false
    ) {
        playbackJob?.cancel()

        playbackJob = scope.launch(Dispatchers.Main) {

            stopPlayer()

            logPipeline("------------------------------------------")
            logPipeline(
                "STEP 1: videoId = ${song.id} | Title = '${song.title}'"
            )

            logPipeline(
                "STEP 2: Requesting GET /stream/${song.id} from FastAPI backend..."
            )

            val streamResponse = withContext(Dispatchers.IO) {
                try {
                    musicBackendApi.getStream(song.id)
                } catch (e: Exception) {
                    logPipeline(
                        "STEP 2 ERROR: ${e.message}"
                    )
                    null
                }
            }

            val audioUrl = streamResponse?.audioUrl?.trim() ?: ""

            if (audioUrl.isBlank()) {
                logPipeline(
                    "STEP 2 FAILED: No audio URL received for '${song.id}'"
                )

                handlePlaybackFailure(
                    song,
                    "Could not resolve audio stream for '${song.title}'"
                )

                return@launch
            }

            logPipeline(
                "STEP 3: Resolved audio stream URL = ${audioUrl.take(85)}..."
            )

            try {

                logPipeline(
                    "STEP 4: Configuring MediaPlayer..."
                )

                val player = MediaPlayer().apply {

                    setAudioAttributes(audioAttributes)

                    try {
                        setDataSource(
                            context,
                            Uri.parse(audioUrl),
                            streamHeaders
                        )
                    } catch (e: Exception) {

                        logPipeline(
                            "STEP 4 WARN: Header-based setDataSource failed: ${e.message}"
                        )

                        setDataSource(audioUrl)
                    }

                    setOnPreparedListener { mp ->

                        try {

                            mp.start()

                            val actualDuration =
                                if (mp.duration > 0) {
                                    mp.duration / 1000
                                } else {
                                    song.durationSeconds
                                }

                            logPipeline(
                                "STEP 6: PLAYING SUCCESSFUL! Duration = ${actualDuration}s"
                            )

                            _playbackState.update {
                                it.copy(
                                    isPlaying = true,
                                    durationSeconds = actualDuration,
                                    errorMessage = null
                                )
                            }

                            startProgressTicker()

                        } catch (e: Exception) {

                            logPipeline(
                                "STEP 6 ERROR: ${e.message}"
                            )

                            handlePlaybackFailure(
                                song,
                                e.message ?: "Player start failed"
                            )
                        }
                    }

                    setOnBufferingUpdateListener { _, _ ->
                    }

                    setOnCompletionListener {

                        logPipeline(
                            "Track completed: '${song.title}'"
                        )

                        onTrackCompletion()
                    }

                    setOnErrorListener { _, what, extra ->

                        logPipeline(
                            "MediaPlayer error: what=$what extra=$extra"
                        )

                        stopProgressTicker()

                        handlePlaybackFailure(
                            song,
                            "MediaPlayer error (what=$what, extra=$extra)"
                        )

                        true
                    }

                    logPipeline(
                        "STEP 5: Preparing audio stream..."
                    )

                    prepareAsync()
                }

                mediaPlayer = player

            } catch (e: Exception) {

                logPipeline(
                    "STEP 4 CRITICAL: ${e.message}"
                )

                handlePlaybackFailure(
                    song,
                    e.message ?: "Player initialization error"
                )
            }
        }
    }

    private fun handlePlaybackFailure(
        song: Song,
        reason: String
    ) {

        if (currentSongRetryCount < 1) {

            currentSongRetryCount++

            logPipeline(
                "RETRY: Requesting fresh stream for ${song.id}"
            )

            startAudioStream(
                song,
                isRetry = true
            )

        } else {

            logPipeline(
                "CRITICAL FAILURE: ${song.title} | $reason"
            )

            _playbackState.update {
                it.copy(
                    isPlaying = false,
                    errorMessage = "Playback failed: $reason"
                )
            }

            stopPlayer()
        }
    }

    private fun onTrackCompletion() {

        val state = _playbackState.value

        if (state.isRepeat) {

            try {
                mediaPlayer?.seekTo(0)
                mediaPlayer?.start()

                _playbackState.update {
                    it.copy(
                        positionSeconds = 0,
                        isPlaying = true
                    )
                }

                startProgressTicker()

            } catch (e: Exception) {
                logPipeline(
                    "Repeat playback error: ${e.message}"
                )
            }

        } else if (state.queue.isNotEmpty()) {

            next()

        } else {

            _playbackState.update {
                it.copy(
                    isPlaying = false,
                    positionSeconds = 0
                )
            }

            stopProgressTicker()
        }
    }

    fun togglePlayPause() {

        val mp = mediaPlayer
        val currentSong =
            _playbackState.value.currentSong ?:

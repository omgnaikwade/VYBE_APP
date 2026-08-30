package com.example.service.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.session.MediaSession
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
        initMediaSession()
    }

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> =
        _playbackState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressTickerJob: Job? = null
    private var playbackJob: Job? = null
    private var mediaSession: MediaSession? = null

    private val musicBackendApi = MusicBackendApi()

    private var currentSongRetryCount = 0
    private var currentPlayingSongId: String? = null

    private val audioAttributes =
        AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

    private val streamHeaders = mapOf(
        "User-Agent" to
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36",
        "Accept" to "*/*",
        "Accept-Encoding" to "identity;q=1, *;q=0"
    )

    private fun initMediaSession() {
        if (mediaSession == null) {
            mediaSession = MediaSession(context, "VybeMusicSession").apply {
                setFlags(
                    MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                            MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
                )
                isActive = true
            }
        }
    }

    fun initializeWithSong(
        song: Song,
        playlistName: String = "Discover",
        initialQueue: List<Song> = emptyList()
    ) {
        val queue = if (initialQueue.isNotEmpty()) {
            initialQueue
        } else {
            listOf(song)
        }

        val index = queue.indexOfFirst {
            it.id == song.id
        }.let {
            if (it >= 0) it else 0
        }

        _playbackState.value =
            _playbackState.value.copy(
                currentSong = song,
                isPlaying = false,
                positionSeconds = 0,
                durationSeconds = song.durationSeconds,
                currentPlaylistName = playlistName,
                queue = queue,
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
            "Request to play '${song.title}' by '${song.artist}'"
        )

        val fullQueue = if (queue.isNotEmpty()) {
            queue
        } else {
            listOf(song)
        }

        val index = fullQueue.indexOfFirst {
            it.id == song.id
        }.let {
            if (it >= 0) it else 0
        }

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

        showNotification(song, true)

        startAudioStream(song)
    }

    private fun showNotification(song: Song, isPlaying: Boolean) {

        val flags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

        val playPauseIntent =
            PendingIntent.getBroadcast(
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

        val previousIntent =
            PendingIntent.getBroadcast(
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

        val nextIntent =
            PendingIntent.getBroadcast(
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
            isPlaying = isPlaying,
            mediaSessionToken = mediaSession?.sessionToken,
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

        playbackJob =
            scope.launch(Dispatchers.Main) {

                stopPlayer()

                logPipeline(
                    "---------------------------------------------"
                )

                logPipeline(
                    "STEP 1: videoId=${song.id}"
                )

                logPipeline(
                    "STEP 2: Requesting /stream/${song.id}"
                )

                val streamResponse =
                    withContext(Dispatchers.IO) {
                        try {
                            musicBackendApi.getStream(song.id)
                        } catch (e: Exception) {
                            logPipeline(
                                "STREAM ERROR: ${e.message}"
                            )
                            null
                        }
                    }

                val audioUrl =
                    streamResponse?.audioUrl?.trim() ?: ""

                if (audioUrl.isBlank()) {

                    logPipeline(
                        "No audio URL received"
                    )

                    handlePlaybackFailure(
                        song,
                        "Could not resolve audio stream"
                    )

                    return@launch
                }

                logPipeline(
                    "STEP 3: Audio URL received"
                )

                try {

                    val player =
                        MediaPlayer().apply {

                            setAudioAttributes(
                                audioAttributes
                            )

                            try {

                                setDataSource(
                                    context,
                                    Uri.parse(audioUrl),
                                    streamHeaders
                                )

                            } catch (e: Exception) {

                                logPipeline(
                                    "Header DataSource failed, using direct URL"
                                )

                                setDataSource(audioUrl)
                            }

                            setOnPreparedListener { mp ->

                                try {

                                    mp.start()

                                    val duration =
                                        if (mp.duration > 0) {
                                            mp.duration / 1000
                                        } else {
                                            song.durationSeconds
                                        }

                                    _playbackState.update {

                                        it.copy(
                                            isPlaying = true,
                                            durationSeconds = duration,
                                            errorMessage = null
                                        )
                                    }

                                    logPipeline(
                                        "PLAYBACK SUCCESS"
                                    )

                                    startProgressTicker()

                                } catch (e: Exception) {

                                    handlePlaybackFailure(
                                        song,
                                        e.message
                                            ?: "Player start failed"
                                    )
                                }
                            }

                            setOnCompletionListener {

                                logPipeline(
                                    "Track completed: ${song.title}"
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
                                    "MediaPlayer error: $what/$extra"
                                )

                                true
                            }

                            prepareAsync()
                        }

                    mediaPlayer = player

                } catch (e: Exception) {

                    logPipeline(
                        "MediaPlayer initialization failed: ${e.message}"
                    )

                    handlePlaybackFailure(
                        song,
                        e.message
                            ?: "Player initialization error"
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
                "Retrying stream for ${song.id}"
            )

            startAudioStream(
                song,
                isRetry = true
            )

        } else {

            logPipeline(
                "Playback failed after retry: $reason"
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

    fun togglePlayPause() {

        val currentSong =
            _playbackState.value.currentSong
                ?: return

        val player = mediaPlayer

        if (player == null) {

            startAudioStream(currentSong)
            return
        }

        try {

            if (player.isPlaying) {

                player.pause()

                _playbackState.update {
                    it.copy(isPlaying = false)
                }

                stopProgressTicker()

                showNotification(currentSong, false)

            } else {

                player.start()

                _playbackState.update {
                    it.copy(isPlaying = true)
                }

                startProgressTicker()

                showNotification(currentSong, true)
            }

        } catch (e: Exception) {

            logPipeline(
                "Toggle error: ${e.message}"
            )

            startAudioStream(currentSong)
        }
    }

    fun seekTo(positionSeconds: Int) {

        val duration =
            _playbackState.value.durationSeconds

        val position =
            positionSeconds.coerceIn(
                0,
                duration
            )

        _playbackState.update {
            it.copy(
                positionSeconds = position
            )
        }

        try {

            mediaPlayer?.seekTo(
                position * 1000
            )

        } catch (e: Exception) {

            logPipeline(
                "Seek error: ${e.message}"
            )
        }
    }

    fun next() {

        val state = _playbackState.value

        if (state.queue.isEmpty()) {
            return
        }

        val nextIndex =
            if (state.isShuffle) {

                val possible =
                    state.queue.indices.filter {
                        it != state.currentQueueIndex
                    }

                if (possible.isNotEmpty()) {
                    possible.random()
                } else {
                    0
                }

            } else {

                (state.currentQueueIndex + 1) %
                        state.queue.size
            }

        val nextSong =
            state.queue[nextIndex]

        currentSongRetryCount = 0
        currentPlayingSongId = nextSong.id

        _playbackState.update {

            it.copy(
                currentSong = nextSong,
                currentQueueIndex = nextIndex,
                positionSeconds = 0,
                durationSeconds =
                    nextSong.durationSeconds,
                isPlaying = true,
                errorMessage = null
            )
        }

        showNotification(nextSong, true)

        startAudioStream(nextSong)
    }

    fun previous() {

        val state = _playbackState.value

        if (state.queue.isEmpty()) {
            return
        }

        if (state.positionSeconds > 3) {

            seekTo(0)
            return
        }

        val previousIndex =
            if (state.currentQueueIndex > 0) {
                state.currentQueueIndex - 1
            } else {
                state.queue.size - 1
            }

        val previousSong =
            state.queue[previousIndex]

        currentSongRetryCount = 0
        currentPlayingSongId = previousSong.id

        _playbackState.update {

            it.copy(
                currentSong = previousSong,
                currentQueueIndex = previousIndex,
                positionSeconds = 0,
                durationSeconds =
                    previousSong.durationSeconds,
                isPlaying = true,
                errorMessage = null
            )
        }

        showNotification(previousSong, true)

        startAudioStream(previousSong)
    }

    private fun onTrackCompletion() {

        val state = _playbackState.value

        if (state.isRepeat) {

            mediaPlayer?.seekTo(0)
            mediaPlayer?.start()

            _playbackState.update {
                it.copy(
                    positionSeconds = 0,
                    isPlaying = true
                )
            }

            startProgressTicker()

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

    fun toggleShuffle() {

        _playbackState.update {
            it.copy(
                isShuffle = !it.isShuffle
            )
        }
    }

    fun toggleRepeat() {

        _playbackState.update {
            it.copy(
                isRepeat = !it.isRepeat
            )
        }
    }

    fun setStreamingQuality(
        quality: AudioQuality
    ) {

        _playbackState.update {
            it.copy(
                streamingQuality = quality
            )
        }
    }

    fun addToQueue(song: Song) {

        val queue =
            _playbackState.value.queue.toMutableList()

        queue.add(song)

        _playbackState.update {
            it.copy(queue = queue)
        }
    }

    fun playNextInQueue(song: Song) {

        val queue =
            _playbackState.value.queue.toMutableList()

        val currentIndex =
            _playbackState.value.currentQueueIndex

        val insertIndex =
            (currentIndex + 1)
                .coerceIn(0, queue.size)

        queue.add(
            insertIndex,
            song
        )

        _playbackState.update {
            it.copy(queue = queue)
        }
    }

    fun removeFromQueue(index: Int) {

        val queue =
            _playbackState.value.queue.toMutableList()

        if (index !in queue.indices) {
            return
        }

        queue.removeAt(index)

        val oldIndex =
            _playbackState.value.currentQueueIndex

        val newIndex =
            when {
                queue.isEmpty() -> 0

                index < oldIndex ->
                    oldIndex - 1

                index == oldIndex ->
                    oldIndex.coerceAtMost(
                        queue.lastIndex
                    )

                else ->
                    oldIndex.coerceAtMost(
                        queue.lastIndex
                    )
            }

        _playbackState.update {

            it.copy(
                queue = queue,
                currentQueueIndex = newIndex
            )
        }
    }

    fun clearQueue() {

        val currentSong =
            _playbackState.value.currentSong

        val queue =
            if (currentSong != null) {
                listOf(currentSong)
            } else {
                emptyList()
            }

        _playbackState.update {

            it.copy(
                queue = queue,
                currentQueueIndex = 0
            )
        }
    }

    fun reorderQueue(
        from: Int,
        to: Int
    ) {

        val queue =
            _playbackState.value.queue.toMutableList()

        if (from !in queue.indices ||
            to !in queue.indices
        ) {
            return
        }

        val item =
            queue.removeAt(from)

        queue.add(to, item)

        var currentIndex =
            _playbackState.value.currentQueueIndex

        if (from == currentIndex) {

            currentIndex = to

        } else if (
            from < currentIndex &&
            to >= currentIndex
        ) {

            currentIndex--

        } else if (
            from > currentIndex &&
            to <= currentIndex
        ) {

            currentIndex++
        }

        _playbackState.update {

            it.copy(
                queue = queue,
                currentQueueIndex =
                    currentIndex.coerceIn(
                    0,
                        (queue.size - 1)
                            .coerceAtLeast(0)
                    )
            )
        }
    }

    private fun startProgressTicker() {

        progressTickerJob?.cancel()

        progressTickerJob =
            scope.launch(Dispatchers.Main) {

                while (
                    isActive &&
                    _playbackState.value.isPlaying
                ) {

                    val player =
                        mediaPlayer

                    if (player != null) {

                        try {

                            if (player.isPlaying) {

                                val position =
                                    player.currentPosition / 1000

                                val duration =
                                    if (player.duration > 0) {
                                        player.duration / 1000
                                    } else {
                                        _playbackState.value.durationSeconds
                                    }

                                _playbackState.update {
                                    it.copy(
                                        positionSeconds = position,
                                        durationSeconds = duration
                                    )
                                }
                            }
                        } catch (_: Exception) {
                            // Ignore temporary player state errors
                        }
                    }

                    delay(250)
                }
            }
    }

    private fun stopProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = null
    }

    private fun stopPlayer() {
        stopProgressTicker()

        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }

        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }

        mediaPlayer = null
    }

    fun release() {
        playbackJob?.cancel()
        playbackJob = null

        stopPlayer()

        if (PlayerHolder.manager === this) {
            PlayerHolder.manager = null
        }
    }

    private fun logPipeline(message: String) {
        try {
            Log.i(
                "AudioPlayerPipeline",
                message
            )
        } catch (_: Throwable) {
            println(
                "[AudioPlayerPipeline] $message"
            )
        }
    }
}

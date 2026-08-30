package com.example.service.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.example.service.NotificationHelper
import android.app.PendingIntent
import android.content.Intent
import com.example.MainActivity
import com.example.data.api.MusicBackendApi
import com.example.data.model.AudioQuality
import com.example.data.model.PlaybackState
import com.example.data.model.Song
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

  private val _playbackState = MutableStateFlow(PlaybackState())
  val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

  private var mediaPlayer: MediaPlayer? = null
  private var progressTickerJob: Job? = null
  private var playbackJob: Job? = null
  private val musicBackendApi = MusicBackendApi()

  // Track retry count per song to avoid infinite retry loops
  private var currentSongRetryCount = 0
  private var currentPlayingSongId: String? = null

  private val audioAttributes = AudioAttributes.Builder()
    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .build()

  private val streamHeaders = mapOf(
    "User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
    "Accept" to "*/*",
    "Accept-Encoding" to "identity;q=1, *;q=0"
  )

  fun initializeWithSong(song: Song, playlistName: String = "Discover", initialQueue: List<Song> = emptyList()) {
    val q = if (initialQueue.isNotEmpty()) initialQueue else listOf(song)
    val index = q.indexOfFirst { it.id == song.id }.let { if (it >= 0) it else 0 }
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

  fun playSong(song: Song, playlistName: String = "Discover", queue: List<Song> = emptyList()) {
    logPipeline("Request to play song: '${song.title}' by '${song.artist}' (videoId: ${song.id})")
    val fullQueue = if (queue.isNotEmpty()) queue else listOf(song)
    val index = fullQueue.indexOfFirst { it.id == song.id }.let { if (it >= 0) it else 0 }

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

     NotificationHelper.showMusicNotification(
    context = context,
    song = song,
    isPlaying = true
)
    startAudioStream(song, isRetry = false)
  }

  private fun startAudioStream(song: Song, isRetry: Boolean = false) {
    playbackJob?.cancel()
    playbackJob = scope.launch(Dispatchers.Main) {
      stopPlayer()

      logPipeline("--------------------------------------------------")
      logPipeline("STEP 1: videoId = ${song.id} | Title = '${song.title}'")

      // STEP 2: Resolve fresh audio stream URL from FastAPI backend: GET /stream/{videoId}
      logPipeline("STEP 2: Requesting GET /stream/${song.id} from FastAPI backend...")
      val streamResponse = withContext(Dispatchers.IO) {
        try {
          musicBackendApi.getStream(song.id)
        } catch (e: Exception) {
          logPipeline("STEP 2 ERROR: Exception calling /stream/${song.id}: ${e.message}")
          null
        }
      }

      val audioUrl = streamResponse?.audioUrl?.trim() ?: ""

      if (audioUrl.isBlank()) {
        logPipeline("STEP 2 FAILED: No audio URL received for videoId '${song.id}' from backend")
        handlePlaybackFailure(song, "Could not resolve audio stream for '${song.title}' (Check backend connection)")
        return@launch
      }

      logPipeline("STEP 3: Resolved audio stream URL = ${audioUrl.take(85)}...")

      // Step 4: Initialize Android MediaPlayer
      try {
        logPipeline("STEP 4: Configuring MediaPlayer dataSource...")
        val player = MediaPlayer().apply {
          setAudioAttributes(audioAttributes)
          try {
            setDataSource(context, Uri.parse(audioUrl), streamHeaders)
          } catch (e: Exception) {
            logPipeline("STEP 4 WARN: Header-based setDataSource failed (${e.message}), falling back to direct URL")
            setDataSource(audioUrl)
          }

          logPipeline("STEP 5: Buffering / Preparing stream asynchronously...")
          setOnPreparedListener { mp ->
            try {
              mp.start()
              val actualDurSec = if (mp.duration > 0) mp.duration / 1000 else song.durationSeconds
              logPipeline("STEP 6: PLAYING SUCCESSFUL! Duration: ${actualDurSec}s (${mp.duration}ms)")
              _playbackState.update {
                it.copy(
                  isPlaying = true,
                  durationSeconds = actualDurSec,
                  errorMessage = null
                )
              }
              startProgressTicker()
            } catch (e: Exception) {
              logPipeline("STEP 6 ERROR: Failed starting playback after prepare: ${e.message}")
              handlePlaybackFailure(song, e.message ?: "Player start failed")
            }
          }

          setOnBufferingUpdateListener { _, _ -> }

          setOnCompletionListener {
            logPipeline("Track playback completed for '${song.title}'")
            onTrackCompletion()
          }

          setOnErrorListener { _, what, extra ->
            logPipeline("STEP ERROR: MediaPlayer error callback received: what=$what, extra=$extra")
            stopProgressTicker()
            handlePlaybackFailure(song, "MediaPlayer error (what=$what, extra=$extra)")
            true
          }

          prepareAsync()
        }
        mediaPlayer = player
      } catch (e: Exception) {
        logPipeline("STEP 4 CRITICAL: Failed to initialize MediaPlayer: ${e.message}")
        handlePlaybackFailure(song, e.message ?: "Player initialization error")
      }
    }
  }

  private fun handlePlaybackFailure(song: Song, reason: String) {
    if (currentSongRetryCount < 1) {
      currentSongRetryCount++
      logPipeline("RETRY: Requesting fresh /stream/${song.id} from backend (attempt $currentSongRetryCount)...")
      startAudioStream(song, isRetry = true)
    } else {
      logPipeline("CRITICAL FAILURE: Playback failed for '${song.title}' after retry: $reason")
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
      mediaPlayer?.seekTo(0)
      mediaPlayer?.start()
      _playbackState.update { it.copy(positionSeconds = 0, isPlaying = true) }
    } else if (state.queue.isNotEmpty()) {
      next()
    } else {
      _playbackState.update { it.copy(isPlaying = false, positionSeconds = 0) }
      stopProgressTicker()
    }
  }

  fun togglePlayPause() {
    val mp = mediaPlayer
    val currentSong = _playbackState.value.currentSong ?: return

    if (mp == null) {
      logPipeline("togglePlayPause: Player was idle, starting stream for '${currentSong.title}'")
      startAudioStream(currentSong)
      return
    }

    try {
      if (mp.isPlaying) {
        mp.pause()
        logPipeline("Playback paused")
        _playbackState.update { it.copy(isPlaying = false) }
        stopProgressTicker()
      } else {
        mp.start()
        logPipeline("Playback resumed")
        _playbackState.update { it.copy(isPlaying = true) }
        startProgressTicker()
      }
    } catch (e: Exception) {
      logPipeline("Error toggling play/pause: ${e.message}, restarting stream")
      startAudioStream(currentSong)
    }
  }

  fun seekTo(positionSeconds: Int) {
    val duration = _playbackState.value.durationSeconds
    val clamped = positionSeconds.coerceIn(0, duration)
    _playbackState.update { it.copy(positionSeconds = clamped) }

    try {
      logPipeline("Seeking to position: ${clamped}s")
      mediaPlayer?.seekTo(clamped * 1000)
    } catch (e: Exception) {
      logPipeline("Error seeking: ${e.message}")
    }
  }

  fun next() {
    val state = _playbackState.value
    if (state.queue.isEmpty()) return

    val nextIndex = if (state.isShuffle) {
      val indices = state.queue.indices.filter { it != state.currentQueueIndex }
      if (indices.isNotEmpty()) indices.random() else 0
    } else {
      (state.currentQueueIndex + 1) % state.queue.size
    }

    val nextSong = state.queue[nextIndex]
    currentSongRetryCount = 0
    currentPlayingSongId = nextSong.id
    _playbackState.update {
      it.copy(
        currentSong = nextSong,
        currentQueueIndex = nextIndex,
        positionSeconds = 0,
        durationSeconds = nextSong.durationSeconds,
        isPlaying = true,
        errorMessage = null
      )
    }
    startAudioStream(nextSong)
  }

  fun previous() {
    val state = _playbackState.value
    if (state.queue.isEmpty()) return

    if (state.positionSeconds > 3) {
      seekTo(0)
      return
    }

    val prevIndex = if (state.currentQueueIndex > 0) state.currentQueueIndex - 1 else state.queue.size - 1
    val prevSong = state.queue[prevIndex]
    currentSongRetryCount = 0
    currentPlayingSongId = prevSong.id
    _playbackState.update {
      it.copy(
        currentSong = prevSong,
        currentQueueIndex = prevIndex,
        positionSeconds = 0,
        durationSeconds = prevSong.durationSeconds,
        isPlaying = true,
        errorMessage = null
      )
    }
    startAudioStream(prevSong)
  }

  fun toggleShuffle() {
    _playbackState.update { it.copy(isShuffle = !it.isShuffle) }
  }

  fun toggleRepeat() {
    _playbackState.update { it.copy(isRepeat = !it.isRepeat) }
  }

  fun setStreamingQuality(quality: AudioQuality) {
    _playbackState.update { it.copy(streamingQuality = quality) }
  }

  fun addToQueue(song: Song) {
    val currentQueue = _playbackState.value.queue.toMutableList()
    currentQueue.add(song)
    _playbackState.update { it.copy(queue = currentQueue) }
  }

  fun playNextInQueue(song: Song) {
    val currentQueue = _playbackState.value.queue.toMutableList()
    val nextIndex = (_playbackState.value.currentQueueIndex + 1).coerceAtMost(currentQueue.size)
    currentQueue.add(nextIndex, song)
    _playbackState.update { it.copy(queue = currentQueue) }
  }

  fun removeFromQueue(index: Int) {
    val currentQueue = _playbackState.value.queue.toMutableList()
    if (index in currentQueue.indices) {
      currentQueue.removeAt(index)
      val newIndex = if (index < _playbackState.value.currentQueueIndex) {
        _playbackState.value.currentQueueIndex - 1
      } else {
        _playbackState.value.currentQueueIndex
      }
      _playbackState.update {
        it.copy(
          queue = currentQueue,
          currentQueueIndex = newIndex.coerceIn(0, (currentQueue.size - 1).coerceAtLeast(0))
        )
      }
    }
  }

  fun clearQueue() {
    val currentSong = _playbackState.value.currentSong
    val newQueue = if (currentSong != null) listOf(currentSong) else emptyList()
    _playbackState.update {
      it.copy(
        queue = newQueue,
        currentQueueIndex = 0
      )
    }
  }

  fun reorderQueue(from: Int, to: Int) {
    val currentQueue = _playbackState.value.queue.toMutableList()
    if (from in currentQueue.indices && to in currentQueue.indices) {
      val item = currentQueue.removeAt(from)
      currentQueue.add(to, item)
      _playbackState.update { it.copy(queue = currentQueue) }
    }
  }

  private fun startProgressTicker() {
    progressTickerJob?.cancel()
    progressTickerJob = scope.launch(Dispatchers.Main) {
      while (isActive && _playbackState.value.isPlaying) {
        val mp = mediaPlayer
        if (mp != null) {
          try {
            if (mp.isPlaying) {
              val currentPosSec = mp.currentPosition / 1000
              val durSec = if (mp.duration > 0) mp.duration / 1000 else _playbackState.value.durationSeconds
              _playbackState.update {
                it.copy(
                  positionSeconds = currentPosSec,
                  durationSeconds = durSec
                )
              }
            }
          } catch (e: Exception) {
            // Transient state
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
      mediaPlayer?.release()
    } catch (e: Exception) {
      // ignore
    }
    mediaPlayer = null
  }

  fun release() {
    playbackJob?.cancel()
    stopPlayer()
  }

  private fun logPipeline(message: String) {
    try {
      Log.i("AudioPlayerPipeline", message)
    } catch (e: Throwable) {
      println("[AudioPlayerPipeline] $message")
    }
  }
}

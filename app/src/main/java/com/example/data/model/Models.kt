package com.example.data.model

import androidx.compose.ui.graphics.Color

data class Song(
  val id: String,
  val title: String,
  val artist: String,
  val album: String = "",
  val durationSeconds: Int = 240,
  val artworkUrl: String = "",
  val audioStreamUrl: String = "",
  val genre: String = "Bollywood / Pop",
  val isLiked: Boolean = false,
  val playCount: String = "1.2M",
  val releaseYear: String = "2024",
  val primaryColor: Long = 0xFFFF2D75,
  val secondaryColor: Long = 0xFF8B5CF6
) {
  val formattedDuration: String
    get() {
      val minutes = durationSeconds / 60
      val seconds = durationSeconds % 60
      return String.format("%d:%02d", minutes, seconds)
    }
}

data class Artist(
  val id: String,
  val name: String,
  val avatarUrl: String = "",
  val monthlyListeners: String = "15.4M monthly listeners",
  val genre: String = "Bollywood / Pop",
  val topSongs: List<String> = emptyList()
)

data class Playlist(
  val id: String,
  val title: String,
  val description: String = "",
  val coverUrl: String = "",
  val songCount: Int = 0,
  val songs: List<Song> = emptyList(),
  val isUserCreated: Boolean = false,
  val isCommunity: Boolean = false,
  val gradientStart: Long = 0xFF1E1B29,
  val gradientEnd: Long = 0xFF121118
)

data class MusicCategory(
  val id: String,
  val title: String,
  val colorStart: Long,
  val colorEnd: Long,
  val iconName: String = "headphones"
)

enum class VybeThemeMode(val title: String) {
  DARK("Dark"),
  LIGHT("Light"),
  SYSTEM("System")
}

enum class VybeAccent(val displayName: String, val primary: Long, val secondary: Long) {
  PINK("Pink", 0xFFFF2D75, 0xFFE11D48),
  PURPLE("Purple", 0xFFA855F7, 0xFF7C3AED),
  GREEN("Green", 0xFF10B981, 0xFF059669),
  BLUE("Blue", 0xFF3B82F6, 0xFF2563EB),
  ORANGE("Orange", 0xFFF97316, 0xFFEA580C)
}

enum class AudioQuality(val title: String, val bitrate: String, val description: String) {
  LOW("Low", "96 kbps", "Data Saver"),
  NORMAL("Normal", "128 kbps", "Standard Audio"),
  HIGH("High", "320 kbps", "Crisp High Bitrate"),
  LOSSLESS("Lossless", "24-bit / 192kHz", "Studio Master FLAC")
}

data class EqualizerPreset(
  val name: String,
  val gains: List<Float> // 7 bands: 60Hz, 150Hz, 400Hz, 1KHz, 2.4KHz, 6KHz, 15KHz (-12dB to +12dB)
)

data class EqualizerState(
  val isEnabled: Boolean = true,
  val currentPreset: String = "Normal",
  val bandGains: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f) // -12 to +12
)

data class PlaybackState(
  val currentSong: Song? = null,
  val isPlaying: Boolean = false,
  val positionSeconds: Int = 0,
  val durationSeconds: Int = 240,
  val isShuffle: Boolean = false,
  val isRepeat: Boolean = false,
  val currentPlaylistName: String = "Chill Vibes",
  val queue: List<Song> = emptyList(),
  val currentQueueIndex: Int = 0,
  val streamingQuality: AudioQuality = AudioQuality.HIGH,
  val downloadQuality: AudioQuality = AudioQuality.HIGH,
  val downloadOnlyWifi: Boolean = true,
  val errorMessage: String? = null
) {
  val progress: Float
    get() = if (durationSeconds > 0) positionSeconds.toFloat() / durationSeconds.toFloat() else 0f

  val formattedPosition: String
    get() {
      val minutes = positionSeconds / 60
      val seconds = positionSeconds % 60
      return String.format("%d:%02d", minutes, seconds)
    }

  val formattedDuration: String
    get() {
      val minutes = durationSeconds / 60
      val seconds = durationSeconds % 60
      return String.format("%d:%02d", minutes, seconds)
    }
}

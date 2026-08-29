package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.api.MusicBackendApi
import com.example.data.model.AudioQuality
import com.example.data.model.EqualizerPreset
import com.example.data.model.EqualizerState
import com.example.data.model.VybeAccent
import com.example.data.model.VybeThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("vybe_settings", Context.MODE_PRIVATE)

  private val _backendServerUrl = MutableStateFlow(
    prefs.getString("backend_server_url", MusicBackendApi.DEFAULT_BASE_URL) ?: MusicBackendApi.DEFAULT_BASE_URL
  )
  val backendServerUrl: Flow<String> = _backendServerUrl.asStateFlow()

  init {
    MusicBackendApi.setGlobalBaseUrl(_backendServerUrl.value)
  }

  private val _themeMode = MutableStateFlow(
    VybeThemeMode.valueOf(
      prefs.getString("theme_mode", VybeThemeMode.DARK.name) ?: VybeThemeMode.DARK.name
    )
  )
  val themeMode: Flow<VybeThemeMode> = _themeMode.asStateFlow()

  private val _accentColor = MutableStateFlow(
    VybeAccent.valueOf(
      prefs.getString("accent_color", VybeAccent.PINK.name) ?: VybeAccent.PINK.name
    )
  )
  val accentColor: Flow<VybeAccent> = _accentColor.asStateFlow()

  private val _streamingQuality = MutableStateFlow(
    AudioQuality.valueOf(
      prefs.getString("streaming_quality", AudioQuality.HIGH.name) ?: AudioQuality.HIGH.name
    )
  )
  val streamingQuality: Flow<AudioQuality> = _streamingQuality.asStateFlow()

  private val _downloadQuality = MutableStateFlow(
    AudioQuality.valueOf(
      prefs.getString("download_quality", AudioQuality.HIGH.name) ?: AudioQuality.HIGH.name
    )
  )
  val downloadQuality: Flow<AudioQuality> = _downloadQuality.asStateFlow()

  private val _downloadOnlyWifi = MutableStateFlow(
    prefs.getBoolean("download_only_wifi", true)
  )
  val downloadOnlyWifi: Flow<Boolean> = _downloadOnlyWifi.asStateFlow()

  private val _equalizerState = MutableStateFlow(
    EqualizerState(
      isEnabled = prefs.getBoolean("eq_enabled", true),
      currentPreset = prefs.getString("eq_preset", "Normal") ?: "Normal",
      bandGains = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)
    )
  )
  val equalizerState: Flow<EqualizerState> = _equalizerState.asStateFlow()

  val equalizerPresets = listOf(
    EqualizerPreset("Normal", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    EqualizerPreset("Bass Boost", listOf(6f, 5f, 3f, 0f, -1f, -2f, -2f)),
    EqualizerPreset("Pop", listOf(-1.5f, 1f, 3f, 4.5f, 3.5f, 1.5f, -1f)),
    EqualizerPreset("Rock", listOf(5f, 3f, -1f, -2f, 1f, 4f, 6f)),
    EqualizerPreset("Jazz", listOf(3f, 2f, 0f, 2f, -1f, 2f, 4f)),
    EqualizerPreset("Classic", listOf(4f, 3f, 1f, 0f, 1f, 3f, 5f))
  )

  fun setBackendServerUrl(url: String) {
    val sanitized = url.trim()
    if (sanitized.isNotBlank()) {
      prefs.edit().putString("backend_server_url", sanitized).apply()
      _backendServerUrl.value = sanitized
      MusicBackendApi.setGlobalBaseUrl(sanitized)
    }
  }

  fun setThemeMode(mode: VybeThemeMode) {
    prefs.edit().putString("theme_mode", mode.name).apply()
    _themeMode.value = mode
  }

  fun setAccentColor(accent: VybeAccent) {
    prefs.edit().putString("accent_color", accent.name).apply()
    _accentColor.value = accent
  }

  fun setStreamingQuality(quality: AudioQuality) {
    prefs.edit().putString("streaming_quality", quality.name).apply()
    _streamingQuality.value = quality
  }

  fun setDownloadQuality(quality: AudioQuality) {
    prefs.edit().putString("download_quality", quality.name).apply()
    _downloadQuality.value = quality
  }

  fun setDownloadOnlyWifi(enabled: Boolean) {
    prefs.edit().putBoolean("download_only_wifi", enabled).apply()
    _downloadOnlyWifi.value = enabled
  }

  fun setEqualizerEnabled(enabled: Boolean) {
    prefs.edit().putBoolean("eq_enabled", enabled).apply()
    _equalizerState.value = _equalizerState.value.copy(isEnabled = enabled)
  }

  fun setEqualizerPreset(presetName: String) {
    val preset = equalizerPresets.find { it.name == presetName } ?: equalizerPresets[0]
    prefs.edit().putString("eq_preset", presetName).apply()
    _equalizerState.value = _equalizerState.value.copy(
      currentPreset = presetName,
      bandGains = preset.gains
    )
  }

  fun setEqualizerBandGain(bandIndex: Int, gain: Float) {
    val currentGains = _equalizerState.value.bandGains.toMutableList()
    if (bandIndex in currentGains.indices) {
      currentGains[bandIndex] = gain.coerceIn(-12f, 12f)
      _equalizerState.value = _equalizerState.value.copy(
        currentPreset = "Custom",
        bandGains = currentGains
      )
    }
  }
}

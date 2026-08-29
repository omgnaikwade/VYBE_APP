package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioQuality
import com.example.data.model.VybeAccent
import com.example.data.model.VybeThemeMode
import com.example.ui.components.AccentColorDialog
import com.example.ui.components.VybeHeader
import com.example.ui.theme.LocalVybeColors
import com.example.ui.theme.getAccentColor
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
  viewModel: MainViewModel,
  onNotificationClick: () -> Unit,
  onCastClick: () -> Unit,
  onQualitySelectorClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val themeMode by viewModel.themeMode.collectAsState()
  val accentColor by viewModel.accentColor.collectAsState()
  val streamingQuality by viewModel.streamingQuality.collectAsState()
  val downloadQuality by viewModel.downloadQuality.collectAsState()
  val downloadOnlyWifi by viewModel.downloadOnlyWifi.collectAsState()
  val equalizerState by viewModel.equalizerState.collectAsState()
  val accent = LocalVybeColors.current.accent

  var showEqualizerSheet by remember { mutableStateOf(false) }
  var showAccentDialog by remember { mutableStateOf(false) }
  var cacheClearedToast by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    VybeHeader(
      onNotificationClick = onNotificationClick,
      onCastClick = onCastClick
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("settings_scroll_list"),
      contentPadding = PaddingValues(bottom = 120.dp)
    ) {
      item {
        Text(
          text = "Settings",
          style = MaterialTheme.typography.displayMedium.copy(
            fontSize = 28.sp,
            letterSpacing = (-0.5).sp
          ),
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )
      }

      // 1. THEME & DISPLAY SECTION
      item {
        SettingsSectionHeader(title = "Appearance & Theme")
      }

      item {
        SettingsGroupContainer {
          // Theme Mode Switcher
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.DarkMode,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = "Theme Mode",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              VybeThemeMode.values().forEach { mode ->
                val isSelected = themeMode == mode
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) accent else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { viewModel.setThemeMode(mode) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                  Text(
                    text = mode.title,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

          // Accent Color Clickable Row (Hidden Inside Dialog)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showAccentDialog = true }
              .padding(16.dp)
              .testTag("settings_accent_color_row"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Accent Color",
                  fontSize = 14.sp,
                  color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                  text = accentColor.displayName,
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              // Current Active Color Dot
              Box(
                modifier = Modifier
                  .size(20.dp)
                  .clip(CircleShape)
                  .background(accent)
                  .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }

      // 3. AUDIO QUALITY & EQUALIZER
      item {
        SettingsSectionHeader(title = "Audio Playback")
      }

      item {
        SettingsGroupContainer {
          // Streaming Quality Item
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onQualitySelectorClick() }
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Streaming Quality",
                  fontSize = 14.sp,
                  color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                  text = "${streamingQuality.title} (${streamingQuality.bitrate})",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Icon(
              imageVector = Icons.Default.ChevronRight,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          // Equalizer Item
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showEqualizerSheet = true }
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Equalizer & Sound Effects",
                  fontSize = 14.sp,
                  color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                  text = if (equalizerState.isEnabled) "Preset: ${equalizerState.currentPreset}" else "Disabled",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Icon(
              imageVector = Icons.Default.ChevronRight,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // 4. STORAGE & DOWNLOADS
      item {
        SettingsSectionHeader(title = "Downloads & Offline")
      }

      item {
        SettingsGroupContainer {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Download over Wi-Fi only",
                  fontSize = 14.sp,
                  color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                  text = "Save mobile network data",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Switch(
              checked = downloadOnlyWifi,
              onCheckedChange = { viewModel.setDownloadOnlyWifi(it) },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent
              )
            )
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Storage Used",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
              )
              Text(
                text = if (cacheClearedToast) "0 MB used of 64 GB" else "2.4 GB cached audio",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            TextButton(
              onClick = { cacheClearedToast = true }
            ) {
              Text(
                text = if (cacheClearedToast) "Cleared" else "Clear Cache",
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // 5. ABOUT
      item {
        SettingsSectionHeader(title = "About")
      }

      item {
        SettingsGroupContainer {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "VYBE Music Player v2.4.0",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Precision 32-bit Floating Audio Engine with Lossless DAC Passthrough.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }

  // Equalizer Bottom Sheet
  if (showEqualizerSheet) {
    EqualizerBottomSheet(
      viewModel = viewModel,
      onDismiss = { showEqualizerSheet = false }
    )
  }

  // Accent Color Dialog
  if (showAccentDialog) {
    AccentColorDialog(
      currentAccent = accentColor,
      onAccentSelected = { selectedAccent ->
        viewModel.setAccentColor(selectedAccent)
      },
      onDismiss = { showAccentDialog = false }
    )
  }
}

@Composable
fun SettingsSectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleMedium.copy(
      fontWeight = FontWeight.Bold,
      fontSize = 14.sp
    ),
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
  )
}

@Composable
fun SettingsGroupContainer(content: @Composable () -> Unit) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column { content() }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerBottomSheet(
  viewModel: MainViewModel,
  onDismiss: () -> Unit
) {
  val equalizerState by viewModel.equalizerState.collectAsState()
  val accent = LocalVybeColors.current.accent
  val bandLabels = listOf("60Hz", "150Hz", "400Hz", "1kHz", "2.4kHz", "6kHz", "15kHz")

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
        .padding(bottom = 32.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Equalizer",
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          color = MaterialTheme.colorScheme.onBackground
        )

        Switch(
          checked = equalizerState.isEnabled,
          onCheckedChange = { viewModel.setEqualizerEnabled(it) },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = accent
          )
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Preset Chips
      Text(
        text = "Presets",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(8.dp))

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(viewModel.equalizerPresets) { preset ->
          val isSelected = equalizerState.currentPreset == preset.name
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .background(if (isSelected) accent else MaterialTheme.colorScheme.surfaceVariant)
              .clickable { viewModel.setEqualizerPreset(preset.name) }
              .padding(horizontal = 14.dp, vertical = 6.dp)
          ) {
            Text(
              text = preset.name,
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // 7-Band Sliders
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        bandLabels.forEachIndexed { index, label ->
          val gain = equalizerState.bandGains.getOrNull(index) ?: 0f
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = "${gain.toInt()}dB",
              fontSize = 9.sp,
              color = accent
            )

            // Slider from -12dB to +12dB
            Slider(
              value = gain,
              onValueChange = { viewModel.setEqualizerBandGain(index, it) },
              valueRange = -12f..12f,
              enabled = equalizerState.isEnabled,
              colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
              ),
              modifier = Modifier.height(100.dp)
            )

            Text(
              text = label,
              fontSize = 9.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

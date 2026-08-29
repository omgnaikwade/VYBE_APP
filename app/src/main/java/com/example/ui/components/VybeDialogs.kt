package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioQuality
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.ui.theme.LocalVybeColors
import com.example.ui.theme.VybePink

@Composable
fun CreatePlaylistDialog(
  onDismiss: () -> Unit,
  onCreate: (String, String) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  val accent = LocalVybeColors.current.accent

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(16.dp),
    title = {
      Text(
        text = "Create Playlist",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.onBackground
      )
    },
    text = {
      Column {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Playlist Name") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("create_playlist_title_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            focusedLabelColor = accent,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          )
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description (Optional)") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("create_playlist_desc_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            focusedLabelColor = accent,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          )
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            onCreate(title.trim(), description.trim())
          }
        },
        enabled = title.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = accent),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("create_playlist_confirm_button")
      ) {
        Text("Create", color = Color.White)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("create_playlist_cancel_button")
      ) {
        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
  song: Song,
  isFavorite: Boolean,
  playlists: List<Playlist>,
  onDismiss: () -> Unit,
  onPlayNext: () -> Unit,
  onAddToQueue: () -> Unit,
  onToggleFavorite: () -> Unit,
  onAddToPlaylist: (String) -> Unit
) {
  val accent = LocalVybeColors.current.accent
  var showPlaylistPicker by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(vertical = 10.dp)
          .size(width = 36.dp, height = 4.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.outlineVariant)
      )
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 10.dp)
        .padding(bottom = 30.dp)
    ) {
      // Header info
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = song.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "${song.artist} • ${song.album}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Spacer(modifier = Modifier.height(10.dp))

      if (!showPlaylistPicker) {
        SongOptionRow(
          icon = Icons.Default.QueueMusic,
          title = "Play Next",
          onClick = {
            onPlayNext()
            onDismiss()
          }
        )
        SongOptionRow(
          icon = Icons.Default.PlaylistAdd,
          title = "Add to Queue",
          onClick = {
            onAddToQueue()
            onDismiss()
          }
        )
        SongOptionRow(
          icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
          title = if (isFavorite) "Remove from Favourites" else "Add to Favourites",
          tint = if (isFavorite) accent else MaterialTheme.colorScheme.onBackground,
          onClick = {
            onToggleFavorite()
            onDismiss()
          }
        )
        SongOptionRow(
          icon = Icons.Default.Add,
          title = "Add to Playlist",
          onClick = {
            showPlaylistPicker = true
          }
        )
        SongOptionRow(
          icon = Icons.Default.Share,
          title = "Share Song",
          onClick = onDismiss
        )
      } else {
        Text(
          text = "Select Playlist",
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(vertical = 8.dp)
        )
        LazyColumn {
          items(playlists) { pl ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onAddToPlaylist(pl.id)
                  onDismiss()
                }
                .padding(vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.PlaylistAdd,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = pl.title,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SongOptionRow(
  icon: ImageVector,
  title: String,
  onClick: () -> Unit,
  tint: Color = MaterialTheme.colorScheme.onBackground
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = title,
      tint = tint,
      modifier = Modifier.size(22.dp)
    )
    Spacer(modifier = Modifier.width(16.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
      color = tint
    )
  }
}

@Composable
fun CastDevicesDialog(
  onDismiss: () -> Unit
) {
  val accent = LocalVybeColors.current.accent
  var selectedDevice by remember { mutableStateOf("This Phone") }

  val devices = listOf(
    Pair("This Phone", Icons.Default.Speaker),
    Pair("Living Room Speaker", Icons.Default.Speaker),
    Pair("Bedroom Smart TV", Icons.Default.Tv),
    Pair("Studio Soundbar", Icons.Default.Cast)
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(16.dp),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Cast,
          contentDescription = null,
          tint = accent,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Connect to a Device",
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          color = MaterialTheme.colorScheme.onBackground
        )
      }
    },
    text = {
      Column {
        devices.forEach { (name, icon) ->
          val isSelected = selectedDevice == name
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) accent.copy(alpha = 0.15f) else Color.Transparent)
              .clickable { selectedDevice = name }
              .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) accent else MaterialTheme.colorScheme.onBackground
              )
            }
            if (isSelected) {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = accent,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = accent),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Done", color = Color.White)
      }
    }
  )
}

@Composable
fun NotificationsDialog(
  onDismiss: () -> Unit
) {
  val accent = LocalVybeColors.current.accent

  val notifications = listOf(
    Triple("New Release", "Arijit Singh dropped a brand new acoustic session 'Chaleya Unplugged'.", "2h ago"),
    Triple("Trending Playlist", "Trending Community: 'Cyberpunk Night' just hit 100K plays!", "1d ago"),
    Triple("Welcome to VYBE", "Enjoy high fidelity lossless streaming and custom equalizers.", "2d ago")
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(16.dp),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Notifications,
          contentDescription = null,
          tint = accent,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Notifications",
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          color = MaterialTheme.colorScheme.onBackground
        )
      }
    },
    text = {
      Column {
        notifications.forEach { (title, body, time) ->
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = accent
              )
              Text(
                text = time,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = body,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onBackground
            )
            HorizontalDivider(
              modifier = Modifier.padding(top = 8.dp),
              color = MaterialTheme.colorScheme.outlineVariant
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close", color = accent)
      }
    }
  )
}

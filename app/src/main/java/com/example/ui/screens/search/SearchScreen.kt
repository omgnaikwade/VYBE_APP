package com.example.ui.screens.search

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Artist
import com.example.data.model.MusicCategory
import com.example.data.model.Song
import com.example.ui.components.SongRowItem
import com.example.ui.components.VybeHeader
import com.example.ui.theme.LocalVybeColors
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SearchScreen(
  viewModel: MainViewModel,
  onSongOptionsClick: (Song) -> Unit,
  onNotificationClick: () -> Unit,
  onCastClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val searchQuery by viewModel.searchQuery.collectAsState()
  val searchResults by viewModel.searchResults.collectAsState()
  val recentSearches by viewModel.recentSearches.collectAsState()
  val categories by viewModel.categories.collectAsState()
  val topArtists by viewModel.topArtists.collectAsState()
  val playbackState by viewModel.playbackState.collectAsState()
  val accent = LocalVybeColors.current.accent

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
        .testTag("search_scroll_list"),
      contentPadding = PaddingValues(bottom = 120.dp)
    ) {
      item {
        Text(
          text = "Search",
          style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp),
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )
      }

      // Search Bar
      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { viewModel.setSearchQuery(it) },
          placeholder = {
            Text(
              text = "Songs, artists, albums, playlists...",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 14.sp
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { viewModel.setSearchQuery("") }) {
                Icon(
                  imageVector = Icons.Default.Clear,
                  contentDescription = "Clear search",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(24.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
          ),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("search_text_input")
        )
      }

      if (searchQuery.isBlank()) {
        // 1. Recent Searches
        if (recentSearches.isNotEmpty()) {
          item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Recent searches",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onBackground
              )
              Text(
                text = "Clear",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                color = accent,
                modifier = Modifier
                  .clickable { viewModel.clearRecentSearches() }
                  .testTag("clear_recent_searches")
              )
            }
          }

          item {
            LazyRow(
              contentPadding = PaddingValues(horizontal = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.testTag("recent_searches_row")
            ) {
              items(recentSearches) { query ->
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { viewModel.setSearchQuery(query) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                  Text(
                    text = query,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground
                  )
                }
              }
            }
          }
        }

        // 2. Popular Searches Category Cards
        item {
          Text(
            text = "Popular searches",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)
          )
        }

        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp)
          ) {
            val chunked = categories.chunked(2)
            chunked.forEach { pair ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                pair.forEach { category ->
                  CategoryCard(
                    category = category,
                    onClick = { viewModel.setSearchQuery(category.title) },
                    modifier = Modifier.weight(1f)
                  )
                }
                if (pair.size == 1) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }

        // 3. Top Artists
        item {
          Text(
            text = "Top artists",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)
          )
        }

        item {
          LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.testTag("top_artists_row")
          ) {
            items(topArtists) { artist ->
              ArtistAvatarCard(
                artist = artist,
                onClick = { viewModel.setSearchQuery(artist.name) }
              )
            }
          }
        }

      } else {
        // Search Results
        item {
          Text(
            text = "Search results (${searchResults.size})",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
          )
        }

        if (searchResults.isEmpty()) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  imageVector = Icons.Default.Search,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "No songs found for '$searchQuery'",
                  fontSize = 14.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        } else {
          items(searchResults) { song ->
            SongRowItem(
              song = song,
              isCurrentPlaying = playbackState.currentSong?.id == song.id,
              isPlaying = playbackState.isPlaying,
              onClick = { viewModel.playSong(song, "Search: $searchQuery", searchResults) },
              onMenuClick = { onSongOptionsClick(song) }
            )
          }
        }
      }
    }
  }
}

@Composable
fun CategoryCard(
  category: MusicCategory,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val iconVector: ImageVector = when (category.iconName) {
    "heart" -> Icons.Default.Favorite
    "disco" -> Icons.Default.Sensors
    else -> Icons.Default.Headphones
  }

  Box(
    modifier = modifier
      .height(64.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(
        Brush.horizontalGradient(
          listOf(
            Color(category.colorStart),
            Color(category.colorEnd)
          )
        )
      )
      .clickable { onClick() }
      .padding(horizontal = 14.dp),
    contentAlignment = Alignment.CenterStart
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = category.title,
        style = MaterialTheme.typography.bodyMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        ),
        color = Color.White,
        modifier = Modifier.weight(1f)
      )
      Icon(
        imageVector = iconVector,
        contentDescription = null,
        tint = Color.White.copy(alpha = 0.85f),
        modifier = Modifier.size(24.dp)
      )
    }
  }
}

@Composable
fun ArtistAvatarCard(
  artist: Artist,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
      .clickable { onClick() }
      .testTag("artist_item_${artist.id}")
  ) {
    Box(
      modifier = Modifier
        .size(68.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant),
      contentAlignment = Alignment.Center
    ) {
      if (artist.avatarUrl.isNotEmpty()) {
        AsyncImage(
          model = ImageRequest.Builder(LocalContext.current)
            .data(artist.avatarUrl)
            .crossfade(true)
            .build(),
          contentDescription = artist.name,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      } else {
        Icon(
          imageVector = Icons.Default.Person,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(32.dp)
        )
      }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = artist.name,
      style = MaterialTheme.typography.bodySmall.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
      ),
      color = MaterialTheme.colorScheme.onBackground,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

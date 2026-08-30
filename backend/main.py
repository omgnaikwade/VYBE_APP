package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.MusicBackendApi
import com.example.data.model.Artist
import com.example.data.model.MusicCategory
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.storage.LocalMusicStorage
import com.example.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
private data class FavoriteRow(
    val id: Long? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("song_id") val songId: String,
    @SerialName("song_name") val songName: String,
    @SerialName("artist_name") val artistName: String,
    @SerialName("album_name") val albumName: String = "",
    @SerialName("image_url") val imageUrl: String = ""
)

@Serializable
private data class HistoryRow(
    val id: Long? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("song_id") val songId: String,
    @SerialName("song_name") val songName: String,
    @SerialName("artist_name") val artistName: String,
    @SerialName("album_name") val albumName: String = "",
    @SerialName("image_url") val imageUrl: String = ""
)

@Serializable
private data class InteractionRow(
    val id: Long? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("song_id") val songId: String,
    @SerialName("play_count") val playCount: Int = 0,
    @SerialName("skip_count") val skipCount: Int = 0,
    @SerialName("like_count") val likeCount: Int = 0
)

@Serializable
private data class PlaylistRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String
)

@Serializable
private data class PlaylistSongRow(
    val id: Long? = null,
    @SerialName("playlist_id") val playlistId: String,
    @SerialName("song_id") val songId: String,
    @SerialName("song_name") val songName: String,
    @SerialName("artist_name") val artistName: String,
    @SerialName("album_name") val albumName: String = "",
    @SerialName("image_url") val imageUrl: String = ""
)

class RealMusicRepository(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : MusicRepository {

    private val musicBackendApi = MusicBackendApi()
    private val storage = LocalMusicStorage(context)

    private val defaultArtists = listOf(
        Artist("artist_arijit", "Arijit Singh", "", "", "Popular Artist"),
        Artist("artist_shreya", "Shreya Ghoshal", "", "", "Popular Artist"),
        Artist("artist_diljit", "Diljit Dosanjh", "", "", "Popular Artist"),
        Artist("artist_anirudh", "Anirudh Ravichander", "", "", "Popular Artist"),
        Artist("artist_pritam", "Pritam", "", "", "Popular Artist"),
        Artist("artist_sid", "Sid Sriram", "", "", "Popular Artist")
    )

    private val _discoverSongs =
        MutableStateFlow<List<Song>>(storage.loadDiscoverCache())

    private val _biggestHits =
        MutableStateFlow<List<Song>>(storage.loadBiggestHitsCache())

    private val _danceHits =
        MutableStateFlow<List<Song>>(storage.loadDanceHitsCache())

    private val _trendingPlaylists =
        MutableStateFlow<List<Playlist>>(emptyList())

    private val _topArtists =
        MutableStateFlow<List<Artist>>(defaultArtists)

    private val _userPlaylists =
        MutableStateFlow<List<Playlist>>(storage.loadPlaylists())

    private val _favoriteSongs =
        MutableStateFlow<List<Song>>(storage.loadFavorites())

    private val _listeningHistory =
        MutableStateFlow<List<Song>>(storage.loadHistory())

    private val _recentSearches =
        MutableStateFlow<List<String>>(storage.loadRecentSearches())

    init {
        updatePlaylistsAndArtists()
        loadHomeData()
        loadSupabaseData()
    }

    // ---------------------------------------------------------
    // SUPABASE USER
    // ---------------------------------------------------------

    private suspend fun getUserId(): String? {
        return try {
            supabase.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            Log.e(
                "RealMusicRepo",
                "Unable to get Supabase user",
                e
            )
            null
        }
    }

    // ---------------------------------------------------------
    // LOAD SUPABASE DATA
    // ---------------------------------------------------------

    private fun loadSupabaseData() {
        scope.launch {
            val userId = getUserId() ?: return@launch

            try {
                loadFavoritesFromSupabase(userId)
                loadHistoryFromSupabase(userId)
                loadPlaylistsFromSupabase(userId)
            } catch (e: Exception) {
                Log.e(
                    "RealMusicRepo",
                    "Error loading Supabase user data",
                    e
                )
            }
        }
    }

    private suspend fun loadFavoritesFromSupabase(userId: String) {
        try {
            val rows = supabase
                .from("favorites")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<FavoriteRow>()

            val songs = rows.map {
                Song(
                    id = it.songId,
                    title = it.songName,
                    artist = it.artistName,
                    album = it.albumName,
                    artworkUrl = it.imageUrl,
                    isLiked = true
                )
            }

            _favoriteSongs.value = songs
            storage.saveFavorites(songs)

            _discoverSongs.value =
                markFavorites(_discoverSongs.value)

            _biggestHits.value =
                markFavorites(_biggestHits.value)

            _danceHits.value =
                markFavorites(_danceHits.value)

        } catch (e: Exception) {
            Log.e(
                "RealMusicRepo",
                "Failed loading favorites",
                e
            )
        }
    }

    private suspend fun loadHistoryFromSupabase(userId: String) {
        try {
            val rows = supabase
                .from("listening_history")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<HistoryRow>()

            val songs = rows.map {
                Song(
                    id = it.songId,
                    title = it.songName,
                    artist = it.artistName,
                    album = it.albumName,
                    artworkUrl = it.imageUrl
                )
            }

            _listeningHistory.value = songs.take(50)
            storage.saveHistory(_listeningHistory.value)

            // Refresh personalized home after history is loaded.
            refreshPersonalizedHome()

        } catch (e: Exception) {
            Log.e(
                "RealMusicRepo",
                "Failed loading history",
                e
            )
        }
    }

    private suspend fun loadPlaylistsFromSupabase(userId: String) {
        try {
            val playlistRows = supabase
                .from("playlists")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<PlaylistRow>()

            val result = mutableListOf<Playlist>()

            for (playlist in playlistRows) {

                val songRows = supabase
                    .from("playlist_songs")
                    .select {
                        filter {
                            eq("playlist_id", playlist.id)
                        }
                    }
                    .decodeList<PlaylistSongRow>()

                val songs = songRows.map {
                    Song(
                        id = it.songId,
                        title = it.songName,
                        artist = it.artistName,
                        album = it.albumName,
                        artworkUrl = it.imageUrl
                    )
                }

                result.add(
                    Playlist(
                        id = playlist.id,
                        title = playlist.name,
                        description = "",
                        coverUrl =
                            songs.firstOrNull()?.artworkUrl ?: "",
                        songCount = songs.size,
                        songs = songs,
                        isUserCreated = true,
                        gradientStart = 0xFF3B82F6,
                        gradientEnd = 0xFF8B5CF6
                    )
                )
            }

            _userPlaylists.value = result
            storage.savePlaylists(result)

        } catch (e: Exception) {
            Log.e(
                "RealMusicRepo",
                "Failed loading playlists",
                e
            )
        }
    }

    // ---------------------------------------------------------
    // HOME DATA
    // ---------------------------------------------------------

    private fun loadHomeData() {

        // Initial/default Discover
        scope.launch {
            try {
                val songs = musicBackendApi.searchMusic(
                    query = "Top Trending Hits",
                    limit = 16
                )

                if (songs.isNotEmpty()) {
                    val marked = markFavorites(songs)

                    _discoverSongs.value = marked
                    storage.saveDiscoverCache(marked)

                    updatePlaylistsAndArtists()
                }

            } catch (e: Exception) {
                Log.e(
                    "RealMusicRepo",
                    "Error loading discover songs",
                    e
                )
            }
        }

        // Initial Bollywood section
        scope.launch {
            try {
                val songs = musicBackendApi.searchMusic(
                    query = "Top Bollywood Hits",
                    limit = 12
                )

                if (songs.isNotEmpty()) {
                    val marked = markFavorites(songs)

                    _biggestHits.value = marked
                    storage.saveBiggestHitsCache(marked)

                    updatePlaylistsAndArtists()
                }

            } catch (e: Exception) {
                Log.e(
                    "RealMusicRepo",
                    "Error loading biggest hits",
                    e
                )
            }
        }

        // Initial Dance section
        scope.launch {
            try {
                val songs = musicBackendApi.searchMusic(
                    query = "Dance Party EDM",
                    limit = 12
                )

                if (songs.isNotEmpty()) {
                    val marked = markFavorites(songs)

                    _danceHits.value = marked
                    storage.saveDanceHitsCache(marked)

                    updatePlaylistsAndArtists()
                }

            } catch (e: Exception) {
                Log.e(
                    "RealMusicRepo",
                    "Error loading dance hits",
                    e
                )
            }
        }

        // Personalization is loaded separately from listening history.
        scope.launch {
            refreshPersonalizedHome()
        }
    }

    // ---------------------------------------------------------
    // PERSONALIZED HOME ALGORITHM
    // ---------------------------------------------------------

    private fun getPreferredArtist(): String? {

        val history = _listeningHistory.value

        if (history.isEmpty()) {
            return null
        }

        val artistCounts = mutableMapOf<String, Int>()

        history.forEach { song ->

            val artist = song.artist
                .trim()
                .split(",", "&", "feat.", "ft.")
                .firstOrNull()
                ?.trim()
                ?: ""

            if (artist.isNotBlank()) {
                artistCounts[artist] =
                    (artistCounts[artist] ?: 0) + 1
            }
        }

        return artistCounts
            .maxByOrNull { it.value }
            ?.key
    }

    private suspend fun refreshPersonalizedHome() {

        try {

            val preferredArtist = getPreferredArtist()

            if (preferredArtist.isNullOrBlank()) {
                return
            }

            Log.d(
                "RealMusicRepo",
                "Preferred artist: $preferredArtist"
            )

            val personalizedSongs =
                musicBackendApi.searchMusic(
                    query = preferredArtist,
                    limit = 16
                )

            if (personalizedSongs.isEmpty()) {
                return
            }

            val historyIds =
                _listeningHistory.value
                    .map { it.id }
                    .toSet()

            val filteredSongs =
                personalizedSongs
                    .filterNot { it.id in historyIds }
                    .distinctBy { it.id }

            val finalSongs =
                if (filteredSongs.isNotEmpty()) {
                    filteredSongs
                } else {
                    personalizedSongs.distinctBy { it.id }
                }

            val marked =
                markFavorites(finalSongs.take(16))

            _discoverSongs.value = marked

            storage.saveDiscoverCache(marked)

            updatePlaylistsAndArtists()

            Log.d(
                "RealMusicRepo",
                "Personalized Home updated with ${marked.size} songs"
            )

        } catch (e: Exception) {

            Log.e(
                "RealMusicRepo",
                "Personalized Home refresh failed",
                e
            )
        }
    }

    // ---------------------------------------------------------
    // PLAYLISTS + ARTISTS
    // ---------------------------------------------------------

    @Synchronized
    private fun updatePlaylistsAndArtists() {

        val discoverList = _discoverSongs.value
        val hitsList = _biggestHits.value
        val danceList = _danceHits.value

        val playlists = mutableListOf<Playlist>()

        if (discoverList.isNotEmpty()) {
            playlists.add(
                Playlist(
                    id = "pl_trending_now",
                    title = "VYBE Top 50",
                    description =
                        "Personalized music based on your listening.",
                    coverUrl =
                        discoverList.firstOrNull()?.artworkUrl ?: "",
                    songCount = discoverList.size,
                    songs = discoverList,
                    gradientStart = 0xFF8B5CF6,
                    gradientEnd = 0xFFFF2D75
                )
            )
        }

        if (hitsList.isNotEmpty()) {
            playlists.add(
                Playlist(
                    id = "pl_bollywood_butter",
                    title = "Bollywood Butter",
                    description =
                        "Pure Bollywood magic featuring top chart hits and golden melodies.",
                    coverUrl =
                        hitsList.firstOrNull()?.artworkUrl ?: "",
                    songCount = hitsList.size,
                    songs = hitsList,
                    gradientStart = 0xFFFF2D75,
                    gradientEnd = 0xFFE11D48
                )
            )
        }

        if (danceList.isNotEmpty()) {
            playlists.add(
                Playlist(
                    id = "pl_club_dance",
                    title = "Club Hyperdrive",
                    description =
                        "High octane EDM, club rhythms and heart-pumping beats.",
                    coverUrl =
                        danceList.firstOrNull()?.artworkUrl ?: "",
                    songCount = danceList.size,
                    songs = danceList,
                    gradientStart = 0xFF06B6D4,
                    gradientEnd = 0xFF3B82F6
                )
            )
        }

        _trendingPlaylists.value = playlists

        val allSongs =
            discoverList + hitsList + danceList

        val artistMap =
            mutableMapOf<String, String>()

        allSongs.forEach { song ->

            val mainArtist =
                song.artist
                    .split(",", "&", "feat.", "ft.")
                    .firstOrNull()
                    ?.trim()
                    ?: song.artist

            if (
                mainArtist.isNotBlank() &&
                !artistMap.containsKey(mainArtist) &&
                song.artworkUrl.isNotBlank()
            ) {
                artistMap[mainArtist] =
                    song.artworkUrl
            }
        }

        val dynamicArtists =
            artistMap.entries
                .take(8)
                .map { (name, artUrl) ->
                    Artist(
                        id =
                            "artist_${Math.abs(name.hashCode())}",
                        name = name,
                        avatarUrl = artUrl,
                        monthlyListeners = "",
                        genre = "Popular Artist"
                    )
                }

        if (dynamicArtists.isNotEmpty()) {
            _topArtists.value =
                dynamicArtists
        }
    }

    // ---------------------------------------------------------
    // FAVORITES
    // ---------------------------------------------------------

    private fun markFavorites(
        songs: List<Song>
    ): List<Song> {

        val favIds =
            _favoriteSongs.value
                .map { it.id }
                .toSet()

        return songs.map {
            it.copy(
                isLiked =
                    favIds.contains(it.id)
            )
        }
    }

    override fun getFavoriteSongs(): Flow<List<Song>> =
        _favoriteSongs.asStateFlow()

    override suspend fun toggleFavorite(
        song: Song
    ): Boolean {

        val currentFavs =
            _favoriteSongs.value.toMutableList()

        val existingIndex =
            currentFavs.indexOfFirst {
                it.id == song.id
            }

        val isNowLiked: Boolean

        if (existingIndex >= 0) {

            currentFavs.removeAt(existingIndex)

            isNowLiked = false

            deleteFavoriteFromSupabase(song.id)

        } else {

            currentFavs.add(
                0,
                song.copy(isLiked = true)
            )

            isNowLiked = true

            saveFavoriteToSupabase(
                song.copy(isLiked = true)
            )
        }

        _favoriteSongs.value =
            currentFavs

        storage.saveFavorites(
            currentFavs
        )

        _discoverSongs.value =
            markFavorites(
                _discoverSongs.value
            )

        _biggestHits.value =
            markFavorites(
                _biggestHits.value
            )

        _danceHits.value =
            markFavorites(
                _danceHits.value
            )

        updateLikeInteraction(
            song.id,
            isNowLiked
        )

        return isNowLiked
    }

    private suspend fun saveFavoriteToSupabase(
        song: Song
    ) {

          val userId = getUserId() ?: return

    try {
        val existing =
            supabase
                .from("favorites")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("song_id", song.id)
                    }
                }
                .decodeList<FavoriteRow>()

        if (existing.isNotEmpty()) {
            return
        }

        supabase
            .from("favorites")
            .insert(
                FavoriteRow(
                    userId = userId,
                    songId = song.id,
                    songName = song.title,
                    artistName = song.artist,
                    albumName = song.album,
                    imageUrl = song.artworkUrl
                )
            )

    } catch (e: Exception) {
        Log.e(
            "RealMusicRepo",
            "Failed saving favorite",
            e
        )
    }
}

private suspend fun deleteFavoriteFromSupabase(
    songId: String
) {
    val userId = getUserId() ?: return

    try {
        supabase
            .from("favorites")
            .delete {
                filter {
                    eq("user_id", userId)
                    eq("song_id", songId)
                }
            }

    } catch (e: Exception) {
        Log.e(
            "RealMusicRepo",
            "Failed deleting favorite",
            e
        )
    }
}

// ---------------------------------------------------------
// LISTENING HISTORY
// ---------------------------------------------------------

override fun getListeningHistory(): Flow<List<Song>> =
    _listeningHistory.asStateFlow()

override suspend fun addToHistory(
    song: Song
) {
    val currentHistory =
        _listeningHistory.value.toMutableList()

    currentHistory.removeAll {
        it.id == song.id
    }

    currentHistory.add(0, song)

    val trimmed =
        currentHistory.take(50)

    _listeningHistory.value = trimmed

    storage.saveHistory(trimmed)

    saveHistoryToSupabase(song)

    incrementPlayCount(song.id)
}

private suspend fun saveHistoryToSupabase(
    song: Song
) {
    val userId = getUserId() ?: return

    try {
        supabase
            .from("listening_history")
            .insert(
                HistoryRow(
                    userId = userId,
                    songId = song.id,
                    songName = song.title,
                    artistName = song.artist,
                    albumName = song.album,
                    imageUrl = song.artworkUrl
                )
            )

    } catch (e: Exception) {
        Log.e(
            "RealMusicRepo",
            "Failed saving history",
            e
        )
    }
}

// ---------------------------------------------------------
// SONG INTERACTIONS
// ---------------------------------------------------------

private suspend fun incrementPlayCount(
    songId: String
) {
    val userId = getUserId() ?: return

    try {
        val rows = supabase
            .from("song_interactions")
            .select {
                filter {
                    eq("user_id", userId)
                    eq("song_id", songId)
                }
            }
            .decodeList<InteractionRow>()

        if (rows.isNotEmpty()) {
            val row = rows.first()
            val rowId = row.id ?: return

            supabase
                .from("song_interactions")
                .update(
                    {
                        set(
                            "play_count",
                            row.playCount + 1
                        )
                    }
                ) {
                    filter {
                        eq("id", rowId)
                    }
                }
        } else {
            supabase
                .from("song_interactions")
                .insert(
                    InteractionRow(
                        userId = userId,
                        songId = songId,
                        playCount = 1,
                        skipCount = 0,
                        likeCount = 0
                    )
                )
        }

    } catch (e: Exception) {
        Log.e(
            "RealMusicRepo",
            "Failed updating play count",
            e
        )
    }
}

private suspend fun updateLikeInteraction(
    songId: String,
    liked: Boolean
) {
    val userId = getUserId() ?: return

    try {
        val rows = supabase
            .from("song_interactions")
            .select {
                filter {
                    eq("user_id", userId)
                    eq("song_id", songId)
                }
            }
            .decodeList<InteractionRow>()

        if (rows.isNotEmpty()) {
            val row = rows.first()
            val rowId = row.id ?: return

            val newLikeCount =
                if (liked) {
                    row.likeCount + 1
                } else {
                    maxOf(0, row.likeCount - 1)
                }

            supabase
                .from("song_interactions")
                .update(
                    {
                        set(
                            "like_count",
                            newLikeCount
                        )
                    }
                ) {
                    filter {
                        eq("id", rowId)
                    }
                }

        } else if (liked) {
            supabase
                .from("song_interactions")
                .insert(
                    InteractionRow(
                        userId = userId,
                        songId = songId,
                        playCount = 0,
                        skipCount = 0,
                        likeCount = 1
                    )
                )
        }

    } catch (e: Exception) {
        Log.e(
            "RealMusicRepo",
            "Failed updating like interaction",
            e
        )
    }
}

// ---------------------------------------------------------
// PLAYLISTS
// ---------------------------------------------------------

override fun getUserPlaylists(): Flow<List<Playlist>> =
    _userPlaylists.asStateFlow()

override suspend fun createPlaylist(
    title: String,
    description: String
) {
    val cleanTitle = title.trim()

    if (cleanTitle.isBlank()) return

    val userId = getUserId()

    if (userId == null) {
        val newPlaylist = Playlist(
            id = "user_pl_${System.currentTimeMillis()}",
            title = cleanTitle,
            description = description.trim(),
            coverUrl = "",
            songCount = 0,
            songs = emptyList(),
            isUserCreated = true,
            gradientStart = 0xFF3B82F6,
            gradientEnd = 0xFF8B5CF6
        )

        val updated =
            listOf(newPlaylist) + _userPlaylists.value

        _userPlaylists.value = updated
        storage.savePlaylists(updated)
        return
    }

    try {
        val playlistId = UUID.randomUUID().toString()

        supabase
            .from("playlists")
            .insert(
                PlaylistRow(
                    id = playlistId,
                    userId = userId,
                    name = cleanTitle
                )
            )

        val newPlaylist = Playlist(
            id = playlistId,
            title = cleanTitle,
            description = description.trim(),
            coverUrl = "",
            songCount = 0,
            songs = emptyList(),
            isUserCreated = true,
            gradientStart = 0xFF3B82F6,
            gradientEnd = 0xFF8B5CF6
        )

        val updated =
            listOf(newPlaylist) + _userPlaylists.value

        _userPlaylists.value = updated
        storage.savePlaylists(updated)

    } catch (e: Exception) {
        Log.e(
            "RealMusicRepo",
            "Failed creating playlist",
            e
        )
    }
}

override suspend fun addSongToPlaylist(
    playlistId: String,
    song: Song
) {
    val currentPlaylists =
        _userPlaylists.value.toMutableList()

    val index =
        currentPlaylists.indexOfFirst {
            it.id == playlistId
        }

    if (index < 0) return

    val playlist = currentPlaylists[index]

    val songs =
        playlist.songs.toMutableList()

    if (songs.any { it.id == song.id }) {
        return
    }

    val userId = getUserId()

    if (userId != null) {
        try {
            supabase
                .from("playlist_songs")
                .insert(
                    PlaylistSongRow(
                        playlistId = playlistId,
                        songId = song.id,
                        songName = song.title,
                        artistName = song.artist,
                        albumName = song.album,
                        imageUrl = song.artworkUrl
                    )
                )
        } catch (e: Exception) {
            Log.e(
                "RealMusicRepo",
                "Failed adding song to playlist",
                e
            )
            return
        }
    }

    songs.add(song)

    val updatedPlaylist =
        playlist.copy(
            songs = songs,
            songCount = songs.size,
            coverUrl =
                if (playlist.coverUrl.isBlank())
                    song.artworkUrl
                else
                    playlist.coverUrl
        )

    currentPlaylists[index] = updatedPlaylist
    _userPlaylists.value = currentPlaylists

    storage.savePlaylists(currentPlaylists)
}

override suspend fun removeSongFromPlaylist(
    playlistId: String,
    songId: String
) {
    val currentPlaylists =
        _userPlaylists.value.toMutableList()

    val index =
        currentPlaylists.indexOfFirst {
            it.id == playlistId
        }

    if (index < 0) return

    val playlist = currentPlaylists[index]

    val userId = getUserId()

    if (userId != null) {
        try {
            supabase
                .from("playlist_songs")
                .delete {
                    filter {
                        eq("playlist_id", playlistId)
                        eq("song_id", songId)
                    }
                }
        } catch (e: Exception) {
            Log.e(
                "RealMusicRepo",
                "Failed removing playlist song",
                e
            )
            return
        }
    }

    val songs =
        playlist.songs.filterNot {
            it.id == songId
        }

    val updatedPlaylist =
        playlist.copy(
            songs = songs,
            songCount = songs.size,
            coverUrl =
                if (songs.isEmpty())
                    ""
                else
                    playlist.coverUrl
        )

    currentPlaylists[index] = updatedPlaylist
    _userPlaylists.value = currentPlaylists

    storage.savePlaylists(currentPlaylists)
}

// ---------------------------------------------------------
// SEARCH
// ---------------------------------------------------------

override fun search(
    query: String
): Flow<List<Song>> =
    flow {
        val clean = query.trim()

        if (clean.isBlank()) {
            emit(emptyList())
            return@flow
        }

        try {
            val results =
                musicBackendApi.searchMusic(
                    query = clean,
                    limit = 25
                )

            emit(markFavorites(results))

        } catch (e: Exception) {
            Log.e(
                "RealMusicRepo",
                "Search failed for $query",
                e
            )

            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

override fun getRecentSearches(): Flow<List<String>> =
    _recentSearches.asStateFlow()

override suspend fun addRecentSearch(
    query: String
) {
    val clean = query.trim()

    if (clean.isBlank()) return

    val current =
        _recentSearches.value.toMutableList()

    current.remove(clean)
    current.add(0, clean)

    _recentSearches.value = current
    storage.saveRecentSearches(current)
}

override suspend fun clearRecentSearches() {
    _recentSearches.value = emptyList()
    storage.saveRecentSearches(emptyList())
}

// ---------------------------------------------------------
// HOME FLOWS
// ---------------------------------------------------------

override fun getDiscoverSongs(): Flow<List<Song>> =
    _discoverSongs.asStateFlow()

override fun getBiggestHits(): Flow<List<Song>> =
    _biggestHits.asStateFlow()

override fun getDanceHits(): Flow<List<Song>> =
    _danceHits.asStateFlow()

override fun getTrendingPlaylists(): Flow<List<Playlist>> =
    _trendingPlaylists.asStateFlow()

override fun getTopArtists(): Flow<List<Artist>> =
    _topArtists.asStateFlow()

override fun getCategories(): Flow<List<MusicCategory>> =
    flow {
        emit(
            listOf(
                MusicCategory(
                    "cat_bollywood",
                    "Bollywood",
                    0xFFFF2D75,
                    0xFFE11D48,
                    "heart"
                ),
                MusicCategory(
                    "cat_punjabi",
                    "Punjabi",
                    0xFFF97316,
                    0xFFEA580C,
                    "headphones"
                ),
                MusicCategory(
                    "cat_pop",
                    "Pop",
                    0xFFA855F7,
                    0xFF7C3AED,
                    "headphones"
                ),
                MusicCategory(
                    "cat_dance",
                    "Dance & EDM",
                    0xFF06B6D4,
                    0xFF0284C7,
                    "disco"
                ),
                MusicCategory(
                    "cat_chill",
                    "Chill & Lo-Fi",
                    0xFF10B981,
                    0xFF059669,
                    "headphones"
                ),
                MusicCategory(
                    "cat_rock",
                    "Rock & Indie",
                    0xFFEC4899,
                    0xFFDB2777,
                    "headphones"
                ),
                MusicCategory(
                    "cat_devotional",
                    "Devotional",
                    0xFFF59E0B,
                    0xFFD97706,
                    "heart"
                ),
                MusicCategory(
                    "cat_hiphop",
                    "Hip-Hop",
                    0xFF3B82F6,
                    0xFF2563EB,
                    "headphones"
                )
            )
        )
    }
}  

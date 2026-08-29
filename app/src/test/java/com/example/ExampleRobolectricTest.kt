package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Song
import com.example.data.repository.RealMusicRepository
import com.example.data.storage.LocalMusicStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("VYBE", appName)
  }

  @Test
  fun `verify local storage saves and retrieves favorites and playlists`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = LocalMusicStorage(context)

    val sampleSong = Song(
      id = "test_song_1",
      title = "Real Stream Track",
      artist = "Test Artist",
      durationSeconds = 180,
      artworkUrl = "https://example.com/art.jpg",
      audioStreamUrl = "https://example.com/audio.mp3"
    )

    storage.saveFavorites(listOf(sampleSong))
    val loadedFavs = storage.loadFavorites()
    assertEquals(1, loadedFavs.size)
    assertEquals("Real Stream Track", loadedFavs[0].title)
  }

  @Test
  fun `verify music repository categories and artists`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = RealMusicRepository(context)

    val categories = repo.getCategories().first()
    assertTrue("Categories should not be empty", categories.isNotEmpty())

    val artists = repo.getTopArtists().first()
    assertTrue("Artists should not be empty", artists.isNotEmpty())
  }
}

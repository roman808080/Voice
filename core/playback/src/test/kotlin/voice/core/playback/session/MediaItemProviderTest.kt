package voice.core.playback.session

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import org.junit.runner.RunWith
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.playback.MemoryDataStore
import voice.core.playback.session.search.book
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class MediaItemProviderTest {

  private val provider = MediaItemProvider(
    bookRepository = mockk(),
    application = mockk(),
    chapterRepo = mockk(),
    contentRepo = mockk(),
    imageFileProvider = mockk(),
    currentBookStoreId = MemoryDataStore(null),
  )

  @Test
  fun `adds default SubRip configuration to playback item`() {
    val subtitleUri = Uri.parse("file:///book/chapter.srt")
    val chapter = Chapter(
      id = ChapterId("file:///book/chapter.mp3"),
      name = "Chapter",
      duration = 10_000,
      fileLastModified = Instant.EPOCH,
      fileSize = 0,
      markData = emptyList(),
      subtitleUri = subtitleUri,
    )

    val mediaItem = provider.playbackItems(book(listOf(chapter))).single()
    val configuration = mediaItem.localConfiguration!!.subtitleConfigurations.single()

    assertEquals(expected = subtitleUri, actual = configuration.uri)
    assertEquals(expected = MimeTypes.APPLICATION_SUBRIP, actual = configuration.mimeType)
    assertEquals(expected = C.SELECTION_FLAG_DEFAULT, actual = configuration.selectionFlags)
  }

  @Test
  fun `does not add subtitle configuration without sidecar`() {
    val chapter = Chapter(
      id = ChapterId("file:///book/chapter.mp3"),
      name = "Chapter",
      duration = 10_000,
      fileLastModified = Instant.EPOCH,
      fileSize = 0,
      markData = emptyList(),
    )

    val mediaItem = provider.playbackItems(book(listOf(chapter))).single()

    assertEquals(expected = emptyList(), actual = mediaItem.localConfiguration!!.subtitleConfigurations)
  }
}

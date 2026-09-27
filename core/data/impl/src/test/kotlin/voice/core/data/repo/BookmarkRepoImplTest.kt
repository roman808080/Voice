package voice.core.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Bookmark
import voice.core.data.ChapterId
import voice.core.data.repo.internals.AppDb
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class BookmarkRepoImplTest {

  private lateinit var db: AppDb
  private lateinit var repo: BookmarkRepo

  @Before
  fun setUp() {
    db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
      .allowMainThreadQueries()
      .build()
    repo = BookmarkRepoImpl(db.bookmarkDao())
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun bookmarkFlowEmitsBookmarksAddedAfterCollectionStarts() = runTest {
    val chapterId = ChapterId("chapter")
    val content = BookContent(
      id = BookId("book"),
      playbackSpeed = 1F,
      skipSilence = false,
      isActive = true,
      lastPlayedAt = Instant.EPOCH,
      author = null,
      name = "Book",
      addedAt = Instant.EPOCH,
      chapters = listOf(chapterId),
      currentChapter = chapterId,
      positionInChapter = 0,
      cover = null,
      gain = 0F,
      genre = null,
      narrator = null,
      series = null,
      part = null,
    )
    val bookmark = Bookmark(
      bookId = content.id,
      chapterId = chapterId,
      title = "Saved place",
      time = 1_000,
      addedAt = Instant.EPOCH,
      setBySleepTimer = false,
      id = Bookmark.Id.random(),
    )
    val observed = backgroundScope.async {
      repo.bookmarks(content).first { bookmark in it }
    }

    repo.addBookmark(bookmark)

    assertEquals(expected = listOf(bookmark), actual = observed.await())
  }
}

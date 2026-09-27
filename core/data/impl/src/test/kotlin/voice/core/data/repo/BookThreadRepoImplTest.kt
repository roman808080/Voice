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
import voice.core.data.ChapterId
import voice.core.data.ThreadId
import voice.core.data.repo.internals.AppDb
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class BookThreadRepoImplTest {

  private lateinit var db: AppDb
  private lateinit var contentRepo: BookContentRepo
  private lateinit var threadRepo: BookThreadRepo

  @Before
  fun setUp() {
    db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
      .allowMainThreadQueries()
      .build()
    contentRepo = BookContentRepoImpl(db.bookContentDao())
    threadRepo = BookThreadRepoImpl(db.bookThreadDao(), contentRepo, db)
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun threadsKeepIndependentPositionsAndMirrorOnlyTheActiveThread() = runTest {
    val firstChapter = ChapterId("first")
    val secondChapter = ChapterId("second")
    val content = content(firstChapter, secondChapter)
    threadRepo.initializeBook(content)

    val second = threadRepo.createAndActivate(
      bookId = content.id,
      title = "Thread 2",
      chapterId = secondChapter,
      positionInChapter = 200,
    )
    threadRepo.updatePosition(
      bookId = content.id,
      threadId = ThreadId.Default,
      chapterId = firstChapter,
      positionInChapter = 100,
    )

    assertEquals(expected = 200, actual = contentRepo.get(content.id)?.positionInChapter)
    assertEquals(expected = 100, actual = threadRepo.all(content.id).single { it.id == ThreadId.Default }.positionInChapter)

    assertEquals(expected = ThreadId.Default, actual = threadRepo.activate(content.id, ThreadId.Default)?.id)
    assertEquals(expected = 100, actual = contentRepo.get(content.id)?.positionInChapter)

    val replacement = threadRepo.delete(content.id, ThreadId.Default)
    assertEquals(expected = second.id, actual = replacement?.id)
    assertEquals(expected = second.id, actual = contentRepo.get(content.id)?.activeThreadId)
  }

  @Test
  fun finalThreadCannotBeDeleted() = runTest {
    val content = content(ChapterId("first"))
    threadRepo.initializeBook(content)

    assertFailsWith<IllegalArgumentException> {
      threadRepo.delete(content.id, ThreadId.Default)
    }
  }

  @Test
  fun reconciliationPreservesTheLatestActiveThread() = runTest {
    val firstChapter = ChapterId("first")
    val secondChapter = ChapterId("second")
    val staleContent = content(firstChapter, secondChapter)
    threadRepo.initializeBook(staleContent)
    val second = threadRepo.createAndActivate(
      bookId = staleContent.id,
      title = "Thread 2",
      chapterId = secondChapter,
      positionInChapter = 200,
    )

    threadRepo.reconcileChapters(staleContent, staleContent.chapters)

    val reconciled = contentRepo.get(staleContent.id)
    assertEquals(expected = second.id, actual = reconciled?.activeThreadId)
    assertEquals(expected = secondChapter, actual = reconciled?.currentChapter)
    assertEquals(expected = 200, actual = reconciled?.positionInChapter)
  }

  @Test
  fun positionTransactionsKeepInvalidationFlowUsable() = runTest {
    val chapter = ChapterId("chapter")
    val content = content(chapter)
    threadRepo.initializeBook(content)
    val observed = backgroundScope.async {
      threadRepo.flow(content.id).first { threads ->
        threads.single().positionInChapter == 25L
      }
    }

    repeat(25) { position ->
      threadRepo.updatePosition(
        bookId = content.id,
        threadId = ThreadId.Default,
        chapterId = chapter,
        positionInChapter = position + 1L,
      )
    }

    assertEquals(expected = 25L, actual = observed.await().single().positionInChapter)
    assertEquals(expected = 25L, actual = contentRepo.get(content.id)?.positionInChapter)
  }

  @Test
  fun initializationRepairsAMissingActiveThread() = runTest {
    val chapter = ChapterId("chapter")
    val content = content(chapter)
    threadRepo.initializeBook(content)
    val second = threadRepo.createAndActivate(
      bookId = content.id,
      title = "Thread 2",
      chapterId = chapter,
      positionInChapter = 200,
    )
    db.bookThreadDao().delete(content.id, second.id)

    threadRepo.initializeBook(requireNotNull(contentRepo.get(content.id)))

    val threads = threadRepo.all(content.id)
    assertEquals(expected = 2, actual = threads.size)
    assertEquals(expected = 200, actual = threads.single { it.id == second.id }.positionInChapter)
  }

  @Test
  fun positionUpdateRepairsAMissingActiveThread() = runTest {
    val chapter = ChapterId("chapter")
    val content = content(chapter)
    contentRepo.put(content)

    threadRepo.updatePosition(
      bookId = content.id,
      threadId = ThreadId.Default,
      chapterId = chapter,
      positionInChapter = 345,
      playedAt = Instant.ofEpochSecond(10),
    )

    assertEquals(expected = 345, actual = threadRepo.all(content.id).single().positionInChapter)
    assertEquals(expected = 345, actual = contentRepo.get(content.id)?.positionInChapter)
    assertEquals(expected = Instant.ofEpochSecond(10), actual = contentRepo.get(content.id)?.lastPlayedAt)
  }

  private fun content(vararg chapters: ChapterId): BookContent {
    return BookContent(
      id = BookId("book"),
      playbackSpeed = 1F,
      skipSilence = false,
      isActive = true,
      lastPlayedAt = Instant.EPOCH,
      author = null,
      name = "Book",
      addedAt = Instant.EPOCH,
      chapters = chapters.toList(),
      currentChapter = chapters.first(),
      positionInChapter = 0,
      cover = null,
      gain = 0F,
      genre = null,
      narrator = null,
      series = null,
      part = null,
    )
  }
}

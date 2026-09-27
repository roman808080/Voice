package voice.features.bookmark

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.BookThread
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.ThreadId
import voice.core.data.repo.BookRepository
import voice.core.data.repo.BookThreadRepo
import voice.core.data.repo.BookmarkRepo
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.playback.CurrentBookResolver
import voice.core.playback.PlayerController
import voice.navigation.Navigator
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BookmarkViewModelTest {

  @Test
  fun viewStateShowsIndependentThreadsAndRememberedTab() = runTest {
    val chapter = Chapter(
      id = ChapterId("chapter"),
      name = "Chapter one",
      duration = 10_000,
      fileLastModified = Instant.EPOCH,
      fileSize = 0,
      markData = emptyList(),
    )
    val secondId = ThreadId("second")
    val content = BookContent(
      id = BookId("book"),
      playbackSpeed = 1F,
      skipSilence = false,
      isActive = true,
      lastPlayedAt = Instant.EPOCH,
      author = null,
      name = "Book",
      addedAt = Instant.EPOCH,
      chapters = listOf(chapter.id),
      currentChapter = chapter.id,
      positionInChapter = 2_000,
      cover = null,
      gain = 0F,
      genre = null,
      narrator = null,
      series = null,
      part = null,
      activeThreadId = secondId,
    )
    val book = Book(content, listOf(chapter))
    val threads = listOf(
      BookThread(content.id, ThreadId.Default, null, chapter.id, 1_000, Instant.EPOCH),
      BookThread(content.id, secondId, "Research", chapter.id, 2_000, Instant.EPOCH.plusSeconds(1)),
    )
    val repo = mockk<BookRepository> {
      coEvery { get(content.id) } returns book
      every { flow(content.id) } returns flowOf(book)
    }
    val threadRepo = mockk<BookThreadRepo> {
      every { flow(content.id) } returns flowOf(threads)
    }
    val tabStore = MemoryDataStore(true)
    val viewModel = BookmarkViewModel(
      currentBookStore = MemoryDataStore(content.id),
      bookmarkTabStore = tabStore,
      repo = repo,
      threadRepo = threadRepo,
      bookmarkRepo = mockk<BookmarkRepo> {
        coEvery { bookmarks(content) } returns emptyList()
      },
      currentBookResolver = mockk<CurrentBookResolver>(),
      playerController = mockk<PlayerController>(),
      navigator = mockk<Navigator>(),
      context = ApplicationProvider.getApplicationContext<Context>(),
      kioskModeFeatureFlag = MemoryFeatureFlag(false),
      bookId = content.id,
    )

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      var state = awaitItem()
      while (state.threads.size != 2) state = awaitItem()
      assertEquals(expected = BookmarkScreenTab.Threads, actual = state.selectedTab)
      assertEquals(expected = "Default", actual = state.threads.last().title)
      assertFalse(state.threads.last().active)
      assertEquals(expected = "Research", actual = state.threads.first().title)
      assertTrue(state.threads.first().active)
      cancelAndIgnoreRemainingEvents()
    }
  }
}

package voice.core.playback.playstate

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.ThreadId
import voice.core.data.repo.BookThreadRepo
import voice.core.playback.session.MediaId
import kotlin.test.Test

class PositionUpdaterTest {

  @Test
  fun playingPositionIsPersistedWithoutAStopCallback() = runTest {
    val bookId = BookId("book")
    val chapterId = ChapterId("chapter")
    val threadId = ThreadId("thread")
    val mediaId = Json.encodeToString(
      MediaId.serializer(),
      MediaId.Chapter(bookId, chapterId, threadId),
    )
    val player = mockk<Player>(relaxed = true) {
      every { currentMediaItem } returns MediaItem.Builder().setMediaId(mediaId).build()
      every { currentPosition } returns 345
    }
    val threadRepo = mockk<BookThreadRepo>(relaxed = true)
    val playStateManager = PlayStateManager().apply {
      playState = PlayStateManager.PlayState.Playing
    }
    val updater = PositionUpdater(threadRepo, backgroundScope, playStateManager)

    updater.attachTo(player)
    runCurrent()
    advanceTimeBy(399)
    coVerify(exactly = 0) {
      threadRepo.updatePosition(bookId, threadId, chapterId, 345, any())
    }

    advanceTimeBy(1)
    runCurrent()

    coVerify(exactly = 1) {
      threadRepo.updatePosition(bookId, threadId, chapterId, 345, any())
    }
    updater.release()
  }
}

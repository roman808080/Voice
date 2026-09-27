package voice.core.data.repo

import kotlinx.coroutines.flow.Flow
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.BookThread
import voice.core.data.ChapterId
import voice.core.data.ThreadId
import java.time.Instant

public interface BookThreadRepo {

  public fun flow(bookId: BookId): Flow<List<BookThread>>

  public suspend fun all(bookId: BookId): List<BookThread>

  public suspend fun initializeBook(content: BookContent)

  public suspend fun createAndActivate(
    bookId: BookId,
    title: String,
    chapterId: ChapterId,
    positionInChapter: Long,
  ): BookThread

  public suspend fun activate(
    bookId: BookId,
    threadId: ThreadId,
  ): BookThread?

  public suspend fun rename(
    bookId: BookId,
    threadId: ThreadId,
    title: String,
  )

  public suspend fun delete(
    bookId: BookId,
    threadId: ThreadId,
  ): BookThread?

  public suspend fun updatePosition(
    bookId: BookId,
    threadId: ThreadId,
    chapterId: ChapterId,
    positionInChapter: Long,
    playedAt: Instant? = null,
  )

  public suspend fun reconcileChapters(
    content: BookContent,
    chapterIds: List<ChapterId>,
  )
}

package voice.core.data.repo

import androidx.room.RoomDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.BookThread
import voice.core.data.ChapterId
import voice.core.data.ThreadId
import voice.core.data.repo.internals.dao.BookThreadDao
import voice.core.data.repo.internals.transaction
import java.time.Instant

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
public class BookThreadRepoImpl(
  private val dao: BookThreadDao,
  private val contentRepo: BookContentRepo,
  private val appDb: RoomDatabase,
) : BookThreadRepo {

  private val mutex = Mutex()

  override fun flow(bookId: BookId): Flow<List<BookThread>> = dao.flow(bookId)

  override suspend fun all(bookId: BookId): List<BookThread> = dao.all(bookId)

  override suspend fun initializeBook(content: BookContent): Unit = mutex.withLock {
    val existing = dao.all(content.id)
    if (existing.isNotEmpty()) {
      return@withLock
    }

    val thread = BookThread(
      bookId = content.id,
      id = ThreadId.Default,
      title = null,
      chapterId = content.currentChapter,
      positionInChapter = content.positionInChapter,
      addedAt = content.addedAt,
    )
    appDb.transaction {
      contentRepo.put(content.copy(activeThreadId = thread.id))
      dao.insert(thread)
    }
  }

  override suspend fun createAndActivate(
    bookId: BookId,
    title: String,
    chapterId: ChapterId,
    positionInChapter: Long,
  ): BookThread = mutex.withLock {
    val content = requireNotNull(contentRepo.get(bookId))
    require(chapterId in content.chapters && positionInChapter >= 0)
    val thread = BookThread(
      bookId = bookId,
      id = ThreadId.random(),
      title = title,
      chapterId = chapterId,
      positionInChapter = positionInChapter,
      addedAt = Instant.now(),
    )
    appDb.transaction {
      dao.insert(thread)
      contentRepo.update(bookId) { latest ->
        latest.copy(
          activeThreadId = thread.id,
          currentChapter = chapterId,
          positionInChapter = positionInChapter,
        )
      }
    }
    thread
  }

  override suspend fun activate(
    bookId: BookId,
    threadId: ThreadId,
  ): BookThread? = mutex.withLock {
    val thread = dao.get(bookId, threadId) ?: return@withLock null
    contentRepo.update(bookId) { content ->
      content.copy(
        activeThreadId = thread.id,
        currentChapter = thread.chapterId,
        positionInChapter = thread.positionInChapter,
      )
    }
    thread
  }

  override suspend fun rename(
    bookId: BookId,
    threadId: ThreadId,
    title: String,
  ): Unit = mutex.withLock {
    val thread = dao.get(bookId, threadId) ?: return@withLock
    dao.insert(thread.copy(title = title))
  }

  override suspend fun delete(
    bookId: BookId,
    threadId: ThreadId,
  ): BookThread? = mutex.withLock {
    val remaining = dao.all(bookId).filter { it.id != threadId }
    require(remaining.isNotEmpty()) { "The final thread cannot be deleted" }
    var replacement: BookThread? = null
    appDb.transaction {
      dao.delete(bookId, threadId)
      contentRepo.update(bookId) { content ->
        if (content.activeThreadId != threadId) return@update content
        val selected = remaining.maxBy { it.addedAt }
        replacement = selected
        content.copy(
          activeThreadId = selected.id,
          currentChapter = selected.chapterId,
          positionInChapter = selected.positionInChapter,
        )
      }
    }
    replacement
  }

  override suspend fun updatePosition(
    bookId: BookId,
    threadId: ThreadId,
    chapterId: ChapterId,
    positionInChapter: Long,
    playedAt: Instant?,
  ): Unit = mutex.withLock {
    val thread = dao.get(bookId, threadId) ?: return@withLock
    val content = contentRepo.get(bookId) ?: return@withLock
    if (chapterId !in content.chapters || positionInChapter < 0) return@withLock
    val updatedThread = thread.copy(
      chapterId = chapterId,
      positionInChapter = positionInChapter,
    )
    appDb.transaction {
      dao.insert(updatedThread)
      contentRepo.update(bookId) { latest ->
        if (latest.activeThreadId == threadId) {
          latest.copy(
            currentChapter = chapterId,
            positionInChapter = positionInChapter,
            lastPlayedAt = playedAt ?: latest.lastPlayedAt,
          )
        } else {
          latest
        }
      }
    }
  }

  override suspend fun reconcileChapters(
    content: BookContent,
    chapterIds: List<ChapterId>,
  ): Unit = mutex.withLock {
    require(chapterIds.isNotEmpty())
    val latestContent = contentRepo.get(content.id) ?: content
    val threads = dao.all(content.id).ifEmpty {
      listOf(
        BookThread(
          bookId = content.id,
          id = latestContent.activeThreadId,
          title = null,
          chapterId = latestContent.currentChapter,
          positionInChapter = latestContent.positionInChapter,
          addedAt = latestContent.addedAt,
        ),
      )
    }
    val reconciled = threads.map { thread ->
      if (thread.chapterId in chapterIds) {
        thread
      } else {
        thread.copy(chapterId = chapterIds.first(), positionInChapter = 0)
      }
    }
    val active = reconciled.find { it.id == latestContent.activeThreadId } ?: reconciled.first()
    appDb.transaction {
      reconciled.forEach { dao.insert(it) }
      contentRepo.update(content.id) { latest ->
        latest.copy(
          chapters = chapterIds,
          activeThreadId = active.id,
          currentChapter = active.chapterId,
          positionInChapter = active.positionInChapter,
          isActive = true,
        )
      }
    }
  }
}

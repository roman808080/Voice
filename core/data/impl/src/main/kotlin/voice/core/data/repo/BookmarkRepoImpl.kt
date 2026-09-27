package voice.core.data.repo

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.Bookmark
import voice.core.data.repo.internals.dao.BookmarkDao
import voice.core.data.runForMaxSqlVariableNumber
import voice.core.logging.api.Logger
import java.time.Instant

@ContributesBinding(AppScope::class)
public class BookmarkRepoImpl
internal constructor(
  private val dao: BookmarkDao,
) : BookmarkRepo {

  override suspend fun deleteBookmark(id: Bookmark.Id) {
    dao.deleteBookmark(id)
  }

  override suspend fun addBookmark(bookmark: Bookmark) {
    dao.addBookmark(bookmark)
  }

  override suspend fun addBookmarkAtBookPosition(
    book: Book,
    title: String?,
    setBySleepTimer: Boolean,
  ): Bookmark {
    return withContext(Dispatchers.IO) {
      val bookMark = Bookmark(
        title = title,
        time = book.content.positionInChapter,
        id = Bookmark.Id.random(),
        addedAt = Instant.now(),
        setBySleepTimer = setBySleepTimer,
        chapterId = book.content.currentChapter,
        bookId = book.id,
      )
      addBookmark(bookMark)
      Logger.v("Added bookmark=$bookMark")
      bookMark
    }
  }

  override fun bookmarks(book: BookContent): Flow<List<Bookmark>> {
    val flows = book.chapters.runForMaxSqlVariableNumber { chapterIds ->
      listOf(dao.flowForChapters(chapterIds))
    }
    return when (flows.size) {
      0 -> flowOf(emptyList())
      1 -> flows.single()
      else -> combine(flows) { bookmarks -> bookmarks.flatMap { it } }
    }
  }
}

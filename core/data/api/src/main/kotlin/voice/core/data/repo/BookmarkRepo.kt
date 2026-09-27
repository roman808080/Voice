package voice.core.data.repo

import kotlinx.coroutines.flow.Flow
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.Bookmark

public interface BookmarkRepo {
  public suspend fun deleteBookmark(id: Bookmark.Id)

  public suspend fun addBookmark(bookmark: Bookmark)

  @IgnorableReturnValue
  public suspend fun addBookmarkAtBookPosition(
    book: Book,
    title: String?,
    setBySleepTimer: Boolean,
  ): Bookmark

  public fun bookmarks(book: BookContent): Flow<List<Bookmark>>
}

package voice.features.bookmark

import voice.core.data.Bookmark
import voice.core.data.ThreadId

enum class BookmarkScreenTab {
  Threads,
  Bookmarks,
}

data class ThreadItemViewState(
  val id: ThreadId,
  val title: String,
  val subtitle: String,
  val active: Boolean,
)

data class BookmarkItemViewState(
  val title: String,
  val subtitle: String,
  val id: Bookmark.Id,
  val showSleepIcon: Boolean,
)

data class BookmarkViewState(
  val selectedTab: BookmarkScreenTab,
  val threads: List<ThreadItemViewState>,
  val bookmarks: List<BookmarkItemViewState>,
  val shouldScrollTo: Bookmark.Id?,
  val dialogViewState: BookmarkDialogViewState,
)

sealed interface BookmarkDialogViewState {
  data object None : BookmarkDialogViewState
  data object AddBookmark : BookmarkDialogViewState
  data object CreateThread : BookmarkDialogViewState
  data class EditBookmark(
    val id: Bookmark.Id,
    val title: String?,
  ) : BookmarkDialogViewState

  data class EditThread(
    val id: ThreadId,
    val title: String,
  ) : BookmarkDialogViewState

  data class DeleteThread(
    val id: ThreadId,
    val title: String,
  ) : BookmarkDialogViewState
}

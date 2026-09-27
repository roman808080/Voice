package voice.features.bookmark

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.BookThread
import voice.core.data.Bookmark
import voice.core.data.Chapter
import voice.core.data.KioskModeDemoData
import voice.core.data.ThreadId
import voice.core.data.markForPosition
import voice.core.data.repo.BookRepository
import voice.core.data.repo.BookThreadRepo
import voice.core.data.repo.BookmarkRepo
import voice.core.data.store.BookmarkTabStore
import voice.core.data.store.CurrentBookStore
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.KioskModeFeatureFlagQualifier
import voice.core.playback.CurrentBookResolver
import voice.core.playback.PlayerController
import voice.core.strings.R
import voice.core.ui.formatTime
import voice.navigation.Navigator
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

@AssistedInject
class BookmarkViewModel(
  @CurrentBookStore
  private val currentBookStore: DataStore<BookId?>,
  @BookmarkTabStore
  private val bookmarkTabStore: DataStore<Boolean>,
  private val repo: BookRepository,
  private val threadRepo: BookThreadRepo,
  private val bookmarkRepo: BookmarkRepo,
  private val currentBookResolver: CurrentBookResolver,
  private val playerController: PlayerController,
  private val navigator: Navigator,
  private val context: Context,
  @KioskModeFeatureFlagQualifier
  private val kioskModeFeatureFlag: FeatureFlag<Boolean>,
  @Assisted
  private val bookId: BookId,
) {

  private val scope = MainScope()
  private var bookmarks by mutableStateOf<List<Bookmark>>(emptyList())
  private var threads by mutableStateOf<List<BookThread>>(emptyList())
  private var chapters by mutableStateOf<List<Chapter>>(emptyList())
  private var activeThreadId by mutableStateOf(ThreadId.Default)

  private var shouldScrollTo by mutableStateOf<Bookmark.Id?>(null)
  private var dialogViewState: BookmarkDialogViewState by mutableStateOf(BookmarkDialogViewState.None)

  @Composable
  fun viewState(): BookmarkViewState {
    val threadsSelected by bookmarkTabStore.data.collectAsState(initial = false)
    val selectedTab = if (threadsSelected) BookmarkScreenTab.Threads else BookmarkScreenTab.Bookmarks
    val kioskMode = remember { kioskModeFeatureFlag.get() }
    if (kioskMode) return kioskModeViewState(selectedTab)

    LaunchedEffect(bookId) {
      repo.flow(bookId).collect { book ->
        if (book != null) {
          chapters = book.chapters
          activeThreadId = book.content.activeThreadId
        }
      }
    }
    LaunchedEffect(bookId) {
      repo.flow(bookId)
        .filterNotNull()
        .distinctUntilChangedBy { it.chapters }
        .collectLatest { book ->
          chapters = book.chapters
          bookmarkRepo.bookmarks(book.content).collect { bookmarks ->
            this@BookmarkViewModel.bookmarks = bookmarks.sortedByDescending { it.addedAt }
          }
        }
    }
    LaunchedEffect(bookId) {
      threadRepo.flow(bookId).collect { threads = it }
    }
    return BookmarkViewState(
      selectedTab = selectedTab,
      threads = threads.sortedByDescending { it.addedAt }.map(::threadViewState),
      bookmarks = bookmarks.mapNotNull(::bookmarkViewState),
      shouldScrollTo = shouldScrollTo,
      dialogViewState = dialogViewState,
    )
  }

  private fun threadViewState(thread: BookThread): ThreadItemViewState {
    val chapter = chapters.find { it.id == thread.chapterId }
    val section = chapter?.markForPosition(thread.positionInChapter)?.name ?: chapter?.name
    return ThreadItemViewState(
      id = thread.id,
      title = thread.title ?: context.getString(R.string.bookmark_thread_default_name),
      subtitle = listOfNotNull(section, formatTime(thread.positionInChapter)).joinToString(" - "),
      active = thread.id == activeThreadId,
    )
  }

  private fun bookmarkViewState(bookmark: Bookmark): BookmarkItemViewState? {
    val currentChapter = chapters.firstOrNull { it.id == bookmark.chapterId } ?: return null
    val bookmarkTitle = bookmark.title
    val title: String = when {
      bookmark.setBySleepTimer -> {
        val justNowThreshold = 1.minutes
        if (ChronoUnit.MILLIS.between(bookmark.addedAt, Instant.now()).milliseconds < justNowThreshold) {
          context.getString(R.string.bookmark_created_just_now)
        } else {
          DateUtils.getRelativeDateTimeString(
            context,
            bookmark.addedAt.toEpochMilli(),
            justNowThreshold.inWholeMilliseconds,
            2.days.inWholeMilliseconds,
            0,
          ).toString()
        }
      }
      !bookmarkTitle.isNullOrEmpty() -> bookmarkTitle
      else -> currentChapter.markForPosition(bookmark.time).name ?: ""
    }
    return BookmarkItemViewState(
      title = title,
      subtitle = formatTime(bookmark.time),
      id = bookmark.id,
      showSleepIcon = bookmark.setBySleepTimer,
    )
  }

  private fun kioskModeViewState(selectedTab: BookmarkScreenTab): BookmarkViewState {
    return BookmarkViewState(
      selectedTab = selectedTab,
      threads = listOf(
        ThreadItemViewState(ThreadId.Default, "Default", "Chapter 12 - 10:24:18", active = true),
        ThreadItemViewState(ThreadId("demo-2"), "Thread 2", "Chapter 4 - 03:11:42", active = false),
      ),
      bookmarks = KioskModeDemoData.bookmarkScreen.items.mapIndexed { index, item ->
        BookmarkItemViewState(
          title = item.title,
          subtitle = item.timestamp,
          id = Bookmark.Id(Uuid.parse("00000000-0000-0000-0000-${(index + 1).toString().padStart(12, '0')}")),
          showSleepIcon = false,
        )
      },
      shouldScrollTo = null,
      dialogViewState = BookmarkDialogViewState.None,
    )
  }

  fun selectTab(tab: BookmarkScreenTab) {
    scope.launch {
      bookmarkTabStore.updateData { tab == BookmarkScreenTab.Threads }
    }
  }

  fun deleteBookmark(id: Bookmark.Id) {
    scope.launch {
      bookmarkRepo.deleteBookmark(id)
      bookmarks = bookmarks.filter { it.id != id }
    }
  }

  fun selectBookmark(id: Bookmark.Id) {
    val bookmark = bookmarks.find { it.id == id } ?: return
    scope.launch {
      currentBookStore.updateData { bookId }
      val book = persistLivePosition() ?: return@launch
      threadRepo.updatePosition(
        bookId = bookId,
        threadId = book.content.activeThreadId,
        chapterId = bookmark.chapterId,
        positionInChapter = bookmark.time,
      )
      playerController.reloadCurrentBook()
      navigator.goBack()
    }
  }

  fun selectThread(id: ThreadId) {
    scope.launch {
      if (persistLivePosition() == null) return@launch
      currentBookStore.updateData { bookId }
      val activated = threadRepo.activate(bookId, id) ?: return@launch
      activeThreadId = activated.id
      playerController.reloadCurrentBook()
      navigator.goBack()
    }
  }

  fun createThread(fromCurrentPosition: Boolean) {
    scope.launch {
      val book = persistLivePosition() ?: return@launch
      val source = if (fromCurrentPosition) book else null
      val existingTitles = threads.mapNotNull { it.title }.toSet()
      var number = threads.size + 1
      var title = context.getString(R.string.bookmark_thread_generated_name, number)
      while (title in existingTitles) {
        number++
        title = context.getString(R.string.bookmark_thread_generated_name, number)
      }
      val created = threadRepo.createAndActivate(
        bookId = bookId,
        title = title,
        chapterId = source?.content?.currentChapter ?: book.chapters.first().id,
        positionInChapter = source?.content?.positionInChapter ?: 0,
      )
      activeThreadId = created.id
      currentBookStore.updateData { bookId }
      playerController.reloadCurrentBook()
      navigator.goBack()
    }
  }

  private suspend fun persistLivePosition(): Book? {
    val book = repo.get(bookId) ?: return null
    val live = playerController.livePlaybackState(bookId)
    if (live != null && (live.threadId == null || live.threadId == book.content.activeThreadId)) {
      threadRepo.updatePosition(
        bookId = bookId,
        threadId = book.content.activeThreadId,
        chapterId = live.chapterId,
        positionInChapter = live.positionMs,
      )
    }
    return repo.get(bookId)
  }

  fun renameThread(
    id: ThreadId,
    title: String,
  ) {
    scope.launch { threadRepo.rename(bookId, id, title) }
  }

  fun deleteThread(id: ThreadId) {
    scope.launch {
      val book = persistLivePosition() ?: return@launch
      val deletingActive = book.content.activeThreadId == id
      val replacement = threadRepo.delete(bookId, id)
      if (deletingActive && replacement != null) {
        activeThreadId = replacement.id
        playerController.reloadCurrentBook()
      }
    }
  }

  fun editBookmark(
    id: Bookmark.Id,
    newTitle: String,
  ) {
    scope.launch {
      bookmarks.find { it.id == id }?.let {
        val withNewTitle = it.copy(title = newTitle, setBySleepTimer = false)
        bookmarkRepo.addBookmark(withNewTitle)
        val index = bookmarks.indexOfFirst { bookmarkId -> bookmarkId.id == id }
        bookmarks = bookmarks.toMutableList().apply { this[index] = withNewTitle }
      }
    }
  }

  fun addBookmark(name: String) {
    scope.launch {
      val book = currentBookResolver.book(bookId) ?: return@launch
      val newBookmark = bookmarkRepo.addBookmarkAtBookPosition(
        book = book,
        title = name,
        setBySleepTimer = false,
      )
      bookmarks = (bookmarks + newBookmark).sortedByDescending { it.addedAt }
      shouldScrollTo = newBookmark.id
    }
  }

  fun onScrollConfirm() {
    shouldScrollTo = null
  }

  fun closeDialog() {
    dialogViewState = BookmarkDialogViewState.None
  }

  fun onAddBookmarkClick() {
    dialogViewState = BookmarkDialogViewState.AddBookmark
  }

  fun onAddThreadClick() {
    dialogViewState = BookmarkDialogViewState.CreateThread
  }

  fun onEditBookmarkClick(id: Bookmark.Id) {
    val bookmark = bookmarks.find { it.id == id } ?: return
    dialogViewState = BookmarkDialogViewState.EditBookmark(id, bookmark.title)
  }

  fun onEditThreadClick(id: ThreadId) {
    val thread = threads.find { it.id == id } ?: return
    dialogViewState = BookmarkDialogViewState.EditThread(
      id = id,
      title = thread.title ?: context.getString(R.string.bookmark_thread_default_name),
    )
  }

  fun onDeleteThreadClick(id: ThreadId) {
    if (threads.size <= 1) return
    val thread = threads.find { it.id == id } ?: return
    dialogViewState = BookmarkDialogViewState.DeleteThread(
      id = id,
      title = thread.title ?: context.getString(R.string.bookmark_thread_default_name),
    )
  }

  fun closeScreen() {
    navigator.goBack()
  }

  @AssistedFactory
  interface Factory {
    fun create(bookId: BookId): BookmarkViewModel
  }
}

package voice.features.bookmark

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.data.Bookmark
import voice.core.data.ThreadId
import voice.core.ui.icons.VoiceIcons
import voice.features.bookmark.dialogs.AddBookmarkDialog
import voice.features.bookmark.dialogs.CreateThreadDialog
import voice.features.bookmark.dialogs.DeleteThreadDialog
import voice.features.bookmark.dialogs.EditBookmarkDialog
import voice.features.bookmark.dialogs.EditThreadDialog
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import kotlin.uuid.Uuid
import voice.core.strings.R as StringsR

@ContributesTo(AppScope::class)
interface Graph {
  val bookmarkViewModelFactory: BookmarkViewModel.Factory
}

@ContributesTo(AppScope::class)
interface BookmarkProvider {

  @Provides
  @IntoSet
  fun bookmarkNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.Bookmarks> { key ->
    NavEntry(key) {
      BookmarkScreen(bookId = key.bookId)
    }
  }
}

@Composable
fun BookmarkScreen(bookId: BookId) {
  val viewModel = retain(bookId.value) {
    rootGraphAs<Graph>().bookmarkViewModelFactory.create(bookId)
  }
  BookmarkScreen(
    viewState = viewModel.viewState(),
    onClose = viewModel::closeScreen,
    onTabSelect = viewModel::selectTab,
    onAddBookmark = viewModel::onAddBookmarkClick,
    onAddThread = viewModel::onAddThreadClick,
    onDeleteBookmark = viewModel::deleteBookmark,
    onEditBookmark = viewModel::onEditBookmarkClick,
    onBookmarkClick = viewModel::selectBookmark,
    onThreadClick = viewModel::selectThread,
    onEditThread = viewModel::onEditThreadClick,
    onDeleteThread = viewModel::onDeleteThreadClick,
    onScrollConfirm = viewModel::onScrollConfirm,
    onNewBookmarkNameChoose = viewModel::addBookmark,
    onCloseDialog = viewModel::closeDialog,
    onEditBookmarkConfirm = viewModel::editBookmark,
    onCreateThread = viewModel::createThread,
    onRenameThread = viewModel::renameThread,
    onDeleteThreadConfirm = viewModel::deleteThread,
  )
}

@Composable
internal fun BookmarkScreen(
  viewState: BookmarkViewState,
  onClose: () -> Unit,
  onTabSelect: (BookmarkScreenTab) -> Unit,
  onAddBookmark: () -> Unit,
  onAddThread: () -> Unit,
  onDeleteBookmark: (Bookmark.Id) -> Unit,
  onEditBookmark: (Bookmark.Id) -> Unit,
  onBookmarkClick: (Bookmark.Id) -> Unit,
  onThreadClick: (ThreadId) -> Unit,
  onEditThread: (ThreadId) -> Unit,
  onDeleteThread: (ThreadId) -> Unit,
  onScrollConfirm: () -> Unit,
  onCloseDialog: () -> Unit,
  onNewBookmarkNameChoose: (String) -> Unit,
  onEditBookmarkConfirm: (Bookmark.Id, String) -> Unit,
  onCreateThread: (Boolean) -> Unit,
  onRenameThread: (ThreadId, String) -> Unit,
  onDeleteThreadConfirm: (ThreadId) -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text(stringResource(StringsR.string.bookmark_threads_title)) },
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(
              imageVector = VoiceIcons.Close,
              contentDescription = stringResource(StringsR.string.common_action_close),
            )
          }
        },
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = if (viewState.selectedTab == BookmarkScreenTab.Threads) onAddThread else onAddBookmark,
      ) {
        Icon(
          imageVector = VoiceIcons.Add,
          contentDescription = stringResource(StringsR.string.common_action_add),
        )
      }
    },
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
    ) {
      BookmarkTabs(
        selectedTab = viewState.selectedTab,
        onTabSelect = onTabSelect,
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
          .padding(horizontal = 16.dp, vertical = 8.dp),
      )
      when (viewState.selectedTab) {
        BookmarkScreenTab.Threads -> ThreadList(
          threads = viewState.threads,
          onClick = onThreadClick,
          onEdit = onEditThread,
          onDelete = onDeleteThread,
          modifier = Modifier.weight(1F),
        )
        BookmarkScreenTab.Bookmarks -> BookmarkList(
          viewState = viewState,
          onDelete = onDeleteBookmark,
          onEdit = onEditBookmark,
          onClick = onBookmarkClick,
          onScrollConfirm = onScrollConfirm,
          modifier = Modifier.weight(1F),
        )
      }
    }
  }

  when (val dialog = viewState.dialogViewState) {
    BookmarkDialogViewState.AddBookmark -> AddBookmarkDialog(
      onDismissRequest = onCloseDialog,
      onBookmarkNameChoose = onNewBookmarkNameChoose,
    )
    BookmarkDialogViewState.CreateThread -> CreateThreadDialog(
      onDismissRequest = onCloseDialog,
      onCreateFromBeginning = { onCreateThread(false) },
      onForkCurrentPosition = { onCreateThread(true) },
    )
    BookmarkDialogViewState.None -> Unit
    is BookmarkDialogViewState.EditBookmark -> EditBookmarkDialog(
      onDismissRequest = onCloseDialog,
      onEditBookmark = onEditBookmarkConfirm,
      bookmarkId = dialog.id,
      initialTitle = dialog.title ?: "",
    )
    is BookmarkDialogViewState.EditThread -> EditThreadDialog(
      threadId = dialog.id,
      initialTitle = dialog.title,
      onDismissRequest = onCloseDialog,
      onRename = onRenameThread,
    )
    is BookmarkDialogViewState.DeleteThread -> DeleteThreadDialog(
      threadId = dialog.id,
      title = dialog.title,
      onDismissRequest = onCloseDialog,
      onDelete = onDeleteThreadConfirm,
    )
  }
}

@Composable
private fun BookmarkTabs(
  selectedTab: BookmarkScreenTab,
  onTabSelect: (BookmarkScreenTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  SingleChoiceSegmentedButtonRow(modifier) {
    val tabs = listOf(
      BookmarkScreenTab.Threads to stringResource(StringsR.string.bookmark_threads_tab_threads),
      BookmarkScreenTab.Bookmarks to stringResource(StringsR.string.bookmark_threads_tab_bookmarks),
    )
    tabs.forEachIndexed { index, (tab, label) ->
      SegmentedButton(
        selected = selectedTab == tab,
        onClick = { onTabSelect(tab) },
        shape = SegmentedButtonDefaults.itemShape(index, tabs.size),
      ) {
        Text(label)
      }
    }
  }
}

@Composable
private fun ThreadList(
  threads: List<ThreadItemViewState>,
  onClick: (ThreadId) -> Unit,
  onEdit: (ThreadId) -> Unit,
  onDelete: (ThreadId) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(modifier) {
    items(threads, key = { it.id.value }) { thread ->
      ThreadItem(
        thread = thread,
        canDelete = threads.size > 1,
        onClick = onClick,
        onEdit = onEdit,
        onDelete = onDelete,
        modifier = Modifier.animateItem(),
      )
    }
    item { Spacer(Modifier.size(88.dp)) }
  }
}

@Composable
private fun ThreadItem(
  thread: ThreadItemViewState,
  canDelete: Boolean,
  onClick: (ThreadId) -> Unit,
  onEdit: (ThreadId) -> Unit,
  onDelete: (ThreadId) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  val activeDescription = stringResource(StringsR.string.bookmark_thread_active)
  ListItem(
    modifier = modifier
      .semantics {
        selected = thread.active
        if (thread.active) stateDescription = activeDescription
      }
      .clickable { onClick(thread.id) },
    colors = ListItemDefaults.colors(
      containerColor = if (thread.active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
    ),
    leadingContent = if (thread.active) {
      { Icon(VoiceIcons.Check, contentDescription = activeDescription) }
    } else {
      null
    },
    supportingContent = { Text(thread.subtitle) },
    trailingContent = {
      Box {
        IconButton(onClick = { expanded = true }) {
          Icon(
            VoiceIcons.MoreVert,
            contentDescription = stringResource(StringsR.string.common_action_more),
          )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
          DropdownMenuItem(
            text = { Text(stringResource(StringsR.string.common_action_edit)) },
            onClick = {
              expanded = false
              onEdit(thread.id)
            },
          )
          if (canDelete) {
            DropdownMenuItem(
              text = { Text(stringResource(StringsR.string.common_action_remove)) },
              onClick = {
                expanded = false
                onDelete(thread.id)
              },
            )
          }
        }
      }
    },
  ) {
    Text(thread.title)
  }
}

@Composable
private fun BookmarkList(
  viewState: BookmarkViewState,
  onDelete: (Bookmark.Id) -> Unit,
  onEdit: (Bookmark.Id) -> Unit,
  onClick: (Bookmark.Id) -> Unit,
  onScrollConfirm: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val lazyListState = rememberLazyListState()
  LaunchedEffect(viewState.shouldScrollTo, onScrollConfirm) {
    val index = viewState.bookmarks.indexOfFirst { it.id == viewState.shouldScrollTo }
    if (index != -1) {
      lazyListState.animateScrollToItem(index)
      onScrollConfirm()
    }
  }
  LazyColumn(modifier = modifier, state = lazyListState) {
    items(viewState.bookmarks, key = { it.id.value.toString() }) { bookmark ->
      BookmarkItem(
        modifier = Modifier.animateItem(),
        bookmark = bookmark,
        onDelete = onDelete,
        onEdit = onEdit,
        onClick = onClick,
      )
    }
    item { Spacer(Modifier.size(88.dp)) }
  }
}

@Composable
internal fun BookmarkItem(
  bookmark: BookmarkItemViewState,
  onDelete: (Bookmark.Id) -> Unit,
  onEdit: (Bookmark.Id) -> Unit,
  onClick: (Bookmark.Id) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  SwipeToDismissBox(
    modifier = modifier,
    onDismiss = {
      if (it == SwipeToDismissBoxValue.StartToEnd) onDelete(bookmark.id)
    },
    enableDismissFromEndToStart = false,
    backgroundContent = {
      Box(Modifier.fillMaxSize().background(Color.Red)) {
        Icon(
          modifier = Modifier.padding(start = 16.dp).align(Alignment.CenterStart),
          imageVector = VoiceIcons.Delete,
          contentDescription = stringResource(StringsR.string.common_action_delete),
          tint = Color.White,
        )
      }
    },
    state = rememberSwipeToDismissBoxState(),
  ) {
    ListItem(
      modifier = Modifier.clickable { onClick(bookmark.id) },
      trailingContent = {
        Box {
          IconButton(onClick = { expanded = true }) {
            Icon(
              imageVector = VoiceIcons.MoreVert,
              contentDescription = stringResource(StringsR.string.common_action_more),
            )
          }
          DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
              text = { Text(stringResource(StringsR.string.common_action_edit)) },
              onClick = {
                expanded = false
                onEdit(bookmark.id)
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(StringsR.string.common_action_remove)) },
              onClick = {
                expanded = false
                onDelete(bookmark.id)
              },
            )
          }
        }
      },
      supportingContent = { Text(bookmark.subtitle) },
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(bookmark.title)
        if (bookmark.showSleepIcon) {
          Icon(
            modifier = Modifier.padding(start = 4.dp).size(16.dp),
            imageVector = VoiceIcons.Timer,
            contentDescription = stringResource(StringsR.string.sleep_timer_action_open),
          )
        }
      }
    }
  }
}

@Composable
@Preview
private fun BookmarkItemPreview() {
  BookmarkItem(
    bookmark = BookmarkItemViewState(
      title = "Bookmark 1",
      subtitle = "10:10:10",
      id = Bookmark.Id(Uuid.random()),
      showSleepIcon = true,
    ),
    onDelete = {},
    onEdit = {},
    onClick = {},
  )
}

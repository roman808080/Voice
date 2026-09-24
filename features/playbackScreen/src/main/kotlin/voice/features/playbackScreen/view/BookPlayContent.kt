package voice.features.playbackScreen.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.features.playbackScreen.BookPlayViewState
import kotlin.time.Duration

@Composable
internal fun BookPlayContent(
  contentPadding: PaddingValues,
  viewState: BookPlayViewState,
  bookId: BookId,
  onPlayClick: () -> Unit,
  onRewindClick: () -> Unit,
  onFastForwardClick: () -> Unit,
  onSeek: (Duration) -> Unit,
  onSkipToNext: () -> Unit,
  onSkipToPrevious: () -> Unit,
  onCurrentChapterClick: () -> Unit,
  onSubtitleClick: (ChapterId, Duration) -> Unit,
  useLandscapeLayout: Boolean,
) {
  var showTranscript by rememberSaveable { mutableStateOf(false) }
  var autoSynchronizeTranscript by rememberSaveable { mutableStateOf(true) }
  val transcriptListState = rememberLazyListState()
  val transcriptVisible = showTranscript && viewState.transcriptCues.isNotEmpty()
  val currentCueIndex = viewState.currentTranscriptCueIndex
  val scope = rememberCoroutineScope()
  LaunchedEffect(transcriptVisible, viewState.transcriptSectionId) {
    if (transcriptVisible && currentCueIndex != null) {
      transcriptListState.scrollToItem(currentCueIndex)
    }
  }
  val onJumpToCurrentSubtitle: (() -> Unit)? = if (transcriptVisible && currentCueIndex != null) {
    {
      scope.launch {
        transcriptListState.animateScrollToItem(currentCueIndex)
      }
    }
  } else {
    null
  }

  if (useLandscapeLayout) {
    Row(Modifier.padding(contentPadding)) {
      PlaybackMediaPane(
        bookId = bookId,
        onPlayClick = onPlayClick,
        viewState = viewState,
        showTranscript = showTranscript,
        onShowTranscriptChange = { showTranscript = it },
        autoSynchronizeTranscript = autoSynchronizeTranscript,
        onAutoSynchronizeTranscriptChange = { autoSynchronizeTranscript = it },
        transcriptListState = transcriptListState,
        onJumpToCurrentSubtitle = onJumpToCurrentSubtitle,
        onSubtitleClick = onSubtitleClick,
        modifier = Modifier
          .fillMaxHeight()
          .weight(1F)
          .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
      )
      Column(
        modifier = Modifier
          .fillMaxHeight()
          .weight(1F),
        verticalArrangement = Arrangement.Center,
      ) {
        viewState.chapterName?.let { chapterName ->
          ChapterRow(
            chapterName = chapterName,
            nextPreviousVisible = viewState.showPreviousNextButtons,
            onSkipToNext = onSkipToNext,
            onSkipToPrevious = onSkipToPrevious,
            onCurrentChapterClick = onCurrentChapterClick,
          )
        }
        Spacer(modifier = Modifier.size(20.dp))
        SliderRow(
          duration = viewState.duration,
          playedTime = viewState.playedTime,
          onSeek = onSeek,
        )
        Spacer(modifier = Modifier.size(16.dp))
        PlaybackRow(
          playing = viewState.playing,
          onPlayClick = onPlayClick,
          onRewindClick = onRewindClick,
          onFastForwardClick = onFastForwardClick,
        )
      }
    }
  } else {
    Column(Modifier.padding(contentPadding)) {
      PlaybackMediaPane(
        bookId = bookId,
        onPlayClick = onPlayClick,
        viewState = viewState,
        showTranscript = showTranscript,
        onShowTranscriptChange = { showTranscript = it },
        autoSynchronizeTranscript = autoSynchronizeTranscript,
        onAutoSynchronizeTranscriptChange = { autoSynchronizeTranscript = it },
        transcriptListState = transcriptListState,
        onJumpToCurrentSubtitle = onJumpToCurrentSubtitle,
        onSubtitleClick = onSubtitleClick,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1F)
          .padding(start = 16.dp, end = 16.dp, top = 8.dp),
      )
      viewState.chapterName?.let { chapterName ->
        Spacer(modifier = Modifier.size(16.dp))
        ChapterRow(
          chapterName = chapterName,
          nextPreviousVisible = viewState.showPreviousNextButtons,
          onSkipToNext = onSkipToNext,
          onSkipToPrevious = onSkipToPrevious,
          onCurrentChapterClick = onCurrentChapterClick,
        )
      }
      Spacer(modifier = Modifier.size(20.dp))
      SliderRow(
        duration = viewState.duration,
        playedTime = viewState.playedTime,
        onSeek = onSeek,
      )
      Spacer(modifier = Modifier.size(16.dp))
      PlaybackRow(
        playing = viewState.playing,
        onPlayClick = onPlayClick,
        onRewindClick = onRewindClick,
        onFastForwardClick = onFastForwardClick,
      )
      Spacer(modifier = Modifier.size(24.dp))
    }
  }
}

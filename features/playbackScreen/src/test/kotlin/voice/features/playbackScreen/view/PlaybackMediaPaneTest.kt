package voice.features.playbackScreen.view

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.ui.VoiceTheme
import voice.features.playbackScreen.BookPlayViewState
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
class PlaybackMediaPaneTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun `switches from cover to transcript and seeks selected cue`() {
    var showTranscript by mutableStateOf(false)
    var selectedPosition: Duration? = null

    composeRule.setContent {
      VoiceTheme {
        PlaybackMediaPane(
          bookId = BookId("book"),
          viewState = viewState(),
          showTranscript = showTranscript,
          onShowTranscriptChange = { showTranscript = it },
          autoSynchronizeTranscript = false,
          transcriptListState = rememberLazyListState(),
          onPlayClick = {},
          onSubtitleClick = { _, position -> selectedPosition = position },
          modifier = Modifier,
        )
      }
    }

    composeRule.onNodeWithText("Cover").assertIsSelected()
    composeRule.onNodeWithText("Current cue").assertDoesNotExist()

    composeRule.onNodeWithText("Transcript").performClick().assertIsSelected()
    composeRule.onNode(hasText("Current cue") and isSelected())
      .assertIsDisplayed()
      .performClick()

    composeRule.runOnIdle {
      assertEquals(expected = 20.seconds, actual = selectedPosition)
    }
  }

  @Test
  fun `long pressing transcript text does not seek the cue`() {
    var selectedPosition: Duration? = null

    composeRule.setContent {
      VoiceTheme {
        PlaybackMediaPane(
          bookId = BookId("book"),
          viewState = viewState(),
          showTranscript = true,
          onShowTranscriptChange = {},
          autoSynchronizeTranscript = false,
          transcriptListState = rememberLazyListState(),
          onPlayClick = {},
          onSubtitleClick = { _, position -> selectedPosition = position },
        )
      }
    }

    composeRule.onNodeWithText("Current cue").performTouchInput {
      longClick()
    }

    composeRule.runOnIdle {
      assertEquals(expected = null, actual = selectedPosition)
    }
  }

  @Test
  fun `auto synchronization scrolls to an invisible current cue`() {
    var viewState by mutableStateOf(scrollingViewState(currentCueIndex = 0))
    lateinit var listState: androidx.compose.foundation.lazy.LazyListState

    composeRule.setContent {
      VoiceTheme {
        Box(Modifier.width(320.dp)) {
          listState = rememberLazyListState()
          PlaybackMediaPane(
            bookId = BookId("book"),
            viewState = viewState,
            showTranscript = true,
            onShowTranscriptChange = {},
            autoSynchronizeTranscript = true,
            transcriptListState = listState,
            onPlayClick = {},
            onSubtitleClick = { _, _ -> },
          )
        }
      }
    }

    composeRule.runOnIdle {
      viewState = scrollingViewState(currentCueIndex = 15)
    }
    composeRule.runOnIdle {
      assertEquals(expected = 15, actual = listState.firstVisibleItemIndex)
    }
    composeRule.onNodeWithText("Cue 15").assertIsDisplayed()
  }

  @Test
  fun `auto synchronization fully reveals a partially visible current cue`() {
    var autoSynchronize by mutableStateOf(false)
    lateinit var listState: androidx.compose.foundation.lazy.LazyListState

    composeRule.setContent {
      VoiceTheme {
        Box(Modifier.width(320.dp)) {
          listState = rememberLazyListState(
            initialFirstVisibleItemIndex = 15,
            initialFirstVisibleItemScrollOffset = 1,
          )
          PlaybackMediaPane(
            bookId = BookId("book"),
            viewState = scrollingViewState(currentCueIndex = 15),
            showTranscript = true,
            onShowTranscriptChange = {},
            autoSynchronizeTranscript = autoSynchronize,
            transcriptListState = listState,
            onPlayClick = {},
            onSubtitleClick = { _, _ -> },
          )
        }
      }
    }

    composeRule.runOnIdle {
      assertEquals(expected = 1, actual = listState.firstVisibleItemScrollOffset)
      autoSynchronize = true
    }
    composeRule.runOnIdle {
      assertEquals(expected = 0, actual = listState.firstVisibleItemScrollOffset)
    }
  }

  @Test
  fun `playback row no longer contains transcript navigation`() {
    composeRule.setContent {
      VoiceTheme {
        Box(Modifier.width(240.dp)) {
          PlaybackRow(
            playing = true,
            onPlayClick = {},
            onRewindClick = {},
            onFastForwardClick = {},
          )
        }
      }
    }

    composeRule.onNodeWithContentDescription("Rewind").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Fast forward").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Jump to current subtitle").assertDoesNotExist()
  }

  private fun viewState() = BookPlayViewState(
    chapterName = "Chapter",
    showPreviousNextButtons = true,
    title = "Book",
    sleepTimerState = BookPlayViewState.SleepTimerViewState.Disabled,
    playedTime = 20.seconds,
    duration = 1.minutes,
    playing = true,
    cover = null,
    skipSilence = false,
    transcriptCues = listOf(
      BookPlayViewState.TranscriptCue(
        chapterId = ChapterId("chapter"),
        text = "Earlier cue",
        timestamp = "0:05",
        position = 5.seconds,
        endPosition = 10.seconds,
      ),
      BookPlayViewState.TranscriptCue(
        chapterId = ChapterId("chapter"),
        text = "Current cue",
        timestamp = "0:20",
        position = 20.seconds,
        endPosition = 25.seconds,
      ),
    ),
    activeTranscriptCueIndices = setOf(1),
    currentTranscriptCueIndex = 1,
    transcriptSectionId = "chapter:0",
  )

  private fun scrollingViewState(currentCueIndex: Int) = BookPlayViewState(
    chapterName = "Chapter",
    showPreviousNextButtons = true,
    title = "Book",
    sleepTimerState = BookPlayViewState.SleepTimerViewState.Disabled,
    playedTime = (currentCueIndex * 5).seconds,
    duration = 3.minutes,
    playing = true,
    cover = null,
    skipSilence = false,
    transcriptCues = (0 until 30).map { index ->
      BookPlayViewState.TranscriptCue(
        chapterId = ChapterId("chapter"),
        text = "Cue $index",
        timestamp = "0:${(index * 5).toString().padStart(2, '0')}",
        position = (index * 5).seconds,
        endPosition = (index * 5 + 4).seconds,
      )
    },
    activeTranscriptCueIndices = setOf(currentCueIndex),
    currentTranscriptCueIndex = currentCueIndex,
    transcriptSectionId = "chapter:0",
  )
}

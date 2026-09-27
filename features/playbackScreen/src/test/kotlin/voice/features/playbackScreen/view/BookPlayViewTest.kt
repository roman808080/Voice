package voice.features.playbackScreen.view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.ui.VoiceTheme
import voice.features.playbackScreen.BookPlayViewState
import kotlin.time.Duration.Companion.minutes

@RunWith(RobolectricTestRunner::class)
class BookPlayViewTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun `auto synchronization displays persisted state and can be toggled from overflow menu`() {
    var viewState by mutableStateOf(viewState())
    composeRule.setContent {
      VoiceTheme {
        BookPlayView(
          viewState = viewState,
          bookId = BookId("book"),
          onPlayClick = {},
          onRewindClick = {},
          onFastForwardClick = {},
          onSeek = {},
          onSleepTimerClick = {},
          onBookmarkClick = {},
          onBookmarkLongClick = {},
          onSpeedChangeClick = {},
          onSkipSilenceClick = {},
          onVolumeBoostClick = {},
          onSkipToNext = {},
          onSkipToPrevious = {},
          onCloseClick = {},
          onCurrentChapterClick = {},
          onSubtitleClick = { _, _ -> },
          onExplainTranscriptPhrase = { _, _ -> },
          onAutoSynchronizeTranscriptClick = {
            viewState = viewState.copy(autoSynchronizeTranscript = !viewState.autoSynchronizeTranscript)
          },
          onShowTranscriptChange = {},
          useLandscapeLayout = false,
        )
      }
    }

    composeRule.onNodeWithContentDescription("More").performClick()
    composeRule.onNodeWithText("Automatically follow current subtitle").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Automatically follow current subtitle")
      .assertIsOn()
      .performClick()

    composeRule.onNodeWithContentDescription("More").performClick()
    composeRule.onNodeWithContentDescription("Automatically follow current subtitle").assertIsOff()
  }

  @Test
  fun `persisted transcript selection is displayed`() {
    composeRule.setContent {
      VoiceTheme {
        BookPlayView(
          viewState = viewState(showTranscript = true),
          bookId = BookId("book"),
          onPlayClick = {},
          onRewindClick = {},
          onFastForwardClick = {},
          onSeek = {},
          onSleepTimerClick = {},
          onBookmarkClick = {},
          onBookmarkLongClick = {},
          onSpeedChangeClick = {},
          onSkipSilenceClick = {},
          onVolumeBoostClick = {},
          onSkipToNext = {},
          onSkipToPrevious = {},
          onCloseClick = {},
          onCurrentChapterClick = {},
          onSubtitleClick = { _, _ -> },
          onExplainTranscriptPhrase = { _, _ -> },
          onAutoSynchronizeTranscriptClick = {},
          onShowTranscriptChange = {},
          useLandscapeLayout = false,
        )
      }
    }

    composeRule.onNodeWithText("Transcript").assertIsSelected()
    composeRule.onNodeWithText("Current cue").assertIsDisplayed()
  }

  private fun viewState(
    autoSynchronizeTranscript: Boolean = true,
    showTranscript: Boolean = false,
  ) = BookPlayViewState(
    chapterName = "Chapter",
    showPreviousNextButtons = true,
    title = "Book",
    sleepTimerState = BookPlayViewState.SleepTimerViewState.Disabled,
    playedTime = 1.minutes,
    duration = 10.minutes,
    playing = true,
    cover = null,
    skipSilence = false,
    autoSynchronizeTranscript = autoSynchronizeTranscript,
    showTranscript = showTranscript,
    transcriptCues = listOf(
      BookPlayViewState.TranscriptCue(
        chapterId = ChapterId("chapter"),
        text = "Current cue",
        timestamp = "1:00",
        position = 1.minutes,
        endPosition = 2.minutes,
      ),
    ),
    activeTranscriptCueIndices = setOf(0),
    currentTranscriptCueIndex = 0,
    transcriptSectionId = "chapter:0",
  )
}

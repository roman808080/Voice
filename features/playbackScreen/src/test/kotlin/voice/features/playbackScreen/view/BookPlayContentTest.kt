package voice.features.playbackScreen.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsOn
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
class BookPlayContentTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun `auto synchronization is enabled by default`() {
    composeRule.setContent {
      VoiceTheme {
        BookPlayContent(
          contentPadding = PaddingValues(),
          viewState = viewState(),
          bookId = BookId("book"),
          onPlayClick = {},
          onRewindClick = {},
          onFastForwardClick = {},
          onSeek = {},
          onSkipToNext = {},
          onSkipToPrevious = {},
          onCurrentChapterClick = {},
          onSubtitleClick = { _, _ -> },
          useLandscapeLayout = false,
        )
      }
    }

    composeRule.onNodeWithText("Transcript").performClick()
    composeRule.onNodeWithContentDescription("Automatically follow current subtitle").assertIsOn()
  }

  private fun viewState() = BookPlayViewState(
    chapterName = "Chapter",
    showPreviousNextButtons = true,
    title = "Book",
    sleepTimerState = BookPlayViewState.SleepTimerViewState.Disabled,
    playedTime = 1.minutes,
    duration = 10.minutes,
    playing = true,
    cover = null,
    skipSilence = false,
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

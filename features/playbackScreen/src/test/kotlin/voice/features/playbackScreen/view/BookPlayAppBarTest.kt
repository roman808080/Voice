package voice.features.playbackScreen.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import voice.core.ui.VoiceTheme
import voice.features.playbackScreen.BookPlayViewState
import kotlin.time.Duration.Companion.minutes

@RunWith(RobolectricTestRunner::class)
class BookPlayAppBarTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun `does not display selected book title`() {
    composeRule.setContent {
      VoiceTheme {
        BookPlayAppBar(
          viewState = viewState(),
          onSleepTimerClick = {},
          onBookmarkClick = {},
          onBookmarkLongClick = {},
          onSpeedChangeClick = {},
          onSkipSilenceClick = {},
          onVolumeBoostClick = {},
          onCloseClick = {},
        )
      }
    }

    composeRule.onNodeWithText("Selected book").assertDoesNotExist()
    composeRule.onNodeWithContentDescription("Close").assertIsDisplayed()
  }

  private fun viewState() = BookPlayViewState(
    chapterName = "Chapter",
    showPreviousNextButtons = true,
    title = "Selected book",
    sleepTimerState = BookPlayViewState.SleepTimerViewState.Disabled,
    playedTime = 1.minutes,
    duration = 10.minutes,
    playing = true,
    cover = null,
    skipSilence = false,
  )
}

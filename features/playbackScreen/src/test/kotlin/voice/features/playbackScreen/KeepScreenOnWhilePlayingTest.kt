package voice.features.playbackScreen

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class KeepScreenOnWhilePlayingTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun `keeps screen on only while playing and composed`() {
    var isPlaying by mutableStateOf(false)
    var isVisible by mutableStateOf(true)
    lateinit var view: View

    composeRule.setContent {
      view = LocalView.current
      if (isVisible) {
        KeepScreenOnWhilePlaying(isPlaying)
      }
    }

    composeRule.runOnIdle {
      assertFalse(view.keepScreenOn)
      isPlaying = true
    }
    composeRule.runOnIdle {
      assertTrue(view.keepScreenOn)
      isPlaying = false
    }
    composeRule.runOnIdle {
      assertFalse(view.keepScreenOn)
      isPlaying = true
    }
    composeRule.runOnIdle {
      assertTrue(view.keepScreenOn)
      isVisible = false
    }
    composeRule.runOnIdle {
      assertFalse(view.keepScreenOn)
    }
  }
}

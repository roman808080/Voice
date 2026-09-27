package voice.features.playbackScreen

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import voice.core.strings.R as StringsR
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class TranscriptExplanationTest {

  @Test
  fun `prompt identifies the selection and its transcript context`() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    val prompt = context.getString(
      StringsR.string.playback_transcript_explain_prompt,
      "on cloud nine",
      "After the news, she was on cloud nine all day.",
    )

    assertEquals(
      """
      Explain the selected phrase in the context of this audiobook transcript. Include its meaning and any relevant idiom or grammar.

      Selected phrase:
      on cloud nine

      Transcript context:
      After the news, she was on cloud nine all day.
      """.trimIndent(),
      prompt,
    )
  }

  @Test
  fun `creates untargeted plain text chooser`() {
    val chooser = transcriptExplanationIntent(
      prompt = "Explain this phrase.",
      chooserTitle = "Explain with",
    )

    assertEquals(Intent.ACTION_CHOOSER, chooser.action)
    assertEquals("Explain with", chooser.getCharSequenceExtra(Intent.EXTRA_TITLE))

    @Suppress("DEPRECATION")
    val sendIntent = requireNotNull(chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))
    assertEquals(Intent.ACTION_SEND, sendIntent.action)
    assertEquals("text/plain", sendIntent.type)
    assertEquals("Explain this phrase.", sendIntent.getStringExtra(Intent.EXTRA_TEXT))
    assertNull(sendIntent.`package`)
    assertNull(sendIntent.component)
  }
}

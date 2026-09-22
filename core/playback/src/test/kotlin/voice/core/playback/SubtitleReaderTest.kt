package voice.core.playback

import androidx.media3.common.util.UnstableApi
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@UnstableApi
@RunWith(RobolectricTestRunner::class)
class SubtitleReaderTest {

  @Test
  fun `parses all timed multiline cues`() {
    val subtitles = """
      1
      00:00:01,250 --> 00:00:03,500
      First line
      Second line

      2
      00:00:03,000 --> 00:00:04,000
      Repeated

      3
      00:00:04,000 --> 00:00:05,250
      Repeated
    """.trimIndent()

    assertEquals(
      expected = listOf(
        SubtitleCue(startMs = 1_250, endMs = 3_500, text = "First line\nSecond line"),
        SubtitleCue(startMs = 3_000, endMs = 4_000, text = "Repeated"),
        SubtitleCue(startMs = 4_000, endMs = 5_250, text = "Repeated"),
      ),
      actual = parseSubrip(subtitles.encodeToByteArray()),
    )
  }

  @Test
  fun `ignores blank cues`() {
    val subtitles = """
      1
      00:00:01,000 --> 00:00:02,000


      2
      00:00:02,000 --> 00:00:03,000
      Visible
    """.trimIndent()

    assertEquals(
      expected = listOf(SubtitleCue(startMs = 2_000, endMs = 3_000, text = "Visible")),
      actual = parseSubrip(subtitles.encodeToByteArray()),
    )
  }
}

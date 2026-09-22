package voice.core.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.text.subrip.SubripParser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import voice.core.common.DispatcherProvider
import voice.core.logging.api.Logger

data class SubtitleCue(
  val startMs: Long,
  val endMs: Long,
  val text: String,
)

@Inject
class SubtitleReader(
  private val context: Context,
  private val dispatcherProvider: DispatcherProvider,
) {

  suspend fun read(uri: Uri?): List<SubtitleCue> {
    if (uri == null) return emptyList()

    return withContext(dispatcherProvider.io) {
      try {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
          ?: return@withContext emptyList()
        parseSubrip(bytes)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Logger.w(e, "Could not read subtitles from $uri")
        emptyList()
      }
    }
  }
}

@UnstableApi
internal fun parseSubrip(data: ByteArray): List<SubtitleCue> {
  val result = mutableListOf<SubtitleCue>()
  SubripParser().parse(data, SubtitleParser.OutputOptions.allCues()) { timedCues ->
    result += timedCues.toSubtitleCues()
  }
  return result.sortedBy(SubtitleCue::startMs)
}

private fun CuesWithTiming.toSubtitleCues(): List<SubtitleCue> {
  val startMs = startTimeUs / 1_000
  val endMs = endTimeUs / 1_000
  if (startMs < 0 || endMs <= startMs) return emptyList()

  return cues.mapNotNull { cue ->
    val text = cue.text?.toString()?.takeUnless(String::isBlank) ?: return@mapNotNull null
    SubtitleCue(
      startMs = startMs,
      endMs = endMs,
      text = text,
    )
  }
}

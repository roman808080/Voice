package voice.features.playbackScreen

import androidx.compose.runtime.Immutable
import voice.core.data.ChapterId
import voice.core.playback.misc.Decibel
import voice.features.sleepTimer.SleepTimerViewState
import kotlin.time.Duration

@Immutable
data class BookPlayViewState(
  val chapterName: String?,
  val showPreviousNextButtons: Boolean,
  val title: String,
  val sleepTimerState: SleepTimerViewState,
  val playedTime: Duration,
  val duration: Duration,
  val playing: Boolean,
  val cover: String?,
  val skipSilence: Boolean,
  val subtitles: List<String> = emptyList(),
  val transcriptCues: List<TranscriptCue> = emptyList(),
  val activeTranscriptCueIndices: Set<Int> = emptySet(),
  val currentTranscriptCueIndex: Int? = null,
  val transcriptSectionId: String? = null,
) {

  @Immutable
  data class TranscriptCue(
    val chapterId: ChapterId,
    val text: String,
    val timestamp: String,
    val position: Duration,
    val endPosition: Duration,
  )

  sealed interface SleepTimerViewState {
    data object Disabled : SleepTimerViewState

    sealed interface Enabled : SleepTimerViewState {
      data object WithEndOfChapter : Enabled

      @JvmInline
      value class WithDuration(val leftDuration: Duration) : Enabled
    }
  }

  init {
    require(duration > Duration.ZERO) {
      "Duration must be positive in $this"
    }
  }
}

internal sealed interface BookPlayDialogViewState {
  data class SpeedDialog(val speed: Float) : BookPlayDialogViewState {

    val maxSpeed: Float get() = if (speed < 2F) 2F else 3.5F
  }

  data class VolumeGainDialog(
    val gain: Decibel,
    val valueFormatted: String,
    val maxGain: Decibel,
  ) : BookPlayDialogViewState

  data class SelectChapterDialog(val items: List<ItemViewState>) : BookPlayDialogViewState {

    data class ItemViewState(
      val number: Int,
      val name: String,
      val active: Boolean,
      val time: String,
    )
  }

  @JvmInline
  value class SleepTimer(val viewState: SleepTimerViewState) : BookPlayDialogViewState
}

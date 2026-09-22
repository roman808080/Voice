package voice.features.playbackScreen.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.strings.R as StringsR
import voice.core.ui.VoiceTheme
import voice.features.playbackScreen.BookPlayViewState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun PlaybackMediaPane(
  bookId: BookId,
  viewState: BookPlayViewState,
  showTranscript: Boolean,
  onShowTranscriptChange: (Boolean) -> Unit,
  transcriptListState: LazyListState,
  onPlayClick: () -> Unit,
  onSubtitleClick: (ChapterId, Duration) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    if (viewState.transcriptCues.isNotEmpty()) {
      SingleChoiceSegmentedButtonRow(
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
          .padding(bottom = 8.dp),
      ) {
        val labels = listOf(
          stringResource(StringsR.string.playback_display_cover),
          stringResource(StringsR.string.playback_display_transcript),
        )
        labels.forEachIndexed { index, label ->
          val transcript = index == 1
          SegmentedButton(
            selected = showTranscript == transcript,
            onClick = { onShowTranscriptChange(transcript) },
            shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size),
          ) {
            Text(label)
          }
        }
      }
    }

    if (showTranscript && viewState.transcriptCues.isNotEmpty()) {
      TranscriptList(
        cues = viewState.transcriptCues,
        activeCueIndices = viewState.activeTranscriptCueIndices,
        state = transcriptListState,
        onSubtitleClick = onSubtitleClick,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1F),
      )
    } else {
      CoverRow(
        bookId = bookId,
        cover = viewState.cover,
        onPlayClick = onPlayClick,
        sleepTimerState = viewState.sleepTimerState,
        subtitles = viewState.subtitles,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1F),
      )
    }
  }
}

@Composable
private fun TranscriptList(
  cues: List<BookPlayViewState.TranscriptCue>,
  activeCueIndices: Set<Int>,
  state: LazyListState,
  onSubtitleClick: (ChapterId, Duration) -> Unit,
  modifier: Modifier = Modifier,
) {
  val currentDescription = stringResource(StringsR.string.playback_transcript_current)
  LazyColumn(
    modifier = modifier,
    state = state,
  ) {
    itemsIndexed(
      items = cues,
      key = { index, cue -> "${cue.position.inWholeMilliseconds}:$index" },
    ) { index, cue ->
      val active = index in activeCueIndices
      ListItem(
        colors = ListItemDefaults.colors(
          containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        ),
        modifier = Modifier
          .padding(vertical = 2.dp)
          .clip(RoundedCornerShape(12.dp))
          .semantics {
            selected = active
            if (active) stateDescription = currentDescription
          }
          .clickable { onSubtitleClick(cue.chapterId, cue.position) },
        trailingContent = {
          Text(text = cue.timestamp)
        },
      ) {
        Text(text = cue.text)
      }
    }
  }
}

@Preview(name = "Transcript portrait", widthDp = 400, heightDp = 700)
@Preview(name = "Transcript landscape", widthDp = 400, heightDp = 360)
@Composable
private fun TranscriptPreview() {
  VoiceTheme {
    PlaybackMediaPane(
      bookId = BookId("preview"),
      viewState = BookPlayViewState(
        chapterName = "Chapter 4",
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
            text = "An earlier subtitle.",
            timestamp = "0:05",
            position = 5.seconds,
            endPosition = 10.seconds,
          ),
          BookPlayViewState.TranscriptCue(
            chapterId = ChapterId("chapter"),
            text = "The subtitle at the current position.",
            timestamp = "0:20",
            position = 20.seconds,
            endPosition = 25.seconds,
          ),
          BookPlayViewState.TranscriptCue(
            chapterId = ChapterId("chapter"),
            text = "A later subtitle with enough text to wrap onto another line.",
            timestamp = "0:35",
            position = 35.seconds,
            endPosition = 40.seconds,
          ),
        ),
        activeTranscriptCueIndices = setOf(1),
        currentTranscriptCueIndex = 1,
        transcriptSectionId = "preview:0",
      ),
      showTranscript = true,
      onShowTranscriptChange = {},
      transcriptListState = rememberLazyListState(),
      onPlayClick = {},
      onSubtitleClick = { _, _ -> },
      modifier = Modifier.fillMaxSize(),
    )
  }
}

package voice.features.playbackScreen.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.core.ui.PlayButton
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.playButtonSharedBoundsModifier
import voice.core.strings.R as StringsR

@Composable
internal fun PlaybackRow(
  playing: Boolean,
  onPlayClick: () -> Unit,
  onRewindClick: () -> Unit,
  onFastForwardClick: () -> Unit,
  onJumpToCurrentSubtitle: (() -> Unit)? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceEvenly,
  ) {
    SkipButton(forward = false, onClick = onRewindClick)

    PlayButton(
      playing = playing,
      fabSize = 80.dp,
      iconSize = 36.dp,
      onPlayClick = onPlayClick,
      sharedElementModifier = Modifier.playButtonSharedBoundsModifier(),
    )
    SkipButton(forward = true, onClick = onFastForwardClick)
    if (onJumpToCurrentSubtitle != null) {
      IconButton(
        onClick = onJumpToCurrentSubtitle,
      ) {
        Icon(
          imageVector = VoiceIcons.Timelapse,
          contentDescription = stringResource(StringsR.string.playback_transcript_jump_to_current),
        )
      }
    }
  }
}

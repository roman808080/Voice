package voice.features.playbackScreen.view

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import voice.core.strings.R
import voice.core.ui.icons.VoiceIcons

@Composable
internal fun OverflowMenu(
  skipSilence: Boolean,
  onSkipSilenceClick: () -> Unit,
  onVolumeBoostClick: () -> Unit,
  showAutoSynchronizeTranscript: Boolean,
  autoSynchronizeTranscript: Boolean,
  onAutoSynchronizeTranscriptClick: () -> Unit,
) {
  Box {
    var expanded by remember { mutableStateOf(false) }
    IconButton(
      onClick = {
        expanded = !expanded
      },
    ) {
      Icon(
        imageVector = VoiceIcons.MoreVert,
        contentDescription = stringResource(id = R.string.common_action_more),
      )
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
    ) {
      DropdownMenuItem(
        onClick = {
          expanded = false
          onSkipSilenceClick()
        },
        text = {
          Text(text = stringResource(id = R.string.playback_option_skip_silence))
        },
        trailingIcon = {
          Checkbox(
            checked = skipSilence,
            onCheckedChange = {
              expanded = false
              onSkipSilenceClick()
            },
          )
        },
      )
      if (showAutoSynchronizeTranscript) {
        val autoSynchronizeLabel = stringResource(id = R.string.playback_transcript_auto_synchronize)
        DropdownMenuItem(
          onClick = {
            expanded = false
            onAutoSynchronizeTranscriptClick()
          },
          text = {
            Text(text = autoSynchronizeLabel)
          },
          trailingIcon = {
            Switch(
              checked = autoSynchronizeTranscript,
              onCheckedChange = {
                expanded = false
                onAutoSynchronizeTranscriptClick()
              },
              modifier = Modifier.semantics {
                contentDescription = autoSynchronizeLabel
              },
            )
          },
        )
      }
      DropdownMenuItem(
        onClick = {
          expanded = false
          onVolumeBoostClick()
        },
        text = {
          Text(text = stringResource(id = R.string.playback_option_volume_boost))
        },
      )
    }
  }
}

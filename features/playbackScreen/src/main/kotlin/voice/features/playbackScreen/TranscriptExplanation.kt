package voice.features.playbackScreen

import android.content.Intent

internal fun transcriptExplanationIntent(
  prompt: String,
  chooserTitle: String,
): Intent = Intent.createChooser(
  Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_TEXT, prompt)
  },
  chooserTitle,
)

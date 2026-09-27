package voice.features.bookmark.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import voice.core.data.ThreadId
import voice.core.strings.R as StringsR

@Composable
internal fun CreateThreadDialog(
  onDismissRequest: () -> Unit,
  onCreateFromBeginning: () -> Unit,
  onForkCurrentPosition: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = { Text(stringResource(StringsR.string.bookmark_thread_create_title)) },
    text = {
      Column {
        ListItem(
          modifier = Modifier.clickable {
            onCreateFromBeginning()
          },
        ) {
          Text(stringResource(StringsR.string.bookmark_thread_create_beginning))
        }
        ListItem(
          modifier = Modifier.clickable {
            onForkCurrentPosition()
          },
        ) {
          Text(stringResource(StringsR.string.bookmark_thread_create_current))
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismissRequest) {
        Text(stringResource(StringsR.string.common_dialog_cancel))
      }
    },
  )
}

@Composable
internal fun EditThreadDialog(
  threadId: ThreadId,
  initialTitle: String,
  onDismissRequest: () -> Unit,
  onRename: (ThreadId, String) -> Unit,
) {
  var title by remember(threadId) {
    mutableStateOf(TextFieldValue(initialTitle, TextRange(0, initialTitle.length)))
  }
  val focusRequester = remember { FocusRequester() }
  fun confirm() {
    val value = title.text.trim()
    if (value.isNotEmpty()) {
      onRename(threadId, value)
      onDismissRequest()
    }
  }
  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = { Text(stringResource(StringsR.string.bookmark_thread_edit_title)) },
    text = {
      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text(stringResource(StringsR.string.bookmark_thread_edit_name_label)) },
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
          .focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.Sentences,
          autoCorrectEnabled = true,
          imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { confirm() }),
      )
    },
    confirmButton = {
      Button(onClick = { confirm() }, enabled = title.text.isNotBlank()) {
        Text(stringResource(StringsR.string.common_dialog_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) {
        Text(stringResource(StringsR.string.common_dialog_cancel))
      }
    },
  )
  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }
}

@Composable
internal fun DeleteThreadDialog(
  threadId: ThreadId,
  title: String,
  onDismissRequest: () -> Unit,
  onDelete: (ThreadId) -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = { Text(stringResource(StringsR.string.bookmark_thread_delete_title)) },
    text = { Text(stringResource(StringsR.string.bookmark_thread_delete_message, title)) },
    confirmButton = {
      Button(
        onClick = {
          onDelete(threadId)
          onDismissRequest()
        },
      ) {
        Text(stringResource(StringsR.string.common_action_delete))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) {
        Text(stringResource(StringsR.string.common_dialog_cancel))
      }
    },
  )
}

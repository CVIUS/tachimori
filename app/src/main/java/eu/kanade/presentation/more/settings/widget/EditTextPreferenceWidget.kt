package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import eu.kanade.tachiyomi.R
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.util.alertDialogWidth

@Composable
fun EditTextPreferenceWidget(
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    value: String,
    error: String?,
    onConfirm: suspend (String) -> Boolean,
    hasAdditionalButton: Boolean,
    onClickAdditional: suspend () -> Boolean,
    onClickAdditionalString: String?,
) {
    var isDialogShown by rememberSaveable { mutableStateOf(false) }

    TextPreferenceWidget(
        title = title,
        subtitle = subtitle?.format(value),
        icon = icon,
        onPreferenceClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        val scope = rememberCoroutineScope()
        val onDismissRequest = { isDialogShown = false }
        var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(value))
        }
        AlertDialog(
            modifier = Modifier.alertDialogWidth(),
            onDismissRequest = onDismissRequest,
            title = { Text(text = title) },
            text = {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    trailingIcon = {
                        if (error != null && textFieldValue.text.isBlank()) {
                            Icon(imageVector = Icons.Filled.Error, contentDescription = null)
                        } else if (textFieldValue.text.isNotBlank()) {
                            IconButton(onClick = { textFieldValue = TextFieldValue("") }) {
                                Icon(imageVector = Icons.Filled.Cancel, contentDescription = null)
                            }
                        }
                    },
                    supportingText = {
                        if (error != null && textFieldValue.text.isBlank()) {
                            Text(text = error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    isError = error != null && textFieldValue.text.isBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
            ),
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasAdditionalButton && onClickAdditionalString != null) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    if (onClickAdditional()) {
                                        onDismissRequest()
                                    }
                                }
                            },
                        ) {
                            Text(text = onClickAdditionalString)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                    TextButton(
                        enabled = textFieldValue.text != value && textFieldValue.text.isNotBlank(),
                        onClick = {
                            scope.launch {
                                if (onConfirm(textFieldValue.text)) {
                                    onDismissRequest()
                                }
                            }
                        },
                    ) {
                        Text(text = stringResource(android.R.string.ok))
                    }
                }
            },
        )
    }
}

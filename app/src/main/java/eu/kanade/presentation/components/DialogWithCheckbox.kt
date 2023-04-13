package eu.kanade.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.R
import tachiyomi.presentation.core.components.material.padding

@Composable
fun DialogWithCheckbox(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmText: String,
    onConfirm: (Boolean) -> Unit,
    withCheckbox: Boolean = true,
    checkboxText: String? = null,
) {
    var confirm by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            Column {
                Text(text = text)
                if (withCheckbox && checkboxText != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                value = confirm,
                                onValueChange = { confirm = it },
                            )
                            .padding(top = MaterialTheme.padding.large),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = confirm,
                            onCheckedChange = null,
                        )
                        Text(
                            modifier = Modifier.padding(start = MaterialTheme.padding.small),
                            text = checkboxText,
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onConfirm(confirm)
                },
            ) {
                Text(text = confirmText)
            }
        },
    )
}

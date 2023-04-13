package eu.kanade.presentation.category.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.DialogProperties
import eu.kanade.tachiyomi.R
import kotlinx.coroutines.delay
import tachiyomi.domain.category.model.Category
import tachiyomi.presentation.core.util.alertDialogWidth
import kotlin.time.Duration.Companion.seconds

@Composable
fun CategoryCreateDialog(
    onDismissRequest: () -> Unit,
    onCreate: (String) -> Unit,
    categories: List<Category>,
) {
    val focusRequester = remember { FocusRequester() }

    var name by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    val isDefaultCategory = name.text == stringResource(R.string.label_default)
    val nameAlreadyExists = remember(name) { categories.anyWithName(name.text) }

    AlertDialog(
        modifier = Modifier.alertDialogWidth(),
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        ),
        confirmButton = {
            TextButton(
                enabled = name.text.isNotEmpty() && !nameAlreadyExists && !isDefaultCategory,
                onClick = {
                    onCreate(name.text)
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
        title = {
            Text(text = stringResource(R.string.action_add_category))
        },
        text = {
            OutlinedTextField(
                modifier = Modifier.focusRequester(focusRequester),
                value = name,
                onValueChange = { name = it },
                trailingIcon = {
                    if (name.text.isNotEmpty()) {
                        when {
                            nameAlreadyExists -> Icon(imageVector = Icons.Filled.Error, contentDescription = null)
                            isDefaultCategory -> Icon(imageVector = Icons.Filled.Error, contentDescription = null)
                            else -> {
                                IconButton(onClick = { name = TextFieldValue("") }) {
                                    Icon(imageVector = Icons.Filled.Cancel, contentDescription = null)
                                }
                            }
                        }
                    }
                },
                label = { Text(text = stringResource(R.string.name)) },
                supportingText = {
                    if (name.text.isNotEmpty()) {
                        when {
                            nameAlreadyExists -> Text(text = stringResource(R.string.error_category_exists))
                            isDefaultCategory -> Text(text = stringResource(R.string.error_default_category))
                            else -> {}
                        }
                    } else {
                        Text(text = stringResource(R.string.information_required_plain))
                    }
                },
                isError = name.text.isNotEmpty() && (nameAlreadyExists || isDefaultCategory),
                singleLine = true,
            )
        },
    )

    LaunchedEffect(focusRequester) {
        // TODO: https://issuetracker.google.com/issues/204502668
        delay(0.1.seconds)
        focusRequester.requestFocus()
    }
}

@Composable
fun CategoryRenameDialog(
    onDismissRequest: () -> Unit,
    onRename: (String) -> Unit,
    categories: List<Category>,
    category: Category,
) {
    val focusRequester = remember { FocusRequester() }

    var valueHasChanged by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(category.name)) }

    val isDefaultCategory = name.text == stringResource(R.string.label_default)
    val nameAlreadyExists = remember(name) { categories.anyWithName(name.text) }

    AlertDialog(
        modifier = Modifier.alertDialogWidth(),
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        ),
        confirmButton = {
            TextButton(
                enabled = name.text.isNotEmpty() && valueHasChanged && !nameAlreadyExists && !isDefaultCategory,
                onClick = {
                    onRename(name.text)
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(R.string.action_rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
        title = {
            Text(text = stringResource(R.string.action_rename_category))
        },
        text = {
            OutlinedTextField(
                modifier = Modifier.focusRequester(focusRequester),
                value = name,
                onValueChange = {
                    valueHasChanged = it.text != category.name
                    name = it
                },
                placeholder = {
                    Text(text = category.name, style = MaterialTheme.typography.bodySmall)
                },
                trailingIcon = {
                    if (name.text.isNotEmpty()) {
                        if (valueHasChanged) {
                            when {
                                nameAlreadyExists -> Icon(imageVector = Icons.Filled.Error, contentDescription = null)
                                isDefaultCategory -> Icon(imageVector = Icons.Filled.Error, contentDescription = null)
                                else -> {
                                    IconButton(onClick = { name = TextFieldValue("") }) {
                                        Icon(imageVector = Icons.Filled.Cancel, contentDescription = null)
                                    }
                                }
                            }
                        } else {
                            IconButton(onClick = { name = TextFieldValue("") }) {
                                Icon(imageVector = Icons.Filled.Cancel, contentDescription = null)
                            }
                        }
                    }
                },
                label = { Text(text = stringResource(R.string.name)) },
                supportingText = {
                    if (name.text.isNotEmpty()) {
                        if (valueHasChanged) {
                            when {
                                nameAlreadyExists -> Text(text = stringResource(R.string.error_category_exists))
                                isDefaultCategory -> Text(text = stringResource(R.string.error_default_category))
                                else -> {}
                            }
                        }
                    } else {
                        Text(text = stringResource(R.string.information_required_plain))
                    }
                },
                isError = name.text.isNotEmpty() && valueHasChanged && (nameAlreadyExists || isDefaultCategory),
                singleLine = true,
            )
        },
    )

    LaunchedEffect(focusRequester) {
        // TODO: https://issuetracker.google.com/issues/204502668
        delay(0.1.seconds)
        focusRequester.requestFocus()
    }
}

@Composable
fun CategoryDeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    category: Category,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(R.string.delete_category_))
        },
        text = {
            Text(text = stringResource(R.string.dialog_delete_category_desc, category.name))
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDelete()
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}

internal fun List<Category>.anyWithName(name: String): Boolean {
    return any { name == it.name }
}

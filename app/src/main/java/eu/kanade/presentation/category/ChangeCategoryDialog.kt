package eu.kanade.presentation.category

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.core.preference.asToggleableState
import eu.kanade.tachiyomi.R
import tachiyomi.core.preference.CheckboxState
import tachiyomi.domain.category.model.Category
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Divider
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.util.isScrolledToEnd
import tachiyomi.presentation.core.util.isScrolledToStart

@Composable
fun ChangeCategoryDialog(
    favorite: Boolean,
    initialSelection: List<CheckboxState<Category>>,
    onDismissRequest: () -> Unit,
    onEditCategories: () -> Unit,
    onConfirm: (List<Long>, List<Long>) -> Unit,
) {
    if (initialSelection.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismissRequest()
                        onEditCategories()
                    },
                ) {
                    Text(text = stringResource(R.string.action_edit_categories))
                }
            },
            title = {
                Text(text = stringResource(R.string.action_move_category))
            },
            text = {
                Text(text = stringResource(R.string.information_empty_category_dialog))
            },
        )
        return
    }
    var selection by remember { mutableStateOf(initialSelection) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Row {
                TextButton(
                    onClick = {
                        onDismissRequest()
                        onEditCategories()
                    },
                ) {
                    Text(text = stringResource(R.string.action_edit))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(R.string.action_cancel))
                }

                TextButton(
                    enabled = selection != initialSelection || !favorite,
                    onClick = {
                        onDismissRequest()
                        onConfirm(
                            selection.filter { it is CheckboxState.State.Checked || it is CheckboxState.TriState.Include }.map { it.value.id },
                            selection.filter { it is CheckboxState.State.None || it is CheckboxState.TriState.None }.map { it.value.id },
                        )
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        },
        title = {
            Text(text = stringResource(R.string.action_move_category))
        },
        text = {
            Box {
                val state = rememberLazyListState()
                ScrollbarLazyColumn(state = state) {
                    selection.forEach { checkbox ->
                        item {
                            val onChange: (CheckboxState<Category>) -> Unit = {
                                val index = selection.indexOf(it)
                                if (index != -1) {
                                    val mutableList = selection.toMutableList()
                                    mutableList[index] = it.next()
                                    selection = mutableList.toList()
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onChange(checkbox) }
                                    .minimumInteractiveComponentSize(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                when (checkbox) {
                                    is CheckboxState.TriState -> {
                                        TriStateCheckbox(
                                            modifier = Modifier.heightIn(min = 48.dp),
                                            state = checkbox.asToggleableState(),
                                            onClick = { onChange(checkbox) },
                                        )
                                    }
                                    is CheckboxState.State -> {
                                        Checkbox(
                                            modifier = Modifier.heightIn(min = 48.dp),
                                            checked = checkbox.isChecked,
                                            onCheckedChange = null,
                                        )
                                    }
                                }

                                Text(
                                    text = checkbox.value.visualName,
                                    style = MaterialTheme.typography.bodyMedium.merge(),
                                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                )
                            }
                        }
                    }
                }
                if (!state.isScrolledToStart()) Divider(modifier = Modifier.align(Alignment.TopCenter))
                if (!state.isScrolledToEnd()) Divider(modifier = Modifier.align(Alignment.BottomCenter))
            }
        },
    )
}

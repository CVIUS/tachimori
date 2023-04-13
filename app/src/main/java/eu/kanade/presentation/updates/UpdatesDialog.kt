package eu.kanade.presentation.updates

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.updates.UpdatesItem

@Composable
fun UpdatesDeleteChaptersDialog(
    onDismissRequest: () -> Unit,
    selected: List<UpdatesItem>,
    removeBookmarkedChapters: Boolean,
    onConfirm: () -> Unit,
    goToSettings: () -> Unit,
) {
    val allDownloads = selected.filter { it.isDownloaded }
    val onlyAllowed = allDownloads.filterNot { it.update.bookmark }
    val downloadCount = if (removeBookmarkedChapters) allDownloads.size else onlyAllowed.size
    val bookmarked = allDownloads.filter { it.update.bookmark }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            if (downloadCount != 0) {
                Text(text = stringResource(R.string.delete_downloads))
            } else {
                if (!removeBookmarkedChapters && bookmarked.isNotEmpty()) {
                    Text(text = stringResource(R.string.hmmm))
                }
            }
        },
        text = {
            val msg = buildString {
                if (downloadCount != 0) {
                    append(pluralStringResource(id = R.plurals.dialog_confirm_delete_chapters_desc, count = allDownloads.size, allDownloads.size))
                }

                if (!removeBookmarkedChapters && bookmarked.isNotEmpty()) {
                    if (downloadCount != 0) {
                        append("\n\n")
                        append(stringResource(R.string.dialog_delete_bookmarked_not_allowed, bookmarked.size, bookmarked.size))
                    } else {
                        append(pluralStringResource(id = R.plurals.dialog_delete_bookmarked_not_allowed_only, count = bookmarked.size, bookmarked.size))
                    }
                }
            }
            Text(text = msg)
        },
        confirmButton = {
            Row {
                if (!removeBookmarkedChapters && bookmarked.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            goToSettings()
                            onDismissRequest()
                        },
                    ) {
                        Text(text = stringResource(R.string.go_to_settings))
                    }
                }
                if (downloadCount != 0) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                    TextButton(
                        onClick = {
                            onDismissRequest()
                            onConfirm()
                        },
                    ) {
                        Text(text = stringResource(R.string.action_delete))
                    }
                }
            }
        },
    )
}

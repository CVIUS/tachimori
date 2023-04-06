package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.more.settings.widget.ListPreferenceDialog
import eu.kanade.tachiyomi.R
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibrarySort

@Composable
fun LibraryHeaderItem(
    sort: LibrarySort,
    displayMode: LibraryDisplayMode,
    onClickOpenSortSheet: () -> Unit,
    onClickOpenRandomManga: () -> Unit,
    onChangeDisplayMode: (LibraryDisplayMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    val arrowIcons = when (!sort.isAscending) {
        true -> Icons.Default.ArrowDownward
        false -> Icons.Default.ArrowUpward
    }

    val (sortString, sortDirections) = when (sort.type) {
        LibrarySort.Type.Alphabetical -> R.string.name to arrowIcons
        LibrarySort.Type.TotalChapters -> R.string.action_sort_total to arrowIcons
        LibrarySort.Type.LastRead -> R.string.action_sort_last_read to arrowIcons
        LibrarySort.Type.LastUpdate -> R.string.action_sort_last_manga_update to arrowIcons
        LibrarySort.Type.UnreadCount -> R.string.action_sort_unread_count to arrowIcons
        LibrarySort.Type.LatestChapter -> R.string.action_sort_latest_chapter to arrowIcons
        LibrarySort.Type.ChapterFetchDate -> R.string.action_sort_chapter_fetch_date to arrowIcons
        LibrarySort.Type.DateAdded -> R.string.action_sort_date_added to arrowIcons
    }

    Row(modifier = modifier) {
        TextButton(onClick = onClickOpenSortSheet) {
            Text(
                text = stringResource(sortString),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))

            Icon(
                imageVector = sortDirections,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = onClickOpenRandomManga) {
            Icon(
                imageVector = Icons.Outlined.Shuffle,
                contentDescription = stringResource(R.string.action_open_random_manga),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(onClick = { showDialog = true }) {
            Icon(
                imageVector = if (displayMode == LibraryDisplayMode.List) Icons.Filled.ViewModule else Icons.Filled.List,
                contentDescription = stringResource(R.string.action_display_mode),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (showDialog) {
                ListPreferenceDialog(
                    value = displayMode,
                    title = stringResource(R.string.action_display_mode),
                    entries = mapOf(
                        LibraryDisplayMode.CompactGrid to stringResource(R.string.action_display_grid),
                        LibraryDisplayMode.ComfortableGrid to stringResource(R.string.action_display_comfortable_grid),
                        LibraryDisplayMode.CoverOnlyGrid to stringResource(R.string.action_display_cover_only_grid),
                        LibraryDisplayMode.List to stringResource(R.string.action_display_list),
                    ),
                    onValueChange = onChangeDisplayMode,
                    onDismissRequest = { showDialog = false },
                )
            }
        }
    }
}

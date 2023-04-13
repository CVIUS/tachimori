package eu.kanade.presentation.manga

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.domain.manga.model.downloadedFilter
import eu.kanade.domain.manga.model.forceDownloaded
import eu.kanade.presentation.components.DialogWithCheckbox
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.presentation.components.TriStateItem
import eu.kanade.tachiyomi.R
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.TriStateFilter
import tachiyomi.presentation.core.components.RadioItem
import tachiyomi.presentation.core.components.SortItem

@Composable
fun ChapterSettingsDialog(
    onDismissRequest: () -> Unit,
    manga: Manga? = null,
    onDownloadFilterChanged: (TriStateFilter) -> Unit,
    onUnreadFilterChanged: (TriStateFilter) -> Unit,
    onBookmarkedFilterChanged: (TriStateFilter) -> Unit,
    onSortModeChanged: (Long) -> Unit,
    onDisplayModeChanged: (Long) -> Unit,
    onSetAsDefault: (Boolean) -> Unit,
) {
    var showSetAsDefaultDialog by rememberSaveable { mutableStateOf(false) }
    if (showSetAsDefaultDialog) {
        DialogWithCheckbox(
            onDismissRequest = { showSetAsDefaultDialog = false },
            title = stringResource(R.string.chapter_settings),
            text = stringResource(R.string.dialog_chapter_settings_desc),
            confirmText = stringResource(R.string.action_apply),
            onConfirm = onSetAsDefault,
            checkboxText = stringResource(R.string.dialog_with_checkbox_chapter_settings_library),
        )
    }

    TabbedDialog(
        onDismissRequest = onDismissRequest,
        tabTitles = listOf(
            stringResource(R.string.action_filter),
            stringResource(R.string.action_sort),
            stringResource(R.string.action_display),
        ),
        tabOverflowMenuContent = { closeMenu ->
            DropdownMenuItem(
                text = { Text(stringResource(R.string.set_chapter_settings_as_default)) },
                onClick = {
                    showSetAsDefaultDialog = true
                    closeMenu()
                },
            )
        },
    ) { contentPadding, page ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .padding(vertical = TabbedDialogPaddings.Vertical)
                .verticalScroll(rememberScrollState()),
        ) {
            when (page) {
                0 -> {
                    FilterPage(
                        downloadFilter = manga?.downloadedFilter ?: TriStateFilter.DISABLED,
                        onDownloadFilterChanged = onDownloadFilterChanged.takeUnless { manga?.forceDownloaded() == true },
                        unreadFilter = manga?.unreadFilter ?: TriStateFilter.DISABLED,
                        onUnreadFilterChanged = onUnreadFilterChanged,
                        bookmarkedFilter = manga?.bookmarkedFilter ?: TriStateFilter.DISABLED,
                        onBookmarkedFilterChanged = onBookmarkedFilterChanged,
                    )
                }
                1 -> {
                    SortPage(
                        sortingMode = manga?.sorting ?: 0,
                        sortDescending = manga?.sortDescending() ?: false,
                        onItemSelected = onSortModeChanged,
                    )
                }
                2 -> {
                    DisplayPage(
                        displayMode = manga?.displayMode ?: 0,
                        onItemSelected = onDisplayModeChanged,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.FilterPage(
    downloadFilter: TriStateFilter,
    onDownloadFilterChanged: ((TriStateFilter) -> Unit)?,
    unreadFilter: TriStateFilter,
    onUnreadFilterChanged: (TriStateFilter) -> Unit,
    bookmarkedFilter: TriStateFilter,
    onBookmarkedFilterChanged: (TriStateFilter) -> Unit,
) {
    TriStateItem(
        label = stringResource(R.string.label_downloaded),
        state = downloadFilter,
        onClick = onDownloadFilterChanged,
    )
    TriStateItem(
        label = stringResource(R.string.action_filter_unread),
        state = unreadFilter,
        onClick = onUnreadFilterChanged,
    )
    TriStateItem(
        label = stringResource(R.string.action_filter_bookmarked),
        state = bookmarkedFilter,
        onClick = onBookmarkedFilterChanged,
    )
}

@Composable
private fun ColumnScope.SortPage(
    sortingMode: Long,
    sortDescending: Boolean,
    onItemSelected: (Long) -> Unit,
) {
    listOf(
        R.string.sort_by_source to Manga.CHAPTER_SORTING_SOURCE,
        R.string.sort_by_number to Manga.CHAPTER_SORTING_NUMBER,
        R.string.sort_by_upload_date to Manga.CHAPTER_SORTING_UPLOAD_DATE,
    ).map { (titleRes, mode) ->
        SortItem(
            label = stringResource(titleRes),
            sortDescending = sortDescending.takeIf { sortingMode == mode },
            onClick = { onItemSelected(mode) },
        )
    }
}

@Composable
private fun ColumnScope.DisplayPage(
    displayMode: Long,
    onItemSelected: (Long) -> Unit,
) {
    listOf(
        R.string.show_title to Manga.CHAPTER_DISPLAY_NAME,
        R.string.show_chapter_number to Manga.CHAPTER_DISPLAY_NUMBER,
    ).map { (titleRes, mode) ->
        RadioItem(
            label = stringResource(titleRes),
            selected = displayMode == mode,
            onClick = { onItemSelected(mode) },
        )
    }
}

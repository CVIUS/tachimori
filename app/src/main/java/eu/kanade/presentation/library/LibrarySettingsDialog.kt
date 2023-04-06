package eu.kanade.presentation.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.presentation.components.TriStateItem
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.library.LibrarySettingsScreenModel
import tachiyomi.domain.manga.model.TriStateFilter
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem

@Composable
fun LibrarySettingsDialog(
    onDismissRequest: () -> Unit,
    screenModel: LibrarySettingsScreenModel,
) {
    TabbedDialog(
        onDismissRequest = onDismissRequest,
        tabTitles = listOf(
            stringResource(R.string.action_filter),
            stringResource(R.string.action_display),
        ),
    ) { contentPadding, page ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .padding(vertical = TabbedDialogPaddings.Vertical)
                .verticalScroll(rememberScrollState()),
        ) {
            when (page) {
                0 -> FilterPage(
                    screenModel = screenModel,
                )
                1 -> DisplayPage(
                    screenModel = screenModel,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.FilterPage(
    screenModel: LibrarySettingsScreenModel,
) {
    val filterDownloaded by screenModel.libraryPreferences.filterDownloaded().collectAsState()
    val downloadedOnly by screenModel.preferences.downloadedOnly().collectAsState()
    TriStateItem(
        label = stringResource(R.string.label_downloaded),
        state = if (downloadedOnly) {
            TriStateFilter.ENABLED_IS
        } else {
            filterDownloaded
        },
        enabled = !downloadedOnly,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterDownloaded) },
    )
    val filterUnread by screenModel.libraryPreferences.filterUnread().collectAsState()
    TriStateItem(
        label = stringResource(R.string.action_filter_unread),
        state = filterUnread,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterUnread) },
    )
    val filterStarted by screenModel.libraryPreferences.filterStarted().collectAsState()
    TriStateItem(
        label = stringResource(R.string.label_started),
        state = filterStarted,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterStarted) },
    )
    val filterBookmarked by screenModel.libraryPreferences.filterBookmarked().collectAsState()
    TriStateItem(
        label = stringResource(R.string.action_filter_bookmarked),
        state = filterBookmarked,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterBookmarked) },
    )
    val filterCompleted by screenModel.libraryPreferences.filterCompleted().collectAsState()
    TriStateItem(
        label = stringResource(R.string.completed),
        state = filterCompleted,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterCompleted) },
    )

    when (screenModel.trackServices.size) {
        0 -> {
            // No trackers
        }
        1 -> {
            val service = screenModel.trackServices[0]
            val filterTracker by screenModel.libraryPreferences.filterTracking(service.id.toInt()).collectAsState()
            TriStateItem(
                label = stringResource(R.string.action_filter_tracked),
                state = filterTracker,
                onClick = { screenModel.toggleTracker(service.id.toInt()) },
            )
        }
        else -> {
            HeadingItem(R.string.action_filter_tracked)
            screenModel.trackServices.map { service ->
                val filterTracker by screenModel.libraryPreferences.filterTracking(service.id.toInt()).collectAsState()
                TriStateItem(
                    label = stringResource(service.nameRes()),
                    state = filterTracker,
                    onClick = { screenModel.toggleTracker(service.id.toInt()) },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.DisplayPage(
    screenModel: LibrarySettingsScreenModel,
) {
    HeadingItem(R.string.badges_header)
    val downloadBadge by screenModel.libraryPreferences.downloadBadge().collectAsState()
    val downloadedOnly by screenModel.preferences.downloadedOnly().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.action_display_download_badge),
        checked = downloadBadge || downloadedOnly,
        enabled = !downloadedOnly,
        onClick = {
            screenModel.togglePreference(LibraryPreferences::downloadBadge)
        },
    )
    val unreadBadge by screenModel.libraryPreferences.unreadBadge().collectAsState()
    val showContinueReadingButton by screenModel.libraryPreferences.showContinueReadingButton().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.action_display_unread_badge),
        checked = unreadBadge || showContinueReadingButton,
        enabled = !showContinueReadingButton,
        onClick = {
            screenModel.togglePreference(LibraryPreferences::unreadBadge)
        },
    )
    val localBadge by screenModel.libraryPreferences.localBadge().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.action_display_local_badge),
        checked = localBadge || downloadedOnly,
        enabled = !downloadedOnly,
        onClick = {
            screenModel.togglePreference(LibraryPreferences::localBadge)
        },
    )
    val languageBadge by screenModel.libraryPreferences.languageBadge().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.action_display_language_badge),
        checked = languageBadge,
        onClick = {
            screenModel.togglePreference(LibraryPreferences::languageBadge)
        },
    )

    HeadingItem(R.string.tabs_header)
    val categoryTabs by screenModel.libraryPreferences.categoryTabs().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.action_display_show_tabs),
        checked = categoryTabs,
        onClick = {
            screenModel.togglePreference(LibraryPreferences::categoryTabs)
        },
    )
    val categoryNumberOfItems by screenModel.libraryPreferences.categoryNumberOfItems().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.action_display_show_number_of_items),
        checked = categoryNumberOfItems,
        onClick = {
            screenModel.togglePreference(LibraryPreferences::categoryNumberOfItems)
        },
    )

    HeadingItem(R.string.other_header)
    CheckboxItem(
        label = stringResource(R.string.action_display_show_continue_reading_button),
        checked = showContinueReadingButton,
        onClick = {
            screenModel.togglePreference(LibraryPreferences::showContinueReadingButton)
        },
    )
}

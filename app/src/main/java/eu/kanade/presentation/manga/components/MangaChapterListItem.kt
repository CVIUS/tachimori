package eu.kanade.presentation.manga.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.twotone.BookmarkAdd
import androidx.compose.material.icons.twotone.BookmarkRemove
import androidx.compose.material.icons.twotone.Visibility
import androidx.compose.material.icons.twotone.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import me.saket.swipe.rememberSwipeableActionsState
import tachiyomi.presentation.core.components.material.ReadItemAlpha
import tachiyomi.presentation.core.components.material.SecondaryItemAlpha
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.util.selectedBackground

@Composable
fun MangaChapterListItem(
    modifier: Modifier = Modifier,
    title: String,
    date: String?,
    readProgress: String?,
    scanlator: String?,
    read: Boolean,
    bookmark: Boolean,
    selected: Boolean,
    swipeActionEnabled: Boolean,
    downloadIndicatorEnabled: Boolean,
    downloadStateProvider: () -> Download.State,
    downloadProgressProvider: () -> Int,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onDownloadClick: ((ChapterDownloadAction) -> Unit)?,
    swipeAction: LibraryPreferences.ChapterSwipeAction,
    onSwipeToBookmark: () -> Unit,
    onSwipeToRemoveBookmark: () -> Unit,
    onSwipeToMarkRead: () -> Unit,
    onSwipeToMarkUnread: () -> Unit,
) {
    MangaChapterListItemSwipeActions(
        read = read,
        bookmark = bookmark,
        swipeAction = swipeAction,
        swipeActionEnabled = swipeActionEnabled,
        onSwipeToBookmark = onSwipeToBookmark,
        onSwipeToRemoveBookmark = onSwipeToRemoveBookmark,
        onSwipeToMarkRead = onSwipeToMarkRead,
        onSwipeToMarkUnread = onSwipeToMarkUnread,
    ) {
        MangaChapterListItemContent(
            modifier = modifier,
            title = title,
            date = date,
            readProgress = readProgress,
            scanlator = scanlator,
            read = read,
            bookmark = bookmark,
            selected = selected,
            downloadIndicatorEnabled = downloadIndicatorEnabled,
            downloadStateProvider = downloadStateProvider,
            downloadProgressProvider = downloadProgressProvider,
            onLongClick = onLongClick,
            onClick = onClick,
            onDownloadClick = onDownloadClick,
        )
    }
}

@Composable
fun MangaChapterListItemSwipeActions(
    read: Boolean,
    bookmark: Boolean,
    swipeActionEnabled: Boolean,
    onSwipeToBookmark: () -> Unit,
    onSwipeToRemoveBookmark: () -> Unit,
    onSwipeToMarkRead: () -> Unit,
    onSwipeToMarkUnread: () -> Unit,
    swipeAction: LibraryPreferences.ChapterSwipeAction,
    content: @Composable (BoxScope.() -> Unit),
) {
    val state = rememberSwipeableActionsState()
//    val thresholdCrossed = abs(state.offset.value) > with(LocalDensity.current) { SwipeThreshold.toPx() }
//    val isSwiping = remember(state) { abs(state.offset.value) > 0 }

    val onSwipeToggleBookmark = SwipeAction(
        onSwipe = if (bookmark) onSwipeToRemoveBookmark else onSwipeToBookmark,
        icon = {
            val bookmarkIcon = when (bookmark) {
                true -> Icons.TwoTone.BookmarkRemove
                false -> Icons.TwoTone.BookmarkAdd
            }
            MangaChapterListItemSwipeActionsIcon(
                icon = bookmarkIcon,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        },
        background = MaterialTheme.colorScheme.primary,
        isUndo = bookmark,
    ).takeIf { swipeAction == LibraryPreferences.ChapterSwipeAction.ToggleBookmark }

    val onSwipeToggleRead = SwipeAction(
        onSwipe = if (read) onSwipeToMarkUnread else onSwipeToMarkRead,
        icon = {
            val readIcon = when (read) {
                true -> Icons.TwoTone.VisibilityOff
                false -> Icons.TwoTone.Visibility
            }
            MangaChapterListItemSwipeActionsIcon(
                icon = readIcon,
                tint = Color.White,
            )
        },
        background = Color.Gray,
        isUndo = read,
    ).takeIf { swipeAction == LibraryPreferences.ChapterSwipeAction.ToggleRead }

    val startActions = buildList {
        if (onSwipeToggleBookmark != null) {
            add(onSwipeToggleBookmark)
        }
        if (onSwipeToggleRead != null) {
            add(onSwipeToggleRead)
        }
    }.takeIf {
        swipeActionEnabled && swipeAction != LibraryPreferences.ChapterSwipeAction.Disabled
    }.orEmpty()

    val backgroundUntilSwipeThreshold: Color =
        if (swipeActionEnabled) {
            when (swipeAction) {
                LibraryPreferences.ChapterSwipeAction.ToggleRead -> Color.Gray
                LibraryPreferences.ChapterSwipeAction.ToggleBookmark -> MaterialTheme.colorScheme.primary
                LibraryPreferences.ChapterSwipeAction.Disabled -> Color.Unspecified
            }
        } else {
            Color.Unspecified
        }

    SwipeableActionsBox(
        state = state,
        startActions = startActions,
        swipeThreshold = SwipeThreshold,
        backgroundUntilSwipeThreshold = backgroundUntilSwipeThreshold,
    ) {
        content()
    }
}

internal val SwipeThreshold = 112.dp

@Composable
fun MangaChapterListItemContent(
    modifier: Modifier = Modifier,
    title: String,
    date: String?,
    readProgress: String?,
    scanlator: String?,
    read: Boolean,
    bookmark: Boolean,
    selected: Boolean,
    downloadIndicatorEnabled: Boolean,
    downloadStateProvider: () -> Download.State,
    downloadProgressProvider: () -> Int,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onDownloadClick: ((ChapterDownloadAction) -> Unit)?,
) {
    val textAlpha = if (read) ReadItemAlpha else 1f
    val textSubtitleAlpha = if (read) ReadItemAlpha else SecondaryItemAlpha

    Row(
        modifier = modifier
            .selectedBackground(selected)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var textHeight by remember { mutableStateOf(0) }
                if (bookmark) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = stringResource(R.string.action_filter_bookmarked),
                        modifier = Modifier
                            .sizeIn(maxHeight = with(LocalDensity.current) { textHeight.toDp() - 2.dp }),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = LocalContentColor.current.copy(alpha = textAlpha),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textHeight = it.size.height },
                )
            }

            Row {
                ProvideTextStyle(
                    value = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        color = LocalContentColor.current.copy(alpha = textSubtitleAlpha),
                    ),
                ) {
                    if (date != null) {
                        Text(
                            text = date,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (readProgress != null || scanlator != null) DotSeparatorText()
                    }
                    if (readProgress != null) {
                        Text(
                            text = readProgress,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = LocalContentColor.current.copy(alpha = ReadItemAlpha),
                        )
                        if (scanlator != null) DotSeparatorText()
                    }
                    if (scanlator != null) {
                        Text(
                            text = scanlator,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        if (onDownloadClick != null) {
            ChapterDownloadIndicator(
                enabled = downloadIndicatorEnabled,
                modifier = Modifier.padding(start = 4.dp),
                downloadStateProvider = downloadStateProvider,
                downloadProgressProvider = downloadProgressProvider,
                onClick = onDownloadClick,
            )
        }
    }
}

@Composable
fun MangaChapterListItemSwipeActionsIcon(
    icon: ImageVector,
    tint: Color,
) {
    Icon(
        modifier = Modifier.padding(horizontal = MaterialTheme.padding.large),
        imageVector = icon,
        tint = tint,
        contentDescription = null,
    )
}

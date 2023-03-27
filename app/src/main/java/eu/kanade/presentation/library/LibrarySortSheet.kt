package eu.kanade.presentation.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.R
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.model.sort
import tachiyomi.presentation.core.components.LazyColumn
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.components.material.Divider
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.theme.header

@Composable
fun SortSheet(
    category: Category,
    onDismissRequest: () -> Unit,
    onClick: (Category, LibrarySort.Type, LibrarySort.Direction) -> Unit,
) {
    AdaptiveSheet(
        onDismissRequest = onDismissRequest,
    ) { contentPadding ->
        LazyColumn(
            contentPadding = contentPadding,
        ) {
            val sortItem =
                listOf(
                    R.string.name to LibrarySort.Type.Alphabetical,
                    R.string.action_sort_total to LibrarySort.Type.TotalChapters,
                    R.string.action_sort_last_read to LibrarySort.Type.LastRead,
                    R.string.action_sort_last_manga_update to LibrarySort.Type.LastUpdate,
                    R.string.action_sort_unread_count to LibrarySort.Type.UnreadCount,
                    R.string.action_sort_latest_chapter to LibrarySort.Type.LatestChapter,
                    R.string.action_sort_chapter_fetch_date to LibrarySort.Type.ChapterFetchDate,
                    R.string.action_sort_date_added to LibrarySort.Type.DateAdded,
                )

            val sortingMode = category.sort.type
            val sortDescending = !category.sort.isAscending

            item { HeadingSortItem() }

            items(sortItem) { (titleRes, sort) ->
                SortItem(
                    label = stringResource(titleRes),
                    sortDescending = sortDescending.takeIf { sortingMode == sort },
                    onClick = {
                        val isTogglingDirection = sortingMode == sort
                        val direction = when {
                            isTogglingDirection -> if (sortDescending) LibrarySort.Direction.Ascending else LibrarySort.Direction.Descending
                            else -> if (sortDescending) LibrarySort.Direction.Descending else LibrarySort.Direction.Ascending
                        }

                        onClick(category, sort, direction)
                        onDismissRequest()
                    },
                )
            }
        }
    }
}

@Composable
private fun HeadingSortItem() {
    Column {
        Text(
            text = stringResource(R.string.action_sort_by),
            style = MaterialTheme.typography.header,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.padding.large, vertical = MaterialTheme.padding.medium),
        )
        Divider()
        Spacer(Modifier.height(MaterialTheme.padding.small))
    }
}

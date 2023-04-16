package eu.kanade.presentation.history

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.history.components.HistoryContent
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.history.HistoryState
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    state: HistoryState,
    snackbarHostState: SnackbarHostState,
    relativeTime: Int,
    dateFormat: DateFormat,
    onSearchQueryChange: (String?) -> Unit,
    onClickCover: (mangaId: Long) -> Unit,
    onClickResume: (mangaId: Long, chapterId: Long) -> Unit,
    onClickDelete: (historyId: Long, mangaId: Long, title: String, preferredChapterName: String) -> Unit,
    onClickDeleteAll: () -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            SearchToolbar(
                titleContent = { AppBarTitle(stringResource(R.string.history)) },
                searchQuery = state.searchQuery,
                onChangeSearchQuery = onSearchQueryChange,
                actions = {
                    IconButton(onClick = onClickDeleteAll) {
                        Icon(
                            Icons.Outlined.DeleteSweep,
                            contentDescription = stringResource(R.string.pref_clear_history),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        state.list.let {
            if (it == null) {
                LoadingScreen(modifier = Modifier.padding(contentPadding))
            } else if (it.isEmpty()) {
                val msg = if (!state.searchQuery.isNullOrEmpty()) {
                    R.string.no_results_found
                } else {
                    R.string.information_no_recent_manga
                }
                EmptyScreen(
                    textResource = msg,
                    modifier = Modifier.padding(contentPadding),
                )
            } else {
                HistoryContent(
                    history = it,
                    relativeTime = relativeTime,
                    dateFormat = dateFormat,
                    contentPadding = contentPadding,
                    onClickCover = { history -> onClickCover(history.mangaId) },
                    onClickResume = { history -> onClickResume(history.mangaId, history.chapterId) },
                    onClickDelete = { history, preferredChapterName ->
                        onClickDelete(
                            history.id,
                            history.mangaId,
                            history.title,
                            preferredChapterName,
                        )
                    },
                )
            }
        }
    }
}

sealed class HistoryUiModel {
    data class Header(val date: Date) : HistoryUiModel()
    data class Item(val history: HistoryWithRelations) : HistoryUiModel()
}

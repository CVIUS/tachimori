package eu.kanade.presentation.history.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.RelativeDateHeader
import eu.kanade.presentation.history.HistoryUiModel
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.DateFormat

@Composable
fun HistoryContent(
    history: List<HistoryUiModel>,
    relativeTime: Int,
    dateFormat: DateFormat,
    contentPadding: PaddingValues,
    onClickCover: (HistoryWithRelations) -> Unit,
    onClickResume: (HistoryWithRelations) -> Unit,
    onClickDelete: (HistoryWithRelations, preferredChapterName: String) -> Unit,
) {

    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        items(
            items = history,
            key = { "history-${it.hashCode()}" },
            contentType = {
                when (it) {
                    is HistoryUiModel.Header -> "header"
                    is HistoryUiModel.Item -> "item"
                }
            },
        ) { item ->
            when (item) {
                is HistoryUiModel.Header -> {
                    RelativeDateHeader(
                        modifier = Modifier.animateItemPlacement(),
                        date = item.date,
                        relativeTime = relativeTime,
                        dateFormat = dateFormat,
                    )
                }
                is HistoryUiModel.Item -> {
                    HistoryItem(
                        manga = runBlocking { Injekt.get<GetManga>().await(item.history.mangaId) },
                        modifier = Modifier.animateItemPlacement(),
                        history = item.history,
                        onClickCover = { onClickCover(item.history) },
                        onClickResume = { onClickResume(item.history) },
                        onClickDelete = { onClickDelete(item.history, it) },
                    )
                }
            }
        }
    }
}

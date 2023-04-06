package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.browse.components.GlobalSearchCardRow
import eu.kanade.presentation.browse.components.GlobalSearchEmptyResultItem
import eu.kanade.presentation.browse.components.GlobalSearchErrorResultItem
import eu.kanade.presentation.browse.components.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.components.GlobalSearchResultItem
import eu.kanade.presentation.browse.components.GlobalSearchToolbar
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchState
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.components.LazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen

@Composable
fun GlobalSearchScreen(
    state: GlobalSearchState,
    navigateUp: () -> Unit,
    pinnedSourcesOnlyEmpty: Boolean,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    getManga: @Composable (CatalogueSource, Manga) -> State<Manga>,
    onClickSource: (CatalogueSource) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
) {
    Scaffold(
        topBar = {
            GlobalSearchToolbar(
                searchQuery = state.searchQuery,
                progress = state.progress,
                total = state.total,
                navigateUp = navigateUp,
                onChangeSearchQuery = onChangeSearchQuery,
                onSearch = onSearch,
            )
        },
    ) { paddingValues ->
        if (pinnedSourcesOnlyEmpty) {
            EmptyScreen(
                message = stringResource(R.string.no_pinned_sources),
                modifier = Modifier.padding(paddingValues),
            )
            return@Scaffold
        }

        GlobalSearchContent(
            items = state.items,
            contentPadding = paddingValues,
            getManga = getManga,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
        )
    }
}

@Composable
fun GlobalSearchContent(
    items: Map<CatalogueSource, SearchItemResult>,
    contentPadding: PaddingValues,
    getManga: @Composable (CatalogueSource, Manga) -> State<Manga>,
    onClickSource: (CatalogueSource) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
) {
    LazyColumn(
        contentPadding = contentPadding,
    ) {
        items.forEach { (source, result) ->
            item(key = source.id) {
                GlobalSearchResultItem(
                    title = source.name,
                    subtitle = LocaleHelper.getDisplayName(source.lang),
                    onClick = { onClickSource(source) },
                ) {
                    when (result) {
                        SearchItemResult.Loading -> {
                            GlobalSearchLoadingResultItem()
                        }
                        is SearchItemResult.Success -> {
                            if (result.isEmpty) {
                                GlobalSearchEmptyResultItem()
                                return@GlobalSearchResultItem
                            }

                            GlobalSearchCardRow(
                                titles = result.result,
                                getManga = { getManga(source, it) },
                                onClick = onClickItem,
                                onLongClick = onLongClickItem,
                            )
                        }
                        is SearchItemResult.Error -> {
                            GlobalSearchErrorResultItem(message = result.throwable.message)
                        }
                    }
                }
            }
        }
    }
}

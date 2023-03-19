package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.BrowseSourceContent
import eu.kanade.presentation.browse.MissingSourceScreen
import eu.kanade.presentation.browse.components.BrowseSourceToolbar
import eu.kanade.presentation.category.ChangeCategoryDialog
import eu.kanade.presentation.manga.DuplicateMangaDialog
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.extension.details.SourcePreferencesScreen
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel.Listing
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import tachiyomi.core.Constants
import tachiyomi.core.util.lang.launchIO
import tachiyomi.presentation.core.components.material.Divider
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.source.local.LocalSource

data class BrowseSourceScreen(
    private val sourceId: Long,
    private val listingQuery: String?,
) : Screen(), AssistContentScreen {

    private var assistUrl: String? = null

    override fun onProvideAssistUrl() = assistUrl

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { BrowseSourceScreenModel(sourceId, listingQuery) }
        val state by screenModel.state.collectAsState()

        val navigator = LocalNavigator.currentOrThrow
        val navigateUp: () -> Unit = {
            when {
                !state.isUserQuery && state.toolbarQuery != null -> screenModel.setToolbarQuery(null)
                else -> navigator.pop()
            }
        }

        if (screenModel.source is SourceManager.StubSource) {
            MissingSourceScreen(
                source = screenModel.source,
                navigateUp = navigateUp,
            )
            return
        }

        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        val uriHandler = LocalUriHandler.current
        val context = LocalContext.current

        val onHelpClick = { uriHandler.openUri(LocalSource.HELP_URL) }
        val onWebViewClick = f@{
            val source = screenModel.source as? HttpSource ?: return@f
            navigator.push(
                WebViewScreen(
                    url = source.baseUrl,
                    initialTitle = source.name,
                    sourceId = source.id,
                ),
            )
        }

        LaunchedEffect(screenModel.source) {
            assistUrl = (screenModel.source as? HttpSource)?.baseUrl
        }

        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    BrowseSourceToolbar(
                        searchQuery = state.toolbarQuery,
                        onSearchQueryChange = screenModel::setToolbarQuery,
                        source = screenModel.source,
                        displayMode = screenModel.displayMode,
                        onDisplayModeChange = { screenModel.displayMode = it },
                        navigateUp = navigateUp,
                        onWebViewClick = onWebViewClick,
                        onHelpClick = onHelpClick,
                        onSettingsClick = { navigator.push(SourcePreferencesScreen(sourceId)) },
                        onSearch = { screenModel.search(it) },
                    )

                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = MaterialTheme.padding.small),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                    ) {
                        FilterChip(
                            selected = state.listing == Listing.Popular,
                            onClick = {
                                screenModel.resetFilters()
                                screenModel.setListing(Listing.Popular)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Favorite,
                                    contentDescription = "",
                                    modifier = Modifier
                                        .size(FilterChipDefaults.IconSize),
                                )
                            },
                            label = {
                                Text(text = stringResource(R.string.popular))
                            },
                        )
                        if ((screenModel.source as CatalogueSource).supportsLatest) {
                            FilterChip(
                                selected = state.listing == Listing.Latest,
                                onClick = {
                                    screenModel.resetFilters()
                                    screenModel.setListing(Listing.Latest)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.NewReleases,
                                        contentDescription = "",
                                        modifier = Modifier
                                            .size(FilterChipDefaults.IconSize),
                                    )
                                },
                                label = {
                                    Text(text = stringResource(R.string.latest))
                                },
                            )
                        }
                        if (state.filters.isNotEmpty()) {
                            FilterChip(
                                selected = state.listing is Listing.Search,
                                onClick = screenModel::openFilterSheet,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.FilterList,
                                        contentDescription = "",
                                        modifier = Modifier
                                            .size(FilterChipDefaults.IconSize),
                                    )
                                },
                                label = {
                                    Text(text = stringResource(R.string.action_filter))
                                },
                            )
                        }
                    }

                    Divider()
                }
            },
            snackbarHost = { SnackbarHost(hostState = screenModel.snackbarHostState) },
        ) { paddingValues ->
            val pagingFlow by screenModel.mangaPagerFlowFlow.collectAsState()

            BrowseSourceContent(
                source = screenModel.source,
                mangaList = pagingFlow.collectAsLazyPagingItems(),
                columns = screenModel.getColumnsPreference(LocalConfiguration.current.orientation),
                displayMode = screenModel.displayMode,
                snackbarHostState = screenModel.snackbarHostState,
                contentPadding = paddingValues,
                onWebViewClick = onWebViewClick,
                onHelpClick = { uriHandler.openUri(Constants.URL_HELP) },
                onLocalSourceHelpClick = onHelpClick,
                onMangaClick = { navigator.push((MangaScreen(it.id, true))) },
                onMangaLongClick = { manga ->
                    scope.launchIO {
                        val duplicateManga = screenModel.getDuplicateLibraryManga(manga)
                        when {
                            manga.favorite -> {
                                screenModel.changeMangaFavorite(manga)
                                screenModel.setSnackbar(BrowseSourceScreenModel.Snackbar.ToggleFavorite(manga))
                            }
                            duplicateManga != null -> screenModel.setDialog(
                                BrowseSourceScreenModel.Dialog.AddDuplicateManga(
                                    manga,
                                    duplicateManga,
                                ),
                            )
                            else -> screenModel.addFavorite(manga)
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
            )
        }

        val onDismissRequest = { screenModel.setDialog(null) }
        when (val dialog = state.dialog) {
            is BrowseSourceScreenModel.Dialog.Filter -> {
                SourceFilterDialog(
                    onDismissRequest = onDismissRequest,
                    filters = state.filters,
                    onReset = {
                        screenModel.resetFilters()
                    },
                    onFilter = {
                        screenModel.search(filters = state.filters)
                    },
                    onUpdate = {
                        screenModel.setFilters(it)
                    },
                )
            }
            is BrowseSourceScreenModel.Dialog.AddDuplicateManga -> {
                DuplicateMangaDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = { screenModel.addFavorite(dialog.manga) },
                    onOpenManga = { navigator.push(MangaScreen(dialog.duplicate.id, true)) },
                )
            }
            is BrowseSourceScreenModel.Dialog.ChangeMangaCategory -> {
                ChangeCategoryDialog(
                    favorite = dialog.manga.favorite,
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = { navigator.push(CategoryScreen()) },
                    onConfirm = { include, _ ->
                        if (!dialog.manga.favorite) {
                            screenModel.changeMangaFavorite(dialog.manga)
                        }
                        screenModel.changeMangaCategory(dialog.manga, include)
                        screenModel.moveMangaToCategories(dialog.manga, include)
                    },
                )
            }
            is BrowseSourceScreenModel.Dialog.Migrate -> {}
            else -> {}
        }

        LaunchedEffect(Unit) {
            launch {
                queryEvent.receiveAsFlow()
                    .collectLatest {
                        when (it) {
                            is SearchType.Genre -> screenModel.searchGenre(it.txt)
                            is SearchType.Text -> screenModel.search(it.txt)
                        }
                    }
            }
            launch {
                screenModel.snackbar.collectLatest { snackbar ->
                    val snackbarHostState = screenModel.snackbarHostState
                    when (snackbar) {
                        is BrowseSourceScreenModel.Snackbar.OnFavoriteDefaultSet -> {
                            val result = snackbarHostState.showSnackbar(
                                message = context.getString(R.string.default_category_favorite),
                                actionLabel = context.getString(R.string.action_change),
                                duration = SnackbarDuration.Short,
                            )

                            if (result == SnackbarResult.ActionPerformed) {
                                screenModel.promptChangeCategories(snackbar.manga.copy(favorite = true))
                            }
                        }
                        is BrowseSourceScreenModel.Snackbar.ToggleFavorite -> {
                            val textRes = if (snackbar.manga.favorite) R.string.manga_removed_library else R.string.manga_added_library
                            val result = snackbarHostState.showSnackbar(
                                message = context.getString(textRes),
                                actionLabel = context.getString(R.string.action_undo).takeIf { snackbar.manga.favorite },
                                withDismissAction = true,
                            )
                            if (result == SnackbarResult.ActionPerformed && snackbar.manga.favorite) {
                                screenModel.changeMangaFavorite(snackbar.manga.copy(favorite = false))
                            }
                        }
                        is BrowseSourceScreenModel.Snackbar.PickCategory -> {
                            when (snackbar.selection.size) {
                                0 -> {
                                    val textRes = if (snackbar.manga.favorite) R.string.manga_moved_library else R.string.manga_added_library
                                    snackbarHostState.showSnackbar(context.getString(textRes), withDismissAction = true)
                                }
                                1 -> {
                                    val textRes = if (snackbar.manga.favorite) R.string.manga_moved_in_ else R.string.manga_added_in_
                                    snackbarHostState.showSnackbar(context.getString(textRes, snackbar.firstCategory?.name), withDismissAction = true)
                                }
                                else -> {
                                    val textRes = if (snackbar.manga.favorite) R.string.manga_moved_in_categories else R.string.manga_added_in_categories
                                    snackbarHostState.showSnackbar(context.getString(textRes, snackbar.selection.size), withDismissAction = true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun search(query: String) = queryEvent.send(SearchType.Text(query))
    suspend fun searchGenre(name: String) = queryEvent.send(SearchType.Genre(name))

    companion object {
        private val queryEvent = Channel<SearchType>()
    }

    sealed class SearchType(val txt: String) {
        class Text(txt: String) : SearchType(txt)
        class Genre(txt: String) : SearchType(txt)
    }
}

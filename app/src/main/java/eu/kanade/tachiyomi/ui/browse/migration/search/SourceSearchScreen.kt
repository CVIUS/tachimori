package eu.kanade.tachiyomi.ui.browse.migration.search

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.BrowseSourceContent
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import tachiyomi.core.Constants
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.source.local.LocalSource

data class SourceSearchScreen(
    private val oldManga: Manga,
    private val sourceId: Long,
    private val query: String?,
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

        val screenModel = rememberScreenModel { BrowseSourceScreenModel(sourceId, query) }
        val state by screenModel.state.collectAsStateWithLifecycle()
        val snackbarHostState = remember { screenModel.snackbarHostState }

        val pagingFlow by screenModel.mangaPagerFlowFlow.collectAsStateWithLifecycle()
        val mangaList = pagingFlow.collectAsLazyPagingItems()

        Scaffold(
            topBar = { scrollBehavior ->
                SearchToolbar(
                    searchQuery = state.toolbarQuery ?: "",
                    onChangeSearchQuery = screenModel::setToolbarQuery,
                    onClickCloseSearch = navigator::pop,
                    onSearch = { screenModel.search(it) },
                    scrollBehavior = scrollBehavior,
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { paddingValues ->
            BrowseSourceContent(
                source = screenModel.source,
                mangaList = mangaList,
                columns = screenModel.getColumnsPreference(LocalConfiguration.current.orientation),
                displayMode = screenModel.displayMode,
                errorSnackbar = { screenModel.setSnackbar(BrowseSourceScreenModel.Snackbar.Error(it)) },
                contentPadding = paddingValues,
                onWebViewClick = {
                    val source = screenModel.source as? HttpSource ?: return@BrowseSourceContent
                    navigator.push(
                        WebViewScreen(
                            url = source.baseUrl,
                            initialTitle = source.name,
                            sourceId = source.id,
                        ),
                    )
                },
                onHelpClick = { uriHandler.openUri(Constants.URL_HELP) },
                onLocalSourceHelpClick = { uriHandler.openUri(LocalSource.HELP_URL) },
                onMangaClick = { screenModel.setDialog(BrowseSourceScreenModel.Dialog.Migrate(oldManga, it)) },
                onMangaLongClick = { navigator.push(MangaScreen(it.id, true)) },
            )
        }

        when (val dialog = state.dialog) {
            is BrowseSourceScreenModel.Dialog.Migrate -> {
                MigrateDialog(
                    oldManga = dialog.oldManga,
                    newManga = dialog.newManga,
                    screenModel = rememberScreenModel { MigrateDialogScreenModel() },
                    onDismissRequest = { screenModel.setDialog(null) },
                    onClickTitle = { navigator.push(MangaScreen(dialog.newManga.id, true)) },
                    onPopScreen = {
                        scope.launch {
                            navigator.popUntilRoot()
                            HomeScreen.openTab(HomeScreen.Tab.Library())
                            navigator.push(MangaScreen(dialog.newManga.id, true))
                        }
                    },
                )
            }
            else -> {}
        }

        LaunchedEffect(Unit) {
            launch {
                screenModel.snackbar.collectLatest { snackbar ->
                    when (snackbar) {
                        is BrowseSourceScreenModel.Snackbar.Error -> {
                            val result = snackbarHostState.showSnackbar(
                                message = snackbar.error,
                                actionLabel = context.getString(R.string.action_webview_refresh),
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                mangaList.refresh()
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

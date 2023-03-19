package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.net.toUri
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.manga.model.hasCustomCover
import eu.kanade.domain.manga.model.toSManga
import eu.kanade.presentation.category.ChangeCategoryDialog
import eu.kanade.presentation.components.NavigatorAdaptiveSheet
import eu.kanade.presentation.manga.ChapterSettingsDialog
import eu.kanade.presentation.manga.DuplicateMangaDialog
import eu.kanade.presentation.manga.EditCoverAction
import eu.kanade.presentation.manga.MangaScreen
import eu.kanade.presentation.manga.components.DeleteChaptersDialog
import eu.kanade.presentation.manga.components.MangaCoverDialog
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.isLocalOrStub
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.migration.search.MigrateSearchScreen
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.manga.track.TrackInfoDialogHomeScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.screens.LoadingScreen

class MangaScreen(
    private val mangaId: Long,
    val fromSource: Boolean = false,
) : Screen(), AssistContentScreen {

    private var assistUrl: String? = null

    override fun onProvideAssistUrl() = assistUrl

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current
        val scope = rememberCoroutineScope()
        val screenModel = rememberScreenModel { MangaInfoScreenModel(context, mangaId, fromSource) }

        val state by screenModel.state.collectAsState()

        if (state is MangaScreenState.Loading) {
            LoadingScreen()
            return
        }

        val successState = state as MangaScreenState.Success
        val isHttpSource = remember { successState.source is HttpSource }

        LaunchedEffect(successState.manga, screenModel.source) {
            if (isHttpSource) {
                try {
                    withIOContext {
                        assistUrl = getMangaUrl(screenModel.manga, screenModel.source)
                    }
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e) { "Failed to get manga URL" }
                }
            }
        }

        MangaScreen(
            state = successState,
            snackbarHostState = screenModel.snackbarHostState,
            dateRelativeTime = screenModel.relativeTime,
            dateFormat = screenModel.dateFormat,
            isTabletUi = isTabletUi(),
            onBackClicked = navigator::pop,
            onChapterClicked = { openChapter(context, it) },
            onDownloadChapter = screenModel::runChapterDownloadActions.takeIf { !successState.source.isLocalOrStub() },
            onAddToLibraryClicked = {
                screenModel.toggleFavorite()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onWebViewClicked = { openMangaInWebView(navigator, screenModel.manga, screenModel.source) }.takeIf { isHttpSource },
            onWebViewLongClicked = { copyMangaUrl(context, screenModel.manga, screenModel.source) }.takeIf { isHttpSource },
            onTrackingClicked = screenModel::showTrackDialog.takeIf { successState.trackingAvailable },
            onTagSearch = { scope.launch { performGenreSearch(navigator, it, screenModel.source!!) } },
            onFilterButtonClicked = screenModel::showSettingsDialog,
            onRefresh = screenModel::fetchAllFromSource,
            onContinueReading = { continueReading(context, screenModel.getNextUnreadChapter()) },
            onSearch = { query, global -> scope.launch { performSearch(navigator, query, global) } },
            onCoverClicked = screenModel::showCoverDialog,
            onShareClicked = { shareManga(context, screenModel.manga, screenModel.source) }.takeIf { isHttpSource },
            onDownloadActionClicked = screenModel::runDownloadAction.takeIf { !successState.source.isLocalOrStub() },
            onEditCategoryClicked = screenModel::promptChangeCategories.takeIf { successState.manga.favorite },
            onMigrateClicked = { navigator.push(MigrateSearchScreen(successState.manga.id)) }.takeIf { successState.manga.favorite },
            onMultiBookmarkClicked = screenModel::bookmarkChapters,
            onMultiMarkAsReadClicked = screenModel::markChaptersRead,
            onMarkPreviousAsReadClicked = screenModel::markPreviousChapterRead,
            onMultiDeleteClicked = screenModel::showDeleteChapterDialog,
            onChapterSelected = screenModel::toggleSelection,
            onAllChapterSelected = screenModel::toggleAllSelection,
            onInvertSelection = screenModel::invertSelection,
        )

        LaunchedEffect(Unit) {
            launch {
                screenModel.snackbar.collectLatest { snackbar ->
                    val snackbarHostState = screenModel.snackbarHostState
                    when (snackbar) {
                        MangaInfoScreenModel.Snackbar.DeleteDownloadedChapters -> {
                            val result = snackbarHostState.showSnackbar(
                                message = context.getString(R.string.delete_downloads_for_manga),
                                actionLabel = context.getString(R.string.action_delete),
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                screenModel.deleteDownloads()
                            }
                        }
                        is MangaInfoScreenModel.Snackbar.InternalError -> {
                            snackbarHostState.showSnackbar(snackbar.error)
                        }
                        is MangaInfoScreenModel.Snackbar.FetchChaptersFromSourceError -> {
                            snackbarHostState.showSnackbar(snackbar.error)
                        }
                        is MangaInfoScreenModel.Snackbar.AddFavorite -> {
                            val result = snackbarHostState.showSnackbar(
                                message = context.getString(R.string.snack_add_to_library),
                                actionLabel = context.getString(R.string.action_add),
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                screenModel.toggleFavorite()
                            }
                        }
                        is MangaInfoScreenModel.Snackbar.ToggleFavorite -> {
                            snackbarHostState.showSnackbar(context.getString(R.string.manga_added_library), withDismissAction = true)
                        }
                        is MangaInfoScreenModel.Snackbar.OnRemoveManga -> {
                            val result = snackbarHostState.showSnackbar(
                                message = context.getString(R.string.manga_removed_library),
                                actionLabel = context.getString(R.string.action_undo),
                                withDismissAction = true,
                            )
                            if (result == SnackbarResult.ActionPerformed && snackbar.manga.favorite) {
                                screenModel.updateFavorite(snackbar.manga, true)
                            }
                        }
                        MangaInfoScreenModel.Snackbar.DefaultCategorySet -> {
                            val result = snackbarHostState.showSnackbar(
                                message = context.getString(R.string.default_category_favorite),
                                actionLabel = context.getString(R.string.action_change),
                                duration = SnackbarDuration.Short,
                            )

                            if (result == SnackbarResult.ActionPerformed) {
                                screenModel.promptChangeCategories()
                            }
                        }
                        is MangaInfoScreenModel.Snackbar.ChangeCategory -> {
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
                        MangaInfoScreenModel.Snackbar.DeleteDownloadedChapters -> {
                            val result = screenModel.snackbarHostState.showSnackbar(
                                message = context.getString(R.string.delete_downloads_for_manga),
                                actionLabel = context.getString(R.string.action_delete),
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                screenModel.deleteDownloads()
                            }
                        }
                        MangaInfoScreenModel.Snackbar.UpdateDefaultChapterSettings -> {
                            screenModel.snackbarHostState.showSnackbar(context.getString(R.string.chapter_settings_updated))
                        }
                    }
                }
            }
        }

        val onDismissRequest = { screenModel.dismissDialog() }
        when (val dialog = (state as? MangaScreenState.Success)?.dialog) {
            null -> {}
            is MangaInfoScreenModel.Dialog.ChangeCategory -> {
                ChangeCategoryDialog(
                    favorite = dialog.manga.favorite,
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = { navigator.push(CategoryScreen()) },
                    onConfirm = { include, _ ->
                        screenModel.moveMangaToCategoriesAndAddToLibrary(dialog.manga, include)
                        screenModel.oni(dialog.manga, include)
                    },
                )
            }
            is MangaInfoScreenModel.Dialog.DeleteChapters -> {
                DeleteChaptersDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = {
                        screenModel.toggleAllSelection(false)
                        screenModel.deleteChapters(dialog.chapters)
                    },
                )
            }
            is MangaInfoScreenModel.Dialog.DuplicateManga -> DuplicateMangaDialog(
                onDismissRequest = onDismissRequest,
                onConfirm = { screenModel.toggleFavorite(checkDuplicate = false) },
                onOpenManga = { navigator.push(MangaScreen(dialog.duplicate.id, true)) },
            )
            MangaInfoScreenModel.Dialog.SettingsSheet -> ChapterSettingsDialog(
                onDismissRequest = onDismissRequest,
                manga = successState.manga,
                onDownloadFilterChanged = screenModel::setDownloadedFilter,
                onUnreadFilterChanged = screenModel::setUnreadFilter,
                onBookmarkedFilterChanged = screenModel::setBookmarkedFilter,
                onSortModeChanged = screenModel::setSorting,
                onDisplayModeChanged = screenModel::setDisplayMode,
                onSetAsDefault = screenModel::setCurrentSettingsAsDefault,
            )
            MangaInfoScreenModel.Dialog.TrackSheet -> {
                NavigatorAdaptiveSheet(
                    screen = TrackInfoDialogHomeScreen(
                        mangaId = successState.manga.id,
                        mangaTitle = successState.manga.title,
                        sourceId = successState.source.id,
                    ),
                    enableSwipeDismiss = { it.lastItem is TrackInfoDialogHomeScreen },
                    onDismissRequest = onDismissRequest,
                )
            }
            MangaInfoScreenModel.Dialog.FullCover -> {
                val sm = rememberScreenModel { MangaCoverScreenModel(successState.manga.id) }
                val manga by sm.state.collectAsState()
                if (manga != null) {
                    val getContent = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
                        if (it == null) return@rememberLauncherForActivityResult
                        sm.editCover(it)
                    }
                    MangaCoverDialog(
                        coverDataProvider = { manga!! },
                        snackbarHostState = sm.snackbarHostState,
                        isCustomCover = remember(manga) { manga!!.hasCustomCover() },
                        onShareClick = { sm.shareCover(context) },
                        onSaveClick = { sm.saveCover(context) },
                        onEditClick = {
                            when (it) {
                                EditCoverAction.EDIT -> getContent.launch("image/*")
                                EditCoverAction.DELETE -> sm.deleteCustomCover()
                            }
                        },
                        onDismissRequest = onDismissRequest,
                    )
                    LaunchedEffect(Unit) {
                        launch {
                            sm.snackbar.collectLatest { snackbar ->
                                when (snackbar) {
                                    MangaCoverScreenModel.Snackbar.CoverSaved -> {
                                        sm.snackbarHostState.showSnackbar(message = context.getString(R.string.cover_saved), withDismissAction = true)
                                    }
                                    MangaCoverScreenModel.Snackbar.CoverSaveError -> {
                                        sm.snackbarHostState.showSnackbar(message = context.getString(R.string.error_saving_cover), duration = SnackbarDuration.Indefinite)
                                    }
                                    MangaCoverScreenModel.Snackbar.CoverUpdated -> {
                                        sm.snackbarHostState.showSnackbar(message = context.getString(R.string.cover_updated), withDismissAction = true)
                                    }
                                    MangaCoverScreenModel.Snackbar.CoverUpdateFailed -> {
                                        sm.snackbarHostState.showSnackbar(message = context.getString(R.string.notification_cover_update_failed), duration = SnackbarDuration.Indefinite)
                                    }
                                    MangaCoverScreenModel.Snackbar.CoverShareError -> {
                                        sm.snackbarHostState.showSnackbar(message = context.getString(R.string.error_sharing_cover), duration = SnackbarDuration.Indefinite)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LoadingScreen(Modifier.systemBarsPadding())
                }
            }
        }
    }

    private fun continueReading(context: Context, unreadChapter: Chapter?) {
        if (unreadChapter != null) openChapter(context, unreadChapter)
    }

    private fun openChapter(context: Context, chapter: Chapter) {
        context.startActivity(ReaderActivity.newIntent(context, chapter.mangaId, chapter.id))
    }

    private fun getMangaUrl(manga_: Manga?, source_: Source?): String? {
        val manga = manga_ ?: return null
        val source = source_ as? HttpSource ?: return null

        return try {
            source.getMangaUrl(manga.toSManga())
        } catch (e: Exception) {
            null
        }
    }

    private fun openMangaInWebView(navigator: Navigator, manga_: Manga?, source_: Source?) {
        getMangaUrl(manga_, source_)?.let { url ->
            navigator.push(
                WebViewScreen(
                    url = url,
                    initialTitle = manga_?.title,
                    sourceId = source_?.id,
                ),
            )
        }
    }

    private fun shareManga(context: Context, manga_: Manga?, source_: Source?) {
        try {
            getMangaUrl(manga_, source_)?.let { url ->
                val intent = url.toUri().toShareIntent(context, type = "text/plain")
                context.startActivity(
                    Intent.createChooser(
                        intent,
                        context.getString(R.string.action_share),
                    ),
                )
            }
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }

    /**
     * Perform a search using the provided query.
     *
     * @param query the search query to the parent controller
     */
    private suspend fun performSearch(navigator: Navigator, query: String, global: Boolean) {
        if (global) {
            navigator.push(GlobalSearchScreen(query))
            return
        }

        if (navigator.size < 2) {
            return
        }

        when (val previousController = navigator.items[navigator.size - 2]) {
            is HomeScreen -> {
                navigator.pop()
                previousController.search(query)
            }
            is BrowseSourceScreen -> {
                navigator.pop()
                previousController.search(query)
            }
        }
    }

    /**
     * Performs a genre search using the provided genre name.
     *
     * @param genreName the search genre to the parent controller
     */
    private suspend fun performGenreSearch(navigator: Navigator, genreName: String, source: Source) {
        if (navigator.size < 2) {
            return
        }

        val previousController = navigator.items[navigator.size - 2]
        if (previousController is BrowseSourceScreen && source is HttpSource) {
            navigator.pop()
            previousController.searchGenre(genreName)
        } else {
            performSearch(navigator, genreName, global = false)
        }
    }

    /**
     * Copy Manga URL to Clipboard
     */
    private fun copyMangaUrl(context: Context, manga_: Manga?, source_: Source?) {
        val manga = manga_ ?: return
        val source = source_ as? HttpSource ?: return
        val url = source.getMangaUrl(manga.toSManga())
        context.copyToClipboard(url, url)
    }
}

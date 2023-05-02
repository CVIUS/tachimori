package eu.kanade.tachiyomi.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastAll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.domain.manga.model.isLocal
import eu.kanade.presentation.category.ChangeCategoryDialog
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.components.DialogWithCheckbox
import eu.kanade.presentation.library.LibrarySettingsDialog
import eu.kanade.presentation.library.components.LibraryContent
import eu.kanade.presentation.library.components.LibraryToolbar
import eu.kanade.presentation.manga.components.LibraryBottomActionMenu
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import tachiyomi.core.util.lang.launchIO
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.model.display
import tachiyomi.domain.library.model.sort
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen

object LibraryTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_library_enter)
            return TabOptions(
                index = 0u,
                title = stringResource(R.string.label_library),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        requestOpenSettingsSheet()
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        val screenModel = rememberScreenModel { LibraryScreenModel() }
        val settingsScreenModel = rememberScreenModel { LibrarySettingsScreenModel() }
        val state by screenModel.state.collectAsStateWithLifecycle()
        val snackbarHostState = remember { screenModel.snackbarHostState }

        Scaffold(
            topBar = { scrollBehavior ->
                val title = state.getToolbarTitle(
                    defaultTitle = stringResource(R.string.label_library),
                    defaultCategoryTitle = stringResource(R.string.label_default),
                    page = screenModel.activeCategoryIndex,
                )
                val tabVisible = state.showCategoryTabs && state.categories.size > 1
                LibraryToolbar(
                    title = title,
                    categories = state.categories,
                    hasActiveFilters = state.hasActiveFilters,
                    showCategoryTabs = state.showCategoryTabs,
                    selectedCount = state.selection.size,
                    onClickUnselectAll = screenModel::clearSelection,
                    onClickSelectAll = { screenModel.selectAll(screenModel.activeCategoryIndex) },
                    onClickInvertSelection = { screenModel.invertSelection(screenModel.activeCategoryIndex) },
                    onClickFilter = { screenModel.showSettingsDialog() },
                    onClickRefresh = { screenModel.onLibraryUpdateTriggered(state.categories[screenModel.activeCategoryIndex]) },
                    onClickGlobalUpdate = { screenModel.onLibraryUpdateTriggered(null) },
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = screenModel::search,
                    scrollBehavior = scrollBehavior.takeIf { !tabVisible }, // For scroll overlay when no tab
                )
            },
            bottomBar = {
                LibraryBottomActionMenu(
                    visible = state.selectionMode,
                    onChangeCategoryClicked = screenModel::showChangeCategoryDialog,
                    onMarkAsReadClicked = { screenModel.markReadSelection(true) },
                    onMarkAsUnreadClicked = { screenModel.markReadSelection(false) },
                    onDownloadClicked = screenModel::runDownloadActionSelection
                        .takeIf { state.selection.fastAll { !it.manga.isLocal() } },
                    onDeleteClicked = screenModel::showDeleteMangaDialog,
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { contentPadding ->
            when {
                state.isLoading -> LoadingScreen(modifier = Modifier.padding(contentPadding))
                state.searchQuery.isNullOrEmpty() && !state.hasActiveFilters && state.isLibraryEmpty -> {
                    val handler = LocalUriHandler.current
                    EmptyScreen(
                        textResource = R.string.information_empty_library,
                        modifier = Modifier.padding(contentPadding),
                        actions = listOf(
                            EmptyScreenAction(
                                stringResId = R.string.getting_started_guide,
                                icon = Icons.Outlined.HelpOutline,
                                onClick = { handler.openUri("https://tachiyomi.org/help/guides/getting-started") },
                            ),
                        ),
                    )
                }
                else -> {
                    LibraryContent(
                        categories = state.categories,
                        searchQuery = state.searchQuery,
                        selection = state.selection,
                        contentPadding = contentPadding,
                        currentPage = { screenModel.activeCategoryIndex },
                        hasActiveFilters = state.hasActiveFilters,
                        showCategoryTabs = state.showCategoryTabs || !state.searchQuery.isNullOrEmpty(),
                        onChangeCurrentPage = { screenModel.activeCategoryIndex = it },
                        onMangaClicked = { navigator.push(MangaScreen(it)) },
                        onContinueReadingClicked = { it: LibraryManga ->
                            scope.launchIO {
                                val chapter = screenModel.getNextUnreadChapter(it.manga)
                                if (chapter != null) {
                                    context.startActivity(ReaderActivity.newIntent(context, chapter.mangaId, chapter.id))
                                } else {
                                    screenModel.errorNoNextChapter()
                                }
                            }
                            Unit
                        }.takeIf { state.showMangaContinueButton },
                        onToggleSelection = { screenModel.toggleSelection(it) },
                        onToggleRangeSelection = {
                            screenModel.toggleRangeSelection(it)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onRefresh = screenModel::onLibraryUpdateTriggered,
                        onGlobalSearchClicked = {
                            navigator.push(GlobalSearchScreen(screenModel.state.value.searchQuery ?: ""))
                        },
                        onClickOpenSortSheet = { screenModel.showSettingsDialog(toSort = true) },
                        onClickOpenRandomManga = {
                            scope.launch {
                                val randomItem = screenModel.getRandomLibraryItemForCurrentCategory()
                                if (state.selectionMode || randomItem == null) {
                                    screenModel.errorOpenRandomManga()
                                    return@launch
                                }
                                navigator.push(MangaScreen(randomItem.libraryManga.manga.id))
                            }
                        },
                        onChangeDisplayMode = { screenModel.showSettingsDialog(toDisplay = true) },
                        getNumberOfMangaForCategory = { state.getMangaCountForCategory(it) },
                        getDisplayModeForPage = { state.categories[it].display },
                        getSortForPage = { state.categories[it].sort },
                        getColumnsForOrientation = { screenModel.getColumnsPreferenceForCurrentOrientation(it) },
                    ) { state.getLibraryItemsByPage(it) }
                }
            }
        }

        val onDismissRequest = { screenModel.setDialog(null) }
        when (val dialog = state.dialog) {
            is LibraryScreenModel.Dialog.SettingsSheet -> {
                LibrarySettingsDialog(
                    toSort = dialog.toSort,
                    toDisplay = dialog.toDisplay,
                    onDismissRequest = onDismissRequest,
                    screenModel = settingsScreenModel,
                    category = state.categories[screenModel.activeCategoryIndex],
                )
            }
            is LibraryScreenModel.Dialog.ChangeCategory -> {
                ChangeCategoryDialog(
                    favorite = dialog.manga.all(Manga::favorite),
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = {
                        screenModel.clearSelection()
                        navigator.push(CategoryScreen())
                    },
                    onConfirm = { include, exclude ->
                        screenModel.clearSelection()
                        screenModel.setMangaCategories(dialog.manga, include, exclude)
                    },
                )
            }
            is LibraryScreenModel.Dialog.DeleteManga -> {
                val downloadCount = screenModel.allDownloadCount(dialog.manga)
                val hasDownloads = screenModel.hasDownloads(dialog.manga)
                DialogWithCheckbox(
                    onDismissRequest = onDismissRequest,
                    title = stringResource(R.string.remove_from_library_),
                    text = pluralStringResource(id = R.plurals.dialog_remove_manga_desc, count = dialog.manga.size, dialog.manga.size),
                    confirmText = stringResource(android.R.string.ok),
                    onConfirm = { confirm ->
                        screenModel.removeMangas(dialog.manga, deleteChapters = confirm)
                        screenModel.clearSelection()
                    },
                    withCheckbox = hasDownloads,
                    checkboxText = pluralStringResource(
                        id = R.plurals.dialog_with_checkbox_delete,
                        count = downloadCount,
                        downloadCount,
                    ).takeIf { hasDownloads },
                )
            }
            null -> {}
        }

        BackHandler(enabled = state.selectionMode || state.searchQuery != null) {
            when {
                state.selectionMode -> screenModel.clearSelection()
                state.searchQuery != null -> screenModel.search(null)
            }
        }

        LaunchedEffect(state.selectionMode, state.dialog) {
            HomeScreen.showBottomNav(!state.selectionMode && state.dialog !is LibraryScreenModel.Dialog.SettingsSheet)
        }

        LaunchedEffect(state.isLoading) {
            if (!state.isLoading) {
                (context as? MainActivity)?.ready = true
            }
        }

        LaunchedEffect(Unit) {
            screenModel.snackbar.collectLatest { snackbar ->
                when (snackbar) {
                    is LibraryScreenModel.Snackbar.OpenRandomLibraryItemError -> {
                        val textRes = if (state.selectionMode) R.string.not_allowed else R.string.error_random_manga
                        snackbarHostState.showSnackbar(message = context.getString(textRes), withDismissAction = true)
                    }
                    LibraryScreenModel.Snackbar.NoNextChapterFound -> {
                        snackbarHostState.showSnackbar(message = context.getString(R.string.no_next_chapter), withDismissAction = true)
                    }
                    is LibraryScreenModel.Snackbar.LibraryUpdateTriggered -> {
                        val msg = when (snackbar.started) {
                            true -> if (snackbar.category != null) {
                                context.getString(R.string.updating_category_, snackbar.category.visualName(context))
                            } else {
                                context.getString(R.string.updating_library)
                            }
                            false -> context.getString(R.string.update_already_running)
                        }
                        val result = snackbarHostState.showSnackbar(
                            message = msg,
                            actionLabel = context.getString(R.string.action_cancel).takeIf { snackbar.started },
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed && snackbar.started) {
                            screenModel.onLibraryUpdateCancelled()
                        }
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            launch { queryEvent.receiveAsFlow().collect(screenModel::search) }
            launch { requestSettingsSheetEvent.receiveAsFlow().collectLatest { screenModel.showSettingsDialog() } }
        }
    }

    // For invoking search from other screen
    private val queryEvent = Channel<String>()
    suspend fun search(query: String) = queryEvent.send(query)

    // For opening settings sheet in LibraryController
    private val requestSettingsSheetEvent = Channel<Unit>()
    private suspend fun requestOpenSettingsSheet() = requestSettingsSheetEvent.send(Unit)
}

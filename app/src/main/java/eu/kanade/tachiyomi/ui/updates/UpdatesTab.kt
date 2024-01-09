package eu.kanade.tachiyomi.ui.updates

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.updates.UpdateScreen
import eu.kanade.presentation.updates.UpdatesDeleteChaptersDialog
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel.Snackbar
import kotlinx.coroutines.launch

object UpdatesTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_updates_enter)
            return TabOptions(
                index = 1u,
                title = stringResource(R.string.label_recent_updates),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(DownloadQueueScreen)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { UpdatesScreenModel() }
        val state by screenModel.state.collectAsStateWithLifecycle()
        val snackbarHostState = remember { screenModel.snackbarHostState }

        UpdateScreen(
            state = state,
            snackbarHostState = snackbarHostState,
            lastUpdated = screenModel.lastUpdated,
            relativeTime = screenModel.relativeTime,
            onClickCover = { item -> navigator.push(MangaScreen(item.update.mangaId)) },
            onSelectAll = screenModel::toggleAllSelection,
            onInvertSelection = screenModel::invertSelection,
            onUpdateLibrary = screenModel::onLibraryUpdateTriggered,
            onDownloadChapter = screenModel::downloadChapters,
            onMultiBookmarkClicked = screenModel::bookmarkUpdates,
            onMultiMarkAsReadClicked = screenModel::markUpdatesRead,
            onMultiDeleteClicked = screenModel::showConfirmDeleteChapters,
            onUpdateSelected = screenModel::toggleSelection,
            onOpenChapter = {
                val intent = ReaderActivity.newIntent(context, it.update.mangaId, it.update.chapterId)
                context.startActivity(intent)
            },
            swipeAction = screenModel.swipeAction,
            onSwipeToBookmark = screenModel::onSwipeToBookmark,
            onSwipeToMarkRead = screenModel::onSwipeToMarkRead,
        )

        val onDismissRequest = { screenModel.setDialog(null) }
        when (val dialog = state.dialog) {
            is UpdatesScreenModel.Dialog.DeleteConfirmation -> {
                UpdatesDeleteChaptersDialog(
                    onDismissRequest = onDismissRequest,
                    selected = dialog.toDelete,
                    removeBookmarkedChapters = screenModel.removeBookmarkedChapters,
                    onConfirm = { screenModel.deleteChapters(dialog.toDelete) },
                    goToSettings = {
                        screenModel.toggleAllSelection(false)
                        navigator.push(SettingsScreen.toDownloadScreen())
                    },
                )
            }
            null -> {}
        }

        LaunchedEffect(screenModel.snackbar) {
            launch {
                var isUndoing = false
                screenModel.snackbar.collect { snackbar ->
                    when (snackbar) {
                        Snackbar.InternalError -> snackbarHostState.showSnackbar(context.getString(R.string.internal_error))
                        is Snackbar.LibraryUpdateTriggered -> {
                            val msgRes = if (snackbar.started) R.string.updating_library else R.string.update_already_running
                            val result = snackbarHostState.showSnackbar(
                                message = context.getString(msgRes),
                                actionLabel = context.getString(R.string.action_cancel).takeIf { snackbar.started },
                                duration = SnackbarDuration.Short,
                            )
                            if (result == SnackbarResult.ActionPerformed && snackbar.started) {
                                screenModel.onLibraryUpdateCancelled()
                            }
                        }
                        is Snackbar.OnSwipeToMarkRead -> {
                            val textRes = if (snackbar.read) "Marked as read" else "Marked as unread"
                            val result = snackbarHostState.showSnackbar(
                                message = textRes,
                                actionLabel = context.getString(R.string.action_undo),
                                duration = SnackbarDuration.Short,
                            )
                            val item = snackbar.update
                            when (result) {
                                SnackbarResult.ActionPerformed -> {
                                    screenModel.onSwipeToMarkRead(item, !snackbar.read, snackbar.lastPageRead, showSnackbar = false)
                                    isUndoing = !isUndoing
                                }
                                SnackbarResult.Dismissed -> {
                                    if (!isUndoing && !item.update.read) {
                                        if (screenModel.removeAfterMarkedAsRead) {
                                            screenModel.deleteChapters(listOf(item))
                                        }
                                    }
                                }
                            }
                        }
                        is Snackbar.OnSwipeToBookmark -> {
                            val textRes = if (snackbar.bookmark) "Bookmarked" else "Unbookmarked"
                            val result = snackbarHostState.showSnackbar(
                                message = textRes,
                                actionLabel = context.getString(R.string.action_undo),
                                duration = SnackbarDuration.Short,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                screenModel.onSwipeToBookmark(snackbar.update, !snackbar.bookmark, showSnackbar = false)
                            }
                        }
                    }
                }
            }
        }

        LaunchedEffect(state.selectionMode) {
            HomeScreen.showBottomNav(!state.selectionMode)
        }

        LaunchedEffect(state.isLoading) {
            if (!state.isLoading) {
                (context as? MainActivity)?.ready = true
            }
        }
        DisposableEffect(Unit) {
            screenModel.resetNewUpdatesCount()

            onDispose {
                screenModel.resetNewUpdatesCount()
            }
        }
    }
}

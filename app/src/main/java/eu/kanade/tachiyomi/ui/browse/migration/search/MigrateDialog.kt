package eu.kanade.tachiyomi.ui.browse.migration.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import cafe.adriel.voyager.core.model.ScreenModel
import eu.kanade.domain.chapter.interactor.SyncChaptersWithSource
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.manga.model.hasCustomCover
import eu.kanade.domain.manga.model.toSManga
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.track.EnhancedTrackService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.ui.browse.migration.MigrationFlags
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.delay
import tachiyomi.core.preference.Preference
import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.lang.withUIContext
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.interactor.GetChapterByMangaId
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.history.interactor.GetHistoryByMangaId
import tachiyomi.domain.history.interactor.RemoveHistory
import tachiyomi.domain.history.interactor.UpsertHistory
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Divider
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.isScrolledToEnd
import tachiyomi.presentation.core.util.isScrolledToStart
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun MigrateDialog(
    oldManga: Manga,
    newManga: Manga,
    isMangaScreen: Boolean = false,
    screenModel: MigrateDialogScreenModel,
    onDismissRequest: () -> Unit,
    onClickTitle: () -> Unit,
    onPopScreen: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val activeFlags = remember { MigrationFlags.getEnabledFlagsPositions(screenModel.migrateFlags.get()) }
    val items = remember {
        MigrationFlags.titles(oldManga)
            .map { context.getString(it) }
            .toList()
    }
    val selected = remember {
        mutableStateListOf(*List(items.size) { i -> activeFlags.contains(i) }.toTypedArray())
    }
    val newFlags: () -> Int = {
        val selectedIndices = mutableListOf<Int>()
        selected.fastForEachIndexed { i, b -> if (b) selectedIndices.add(i) }
        MigrationFlags.getFlagsFromPositions(selectedIndices.toTypedArray())
    }

    var started by rememberSaveable { mutableStateOf(false) }

    AnimatedVisibility(
        visible = started,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        LoadingScreen(modifier = Modifier.background(MaterialTheme.colorScheme.scrim))
    }

    AnimatedVisibility(
        visible = !started,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(text = stringResource(R.string.migration_dialog_what_to_include))
            },
            text = {
                Box {
                    val listState = rememberLazyListState()
                    ScrollbarLazyColumn(state = listState) {
                        items.forEachIndexed { index, title ->
                            item {
                                val onChange: () -> Unit = {
                                    selected[index] = !selected[index]
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = onChange)
                                        .minimumInteractiveComponentSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        modifier = Modifier.heightIn(min = 48.dp),
                                        checked = selected[index],
                                        onCheckedChange = null,
                                    )
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium.merge(),
                                        modifier = Modifier.padding(start = MaterialTheme.padding.medium),
                                    )
                                }
                            }
                        }
                    }

                    if (!listState.isScrolledToStart()) Divider(modifier = Modifier.align(Alignment.TopCenter))
                    if (!listState.isScrolledToEnd()) Divider(modifier = Modifier.align(Alignment.BottomCenter))
                }
            },
            confirmButton = {
                Row {
                    if (!isMangaScreen) {
                        TextButton(
                            onClick = {
                                onClickTitle()
                                onDismissRequest()
                            },
                        ) {
                            Text(text = stringResource(R.string.action_show_manga))
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            scope.launchIO {
                                started = true
                                screenModel.migrateFlags.set(newFlags())
                                delay(2.seconds)

                                val result = screenModel.migrateManga(oldManga, newManga, copy = true)
                                if (!result) {
                                    context.toast(R.string.migration_copy_failed)
                                    onDismissRequest()
                                    return@launchIO
                                }

                                withUIContext {
                                    onPopScreen()
                                    onDismissRequest()
                                    context.toast(R.string.migration_copy_success)
                                }
                            }
                        },
                    ) {
                        Text(text = stringResource(R.string.copy))
                    }
                    TextButton(
                        onClick = {
                            scope.launchIO {
                                started = true
                                screenModel.migrateFlags.set(newFlags())
                                delay(2.seconds)

                                val result = screenModel.migrateManga(oldManga, newManga, copy = false)
                                if (!result) {
                                    context.toast(R.string.migration_migrate_failed)
                                    onDismissRequest()
                                    return@launchIO
                                }

                                withUIContext {
                                    onPopScreen()
                                    onDismissRequest()
                                    context.toast(R.string.migration_migrate_success)
                                }
                            }
                        },
                    ) {
                        Text(text = stringResource(R.string.migrate))
                    }
                }
            },
        )
    }
}

internal class MigrateDialogScreenModel(
    private val sourceManager: SourceManager = Injekt.get(),
    private val updateManga: UpdateManga = Injekt.get(),
    private val getChapterByMangaId: GetChapterByMangaId = Injekt.get(),
    private val getHistoryByMangaId: GetHistoryByMangaId = Injekt.get(),
    private val removeHistory: RemoveHistory = Injekt.get(),
    private val syncChaptersWithSource: SyncChaptersWithSource = Injekt.get(),
    private val updateChapter: UpdateChapter = Injekt.get(),
    private val upsertHistory: UpsertHistory = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val setMangaCategories: SetMangaCategories = Injekt.get(),
    private val getTracks: GetTracks = Injekt.get(),
    private val insertTrack: InsertTrack = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get(),
    private val preferenceStore: PreferenceStore = Injekt.get(),
) : ScreenModel {

    val migrateFlags: Preference<Int> by lazy {
        preferenceStore.getInt("migrate_flags", Int.MAX_VALUE)
    }

    private val enhancedServices by lazy {
        Injekt.get<TrackManager>().services.filterIsInstance<EnhancedTrackService>()
    }

    suspend fun migrateManga(oldManga: Manga, newManga: Manga, copy: Boolean): Boolean {
        val source = sourceManager.get(newManga.source) ?: return false
        val prevSource = sourceManager.get(oldManga.source)

        if (oldManga.id == newManga.id) return false // Nothing to migrate

        return try {
            val chapters = source.getChapterList(newManga.toSManga())

            migrateMangaInternal(
                oldSource = prevSource,
                newSource = source,
                oldManga = oldManga,
                newManga = newManga,
                sourceChapters = chapters,
                replace = !copy,
            )
            true
        } catch (_: Throwable) {
            false
        }
    }

    private suspend fun migrateMangaInternal(
        oldSource: Source?,
        newSource: Source,
        oldManga: Manga,
        newManga: Manga,
        sourceChapters: List<SChapter>,
        replace: Boolean,
    ) {
        val flags = migrateFlags.get()

        try {
            syncChaptersWithSource.await(sourceChapters, newManga, newSource)
        } catch (_: Exception) {
            // Worst case, chapters won't be synced
        }

        // Update chapters including history
        if (MigrationFlags.hasChapters(flags)) {
            val prevHistoryList = getHistoryByMangaId.await(oldManga.id)
            val prevMangaChapters = getChapterByMangaId.await(oldManga.id)
            val newMangaChapters = getChapterByMangaId.await(newManga.id)

            val maxPrevChapterRead = prevMangaChapters
                .filter(Chapter::read)
                .maxOfOrNull(Chapter::chapterNumber)
                ?: 0f

            val newChapterUpdates = mutableListOf<ChapterUpdate>()
            val oldChapterUpdates = mutableListOf<ChapterUpdate>()
            val newHistoryUpdates = mutableListOf<HistoryUpdate>()
            val oldHistory = mutableListOf<History>()

            for (newChapter in newMangaChapters) {
                if (newChapter.isRecognizedNumber) {
                    val prevChapter = prevMangaChapters
                        .find { it.isRecognizedNumber && it.chapterNumber == newChapter.chapterNumber }

                    if (prevChapter != null) {
                        newChapterUpdates += ChapterUpdate(
                            id = newChapter.id,
                            read = prevChapter.read,
                            bookmark = prevChapter.bookmark,
                            dateFetch = prevChapter.dateFetch,
                        )

                        oldChapterUpdates += ChapterUpdate(
                            id = prevChapter.id,
                            read = if (prevChapter.read) false else null,
                            bookmark = if (prevChapter.bookmark) false else null,
                        )

                        prevHistoryList.find { it.chapterId == prevChapter.id }?.let { prevHistory ->
                            // Don't migrate/copy the history of not fully read chapters
                            // because we do not include lastPageRead in migration
                            // since number of pages varies on sources
                            if (prevChapter.lastPageRead > 0L) return@let

                            newHistoryUpdates += HistoryUpdate(
                                chapterId = newChapter.id,
                                readAt = prevHistory.readAt ?: return@let,
                                sessionReadDuration = prevHistory.readDuration,
                            )

                            oldHistory += History(
                                id = prevHistory.id,
                                chapterId = prevHistory.chapterId,
                                readAt = prevHistory.readAt,
                                readDuration = prevHistory.readDuration,
                            )
                        }
                    } else if (newChapter.chapterNumber <= maxPrevChapterRead) {
                        newChapterUpdates += ChapterUpdate(
                            id = newChapter.id,
                            read = true,
                        )
                    }
                }
            }

            updateChapter.awaitAll(newChapterUpdates)
            newHistoryUpdates.forEach { upsertHistory.await(it) }

            if (replace) {
                updateChapter.awaitAll(oldChapterUpdates)
                oldHistory.map(History::id).forEach { removeHistory.awaitOne(it) }
            }
        }

        // Update categories
        if (MigrationFlags.hasCategories(flags)) {
            val categoryIds = getCategories.await(oldManga.id).map(Category::id).toList()
            setMangaCategories.await(newManga.id, categoryIds)
        }

        // Update tracks
        if (MigrationFlags.hasTracks(flags)) {
            val tracks = getTracks.await(oldManga.id).mapNotNull { track ->
                val updatedTrack = track.copy(mangaId = newManga.id)

                val service = enhancedServices
                    .firstOrNull { it.isTrackFrom(updatedTrack, oldManga, oldSource) }

                if (service != null) {
                    service.migrateTrack(updatedTrack, newManga, newSource)
                } else {
                    updatedTrack
                }
            }
            insertTrack.awaitAll(tracks)
        }

        // Update old manga
        if (replace) {
            updateManga.await(
                MangaUpdate(
                    id = oldManga.id,
                    favorite = false,
                    dateAdded = 0L,
                    viewerFlags = 0L,
                    chapterFlags = 0L,
                ),
            )
        }

        // Update custom cover
        if (MigrationFlags.hasCustomCover(flags)) {
            if (oldManga.hasCustomCover(coverCache)) {
                coverCache.setCustomCoverToCache(newManga, coverCache.getCustomCoverFile(oldManga.id).inputStream())
                updateManga.awaitUpdateCoverLastModified(newManga.id)

                if (replace) {
                    coverCache.deleteCustomCover(oldManga.id)
                    updateManga.awaitUpdateCoverLastModified(oldManga.id)
                }
            }
        }

        // Update extras
        if (MigrationFlags.hasExtras(flags)) {
            updateManga.await(
                MangaUpdate(
                    id = newManga.id,
                    chapterFlags = oldManga.chapterFlags,
                    viewerFlags = oldManga.viewerFlags,
                ),
            )
        }

        // Update new manga
        updateManga.await(
            MangaUpdate(
                id = newManga.id,
                favorite = true,
                dateAdded = if (replace) oldManga.dateAdded else Date().time,
            ),
        )
    }
}

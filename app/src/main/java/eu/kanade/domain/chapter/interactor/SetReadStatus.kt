package eu.kanade.domain.chapter.interactor

import eu.kanade.domain.download.interactor.DeleteDownload
import kotlinx.coroutines.isActive
import logcat.LogPriority
import tachiyomi.core.util.lang.withNonCancellableContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository

class SetReadStatus(
    private val downloadPreferences: DownloadPreferences,
    private val deleteDownload: DeleteDownload,
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
) {

    private val mapper = { chapter: Chapter, read: Boolean, lastPageRead: Long ->
        ChapterUpdate(
            id = chapter.id,
            read = read,
            lastPageRead = lastPageRead,
        )
    }

    suspend fun await(read: Boolean, lastPageRead: Long? = null, vararg chapters: Chapter): Result = withNonCancellableContext {
        val chaptersToUpdate = chapters.map {
            it.copy(
                read = when (read) {
                    true -> it.read
                    false -> !it.read
                },
                lastPageRead = when (lastPageRead) {
                    null -> 0L
                    else -> it.lastPageRead
                },
            )
        }.run {
            if (!isActive) {
                this.filter {
                    when (read) {
                        true -> !it.read
                        false -> it.read || it.lastPageRead > 0
                    }
                }
            } else {
                this
            }
        }

        if (chaptersToUpdate.isEmpty()) {
            return@withNonCancellableContext Result.NoChapters
        }

        try {
            chapterRepository.updateAll(
                chaptersToUpdate.map { mapper(it, read, lastPageRead ?: 0) },
            )
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            return@withNonCancellableContext Result.InternalError(e)
        }

        // Handled by snackbar result
        if (read && lastPageRead == null && downloadPreferences.removeAfterMarkedAsRead().get()) {
            chaptersToUpdate
                .groupBy { it.mangaId }
                .forEach { (mangaId, chapters) ->
                    deleteDownload.awaitAll(
                        manga = mangaRepository.getMangaById(mangaId),
                        chapters = chapters.toTypedArray(),
                    )
                }
        }

        Result.Success
    }

    suspend fun await(mangaId: Long, read: Boolean): Result = withNonCancellableContext {
        await(
            read = read,
            chapters = chapterRepository
                .getChapterByMangaId(mangaId)
                .toTypedArray(),
        )
    }

    suspend fun await(manga: Manga, read: Boolean) =
        await(manga.id, read)

    sealed class Result {
        object Success : Result()
        object NoChapters : Result()
        data class InternalError(val error: Throwable) : Result()
    }
}

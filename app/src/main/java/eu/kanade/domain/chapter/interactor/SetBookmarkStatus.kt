package eu.kanade.domain.chapter.interactor

import kotlinx.coroutines.isActive
import logcat.LogPriority
import tachiyomi.core.util.lang.withNonCancellableContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository

class SetBookmarkStatus(
    private val chapterRepository: ChapterRepository,
) {
    private val mapper = { chapter: Chapter, bookmark: Boolean ->
        ChapterUpdate(
            id = chapter.id,
            bookmark = bookmark,
        )
    }

    suspend fun await(bookmark: Boolean, vararg chapters: Chapter): Result = withNonCancellableContext {
        val chaptersToUpdate = chapters.map {
            it.copy(
                bookmark = when (bookmark) {
                    true -> it.bookmark
                    false -> !it.bookmark
                },
            )
        }.run {
            if (!isActive) {
                this.filter {
                    when (bookmark) {
                        true -> !it.bookmark
                        false -> it.bookmark
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
                chaptersToUpdate.map { mapper(it, bookmark) },
            )
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            return@withNonCancellableContext Result.InternalError(e)
        }

        Result.Success
    }

    sealed class Result {
        object Success : Result()
        object NoChapters : Result()
        data class InternalError(val error: Throwable) : Result()
    }
}

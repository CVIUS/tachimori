package tachiyomi.domain.history.interactor

import tachiyomi.domain.history.repository.HistoryRepository

class RemoveHistory(
    private val repository: HistoryRepository,
) {

    suspend fun awaitAll(): Boolean {
        return repository.deleteAllHistory()
    }

    suspend fun awaitOne(historyId: Long) {
        repository.resetHistory(historyId)
    }

    suspend fun await(mangaId: Long) {
        repository.resetHistoryByMangaId(mangaId)
    }
}

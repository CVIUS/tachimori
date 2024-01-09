package eu.kanade.tachiyomi.ui.manga

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import com.google.errorprone.annotations.Immutable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import tachiyomi.core.util.lang.launchIO
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.GetMangaWithChapters
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaScreenModel(
    val mangaId: Long,
    private val getMangaAndChapters: GetMangaWithChapters = Injekt.get(),
) : StateScreenModel<MangaInfoState>(MangaInfoState.Loading) {

    private fun updateSuccessState(func: (MangaInfoState.Success) -> MangaInfoState.Success) {
        mutableState.update { if (it is MangaInfoState.Success) func(it) else it }
    }
    init {
        coroutineScope.launchIO {
            getMangaAndChapters.subscribe(mangaId).distinctUntilChanged()
                .collectLatest { (manga, chapters) ->
                    updateSuccessState {
                        it.copy(
                            manga = manga,
                            chapters = chapters,
                        )
                    }
                }
        }
    }
}
sealed class MangaInfoState {
    @Immutable
    object Loading : MangaInfoState()

    @Immutable
    data class Success(val manga: Manga, val chapters: List<Chapter>) : MangaInfoState()
}

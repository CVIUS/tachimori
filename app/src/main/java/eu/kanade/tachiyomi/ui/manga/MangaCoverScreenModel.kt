package eu.kanade.tachiyomi.ui.manga

import android.app.Application
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.saver.Image
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.data.saver.Location
import eu.kanade.tachiyomi.util.editCover
import eu.kanade.tachiyomi.util.system.toShareIntent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.core.util.lang.withUIContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaCoverScreenModel(
    private val mangaId: Long,
    private val getManga: GetManga = Injekt.get(),
    private val imageSaver: ImageSaver = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get(),
    private val updateManga: UpdateManga = Injekt.get(),
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
) : StateScreenModel<Manga?>(null) {

    private val _snackbar: Channel<Snackbar> = Channel(Channel.CONFLATED)
    val snackbar: Flow<Snackbar> = _snackbar.receiveAsFlow()

    init {
        coroutineScope.launchIO {
            getManga.subscribe(mangaId)
                .collect { newManga -> mutableState.update { newManga } }
        }
    }

    fun saveCover(context: Context) {
        coroutineScope.launch {
            try {
                saveCoverInternal(context, temp = false)
                _snackbar.send(Snackbar.CoverSaved)
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e)
                _snackbar.send(Snackbar.CoverSaveError)
            }
        }
    }

    fun shareCover(context: Context) {
        coroutineScope.launch {
            try {
                val uri = saveCoverInternal(context, temp = true) ?: return@launch
                withUIContext {
                    context.startActivity(uri.toShareIntent(context))
                }
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e)
                _snackbar.send(Snackbar.CoverShareError)
            }
        }
    }

    /**
     * Save manga cover Bitmap to picture or temporary share directory.
     *
     * @param context The context for building and executing the ImageRequest
     * @return the uri to saved file
     */
    private suspend fun saveCoverInternal(context: Context, temp: Boolean): Uri? {
        val manga = state.value ?: return null
        val req = ImageRequest.Builder(context)
            .data(manga)
            .size(Size.ORIGINAL)
            .build()

        return withIOContext {
            val result = context.imageLoader.execute(req).drawable

            // TODO: Handle animated cover
            val bitmap = (result as? BitmapDrawable)?.bitmap ?: return@withIOContext null
            imageSaver.save(
                Image.Cover(
                    bitmap = bitmap,
                    name = manga.title,
                    location = if (temp) Location.Cache else Location.Pictures.create(),
                ),
            )
        }
    }

    /**
     * Update cover with local file.
     *
     * @param data uri of the cover resource.
     */
    fun editCover(data: Uri) {
        val manga = state.value ?: return
        coroutineScope.launchIO {
            @Suppress("BlockingMethodInNonBlockingContext")
            Injekt.get<Application>().contentResolver.openInputStream(data)?.use {
                try {
                    manga.editCover(Injekt.get(), it, updateManga, coverCache)
                    notifyCoverUpdated()
                } catch (e: Exception) {
                    notifyFailedCoverUpdate(e)
                }
            }
        }
    }

    fun deleteCustomCover() {
        val mangaId = state.value?.id ?: return
        coroutineScope.launchIO {
            try {
                coverCache.deleteCustomCover(mangaId)
                updateManga.awaitUpdateCoverLastModified(mangaId)
                notifyCoverUpdated()
            } catch (e: Exception) {
                notifyFailedCoverUpdate(e)
            }
        }
    }

    private fun notifyCoverUpdated() {
        coroutineScope.launch {
            _snackbar.send(Snackbar.CoverUpdated)
        }
    }

    private fun notifyFailedCoverUpdate(e: Throwable) {
        coroutineScope.launch {
            _snackbar.send(Snackbar.CoverUpdateFailed)
            logcat(LogPriority.ERROR, e)
        }
    }

    sealed class Snackbar {
        object CoverSaved : Snackbar()
        object CoverSaveError : Snackbar()
        object CoverUpdated : Snackbar()
        object CoverUpdateFailed : Snackbar()
        object CoverShareError : Snackbar()
    }
}

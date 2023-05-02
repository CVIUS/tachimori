package eu.kanade.tachiyomi.util.chapter

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.R
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import eu.kanade.tachiyomi.data.database.models.Chapter as DbChapter

val chapterDecimalFormat = DecimalFormat(
    "#.###",
    DecimalFormatSymbols()
        .apply { decimalSeparator = '.' },
)

fun Chapter.preferredChapterName(context: Context, manga: Manga): String {
    return if (manga.displayMode == Manga.CHAPTER_DISPLAY_NUMBER && isRecognizedNumber) {
        context.getString(
            R.string.display_mode_chapter,
            chapterDecimalFormat.format(chapterNumber.toDouble()),
        )
    } else {
        name
    }
}

fun DbChapter.preferredChapterName(context: Context, manga: Manga): String {
    return if (manga.displayMode == Manga.CHAPTER_DISPLAY_NUMBER && chapter_number >= 0f) {
        context.getString(
            R.string.display_mode_chapter,
            chapterDecimalFormat.format(chapter_number.toDouble()),
        )
    } else {
        name
    }
}

@Composable
fun Chapter.preferredChapterName(manga: Manga): String {
    return if (manga.displayMode == Manga.CHAPTER_DISPLAY_NUMBER && isRecognizedNumber) {
        stringResource(
            R.string.display_mode_chapter,
            chapterDecimalFormat.format(chapterNumber.toDouble()),
        )
    } else {
        name
    }
}

@Composable
fun Manga?.preferredChapterName(chapterName: String, chapterNumber: Float): String {
    if (this == null) return chapterName
    return if (displayMode == Manga.CHAPTER_DISPLAY_NUMBER && chapterNumber >= 0f) {
        stringResource(
            R.string.display_mode_chapter,
            chapterDecimalFormat.format(chapterNumber.toDouble()),
        )
    } else {
        chapterName
    }
}

package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastAny
import eu.kanade.tachiyomi.R
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.presentation.core.components.material.padding

@Composable
fun ContinueReadingItem(
    nextChapter: Chapter?,
    chapters: List<Chapter>,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    val read = chapters.fastAny { it.read }

    if (chapters.isEmpty()) return
    Button(
        onClick = onClick,
        enabled = enabled && nextChapter != null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.tiny,
            ),
    ) {
        val textRes = if (nextChapter != null) {
            if (read) {
                R.string.action_continue_reading
            } else {
                R.string.action_start_reading
            }
        } else {
            R.string.all_chapters_read
        }

        Text(text = stringResource(textRes))
    }
}

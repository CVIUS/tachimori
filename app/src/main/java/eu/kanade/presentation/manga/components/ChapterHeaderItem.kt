package eu.kanade.presentation.manga.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.R
import tachiyomi.presentation.core.components.material.padding

@Composable
fun ChapterHeaderItem(
    enabled: Boolean,
    chapterCount: Int?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = MaterialTheme.padding.medium)
            .minimumInteractiveComponentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (chapterCount == null) {
                stringResource(R.string.chapters)
            } else {
                pluralStringResource(id = R.plurals.manga_num_chapters, count = chapterCount, chapterCount)
            },
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onBackground,
            ),
        )
    }
}

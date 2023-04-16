package eu.kanade.presentation.history.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.chapter.chapterDecimalFormat
import eu.kanade.tachiyomi.util.lang.toTimestampString
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.components.material.padding

private val HISTORY_ITEM_HEIGHT = 96.dp

@Composable
fun HistoryItem(
    manga: Manga?,
    modifier: Modifier = Modifier,
    history: HistoryWithRelations,
    onClickCover: () -> Unit,
    onClickResume: () -> Unit,
    onClickDelete: (String) -> Unit,
) {
    val displayNumber = remember(manga) { manga?.displayMode == Manga.CHAPTER_DISPLAY_NUMBER && history.chapterNumber >= 0f }
    val prefChapName = if (displayNumber) {
        stringResource(
            R.string.display_mode_chapter,
            chapterDecimalFormat.format(history.chapterNumber.toDouble()),
        )
    } else {
        history.chapterName
    }
    val msg = prefChapName.takeIf { manga != null } ?: history.chapterName

    Row(
        modifier = modifier
            .clickable(onClick = onClickResume)
            .height(HISTORY_ITEM_HEIGHT)
            .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MangaCover.Book(
            modifier = Modifier.fillMaxHeight(),
            data = history.coverData,
            onClick = onClickCover,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = MaterialTheme.padding.medium, end = MaterialTheme.padding.small),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = history.title,
                maxLines = 1,
                style = MaterialTheme.typography.titleSmall,
                overflow = TextOverflow.Ellipsis,
            )
            val readAt = remember { history.readAt?.toTimestampString() }
            Text(
                text = msg,
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (readAt != null) {
                Text(
                    text = readAt,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        IconButton(onClick = { onClickDelete(msg) }) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.action_delete),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

package eu.kanade.presentation.manga.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.util.lang.toRelativeString
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.util.secondaryItemAlpha
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.*

@Composable
fun BaseMangaListItem(
    modifier: Modifier = Modifier,
    manga: Manga,
    onClickItem: () -> Unit = {},
    onClickCover: () -> Unit = onClickItem,
    cover: @Composable RowScope.() -> Unit = { defaultCover(manga, onClickCover) },
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable RowScope.() -> Unit = { defaultContent(manga) },
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClickItem)
            .height(56.dp)
            .padding(horizontal = MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cover()
        content()
        actions()
    }
}

private val defaultCover: @Composable RowScope.(Manga, () -> Unit) -> Unit = { manga, onClick ->
    MangaCover.Square(
        modifier = Modifier
            .padding(vertical = MaterialTheme.padding.small)
            .fillMaxHeight(),
        data = manga,
        onClick = onClick,
    )
}

private val defaultContent: @Composable RowScope.(Manga) -> Unit = {
    val context = LocalContext.current

    val uiPreferences = Injekt.get<UiPreferences>()

    val dateAdded = Date(it.dateAdded).toRelativeString(
        context,
        uiPreferences.relativeTime().get(),
        UiPreferences.dateFormat(uiPreferences.dateFormat().get()),
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .padding(start = MaterialTheme.padding.medium),
    ) {
        Text(
            text = it.title,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                modifier = Modifier
                    .size(MaterialTheme.padding.medium)
                    .secondaryItemAlpha(),
            )
            Spacer(modifier = Modifier.padding(2.dp))
            Text(
                text = dateAdded,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.secondaryItemAlpha(),
            )
        }
    }
}

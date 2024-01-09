package eu.kanade.tachiyomi.ui.manga

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Divider
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.util.plus

class MangaInfoScreen(private val mangaId: Long) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel { MangaScreenModel(mangaId = mangaId) }
        val state by screenModel.state.collectAsStateWithLifecycle()

        val successState = state as MangaInfoState.Success

        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    AppBar(
                        title = successState.manga.title.ifBlank { stringResource(R.string.unknown_title) },
                        subtitle = "Details",
                        navigateUp = navigator::pop,
                    )
                    Divider()
                }
            },
        ) { contentPadding ->
            ScrollbarLazyColumn(
                contentPadding = contentPadding + topSmallPaddingValues,
            ) {
                item {
                    MangaInfoDescription(successState.manga.description)
                }
            }
        }
    }
}

@Composable
private fun MangaInfoDescription(description: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = description.takeIf { !it.isNullOrBlank() } ?: stringResource(R.string.description_placeholder),
            modifier = Modifier
                .padding(
                    vertical = MaterialTheme.padding.small,
                    horizontal = MaterialTheme.padding.medium,
                ),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Divider(modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium))
    }
}

@Composable
private fun MangaInfoChapterAndSource() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Outlined.LibraryBooks, contentDescription = null)
            Spacer(modifier = Modifier.width(32.dp))
            Text(
                text = "",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Divider(modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium))
    }
}

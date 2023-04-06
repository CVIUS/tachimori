package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.tachiyomi.R

@Composable
fun GlobalSearchToolbar(
    searchQuery: String?,
    progress: Int,
    total: Int,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
) {
    Box {
        SearchToolbar(
            searchQuery = searchQuery,
            onChangeSearchQuery = onChangeSearchQuery,
            onSearch = onSearch,
            onClickCloseSearch = navigateUp,
            navigateUp = navigateUp,
            placeholderText = stringResource(R.string.action_global_search_hint),
        )
        if (progress in 1 until total) {
            LinearProgressIndicator(
                progress = progress / total.toFloat(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
            )
        } else {
            Divider(
                modifier = Modifier
                    .align(Alignment.BottomCenter),
            )
        }
    }
}

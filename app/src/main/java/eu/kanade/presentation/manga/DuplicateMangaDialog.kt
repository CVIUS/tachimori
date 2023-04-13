package eu.kanade.presentation.manga

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.R

@Composable
fun DuplicateMangaDialog(
    onDismissRequest: () -> Unit,
    onMigrate: () -> Unit,
    onConfirm: () -> Unit,
    onOpenManga: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Column {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onDismissRequest()
                        onOpenManga()
                    },
                ) {
                    Text(text = stringResource(R.string.action_show_manga))
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onDismissRequest()
                        onMigrate()
                    },
                ) {
                    Text(text = stringResource(R.string.action_migrate))
                }
                ElevatedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onDismissRequest()
                        onConfirm()
                    },
                ) {
                    Text(text = stringResource(R.string.action_just_add))
                }
            }
        },
        title = {
            Text(text = stringResource(R.string.action_choose))
        },
        text = {
            Text(text = stringResource(R.string.dialog_duplicate_desc))
        },
    )
}

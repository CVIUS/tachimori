package eu.kanade.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tachiyomi.core.preference.Preference

@Composable
fun <T> Preference<T>.collectAsStateWithLifecycle(): State<T> {
    val flow = remember(this) { changes() }
    return flow.collectAsStateWithLifecycle(initialValue = get())
}

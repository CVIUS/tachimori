package tachiyomi.presentation.core.util

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import tachiyomi.presentation.core.components.material.SecondaryItemAlpha

fun Modifier.selectedBackground(isSelected: Boolean): Modifier = composed {
    if (isSelected) {
        val alpha = if (isSystemInDarkTheme()) 0.16f else 0.22f
        background(MaterialTheme.colorScheme.secondary.copy(alpha = alpha))
    } else {
        this
    }
}

fun Modifier.secondaryItemAlpha(): Modifier = this.alpha(SecondaryItemAlpha)

/** Ugly fix for [https://issuetracker.google.com/issues/221643630](https://issuetracker.google.com/issues/221643630)  */
fun Modifier.alertDialogWidth(): Modifier = composed {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    this.requiredWidthIn(
        280.dp,
        min((screenWidth - (screenWidth / 6)).dp, 560.dp),
    )
}

fun Modifier.clickableNoIndication(
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    this.combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onLongClick = onLongClick,
        onClick = onClick,
    )
}

/**
 * For TextField, the provided [action] will be invoked when
 * physical enter key is pressed.
 *
 * Naturally, the TextField should be set to single line only.
 */
fun Modifier.runOnEnterKeyPressed(action: () -> Unit): Modifier = this.onPreviewKeyEvent {
    when (it.key) {
        Key.Enter, Key.NumPadEnter -> {
            action()
            true
        }
        else -> false
    }
}

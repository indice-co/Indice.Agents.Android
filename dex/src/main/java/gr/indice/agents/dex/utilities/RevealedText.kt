package gr.indice.agents.dex.utilities

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos

private const val MIN_STEP = 2
private const val CATCH_UP = 10


@Composable
internal fun rememberRevealedText(text: String, animate: Boolean): String {
    var revealed by rememberSaveable { mutableIntStateOf(if (animate) 0 else text.length) }

    LaunchedEffect(text, animate) {
        if (!animate) {
            revealed = text.length
            return@LaunchedEffect
        }
        revealed = revealed.coerceAtMost(text.length)
        while (revealed < text.length) {
            withFrameNanos { }
            val remaining = text.length - revealed
            revealed += maxOf(MIN_STEP, remaining / CATCH_UP).coerceAtMost(remaining)
        }
    }

    return if (animate) text.take(revealed) else text
}

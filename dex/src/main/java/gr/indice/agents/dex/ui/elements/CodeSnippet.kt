package gr.indice.agents.dex.ui.elements

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontFamily
import gr.indice.agents.dex.ui.theme.default
import gr.indice.agents.dex.ui.theme.small
import kotlinx.coroutines.launch

object CodeSnippet {

    @Composable
    fun View(
        modifier: Modifier = Modifier,
        code: String
    ) {
        Box(modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(small))
            .background(MaterialTheme.colorScheme.secondaryContainer)
        ) {
            val clipboard = LocalClipboard.current
            val scope = rememberCoroutineScope()

            SelectionContainer {
                Text(
                    text = code,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    softWrap = false,
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(default)
                        .padding(bottom = default)
                )
            }

            IconButton(
                modifier = Modifier.align(Alignment.BottomEnd),
                onClick = {
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipData.newPlainText("Code", code).toClipEntry()
                        )
                    }
                }
            ) {
                Icon(Icons.Default.ContentCopy, "Copy")
            }
        }
    }
}
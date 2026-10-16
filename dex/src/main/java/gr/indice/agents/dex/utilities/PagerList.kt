package gr.indice.agents.dex.utilities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import gr.indice.agents.dex.ui.theme.default


fun <T: Any> LazyListScope.pagerList(
    items: LazyPagingItems<T>?,
    customContent: (@Composable () -> Unit)? = null,
    checkForHeader: (T, T?) -> Boolean,
    headerContent: @Composable (T)-> Unit,
    itemContent: @Composable (Int, T) -> Unit,
    emptyContent: (@Composable () -> Unit)? = null,
    bottomSpacer: (@Composable () -> Unit)? = null,
    divider: (@Composable () -> Unit)? = null,
) {

    customContent?.let {
        item(key = it.hashCode()) {
            it()
        }
    }

    if ((items?.itemCount ?: 0) <= 0) {
        item { emptyContent?.invoke() }
    } else if (items != null) {
        for (index in 0 until items.itemCount) {
            val currentItem = items.peek(index)!!
            val prevItem = if (index > 0) items.peek(index - 1)!! else null
            val addHeader = checkForHeader(currentItem, prevItem)

            if (addHeader && prevItem != null) {
                divider?.let {
                    item {
                        Box(
                            modifier = Modifier.padding(vertical = default)
                        ) { it() }
                    }
                }
            }

            if (addHeader) {
                stickyHeader {
                    val colors = LocalColors.current
                    Box(
                        modifier = Modifier
                            .background(colors.background).padding(vertical = default),
                        contentAlignment = Alignment.Center
                    ) {
                        headerContent(currentItem)
                    }
                }
            }



            item(key = "${index}_${currentItem.hashCode()}") {
                itemContent(index, currentItem)
            }
        }

        when {
            items.loadState.append is LoadState.Loading   -> {
                item {
                    Column {
                        CircularProgressIndicator(color = LocalColors.current.primary)
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
            else -> {
                bottomSpacer?.let {
                    item {
                        it.invoke()
                    }
                }
            }
        }
    }
}

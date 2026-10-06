package gr.indice.agents.dex.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import gr.indice.agents.dex.ui.theme.default
import gr.indice.agents.network.models.ConversationListItem
import kotlinx.coroutines.launch

object DrawerSheet {

    data class Actions(
        val deleteChat: (String) -> Unit,
        val fetchChat: (String) -> Unit,
    )

    @Composable
    fun View(
        modifier: Modifier = Modifier,
        title: String,
        myChats: List<ConversationListItem>,
        actions: Actions
    ) {
        val scope = rememberCoroutineScope()
        ModalDrawerSheet(
            modifier = modifier
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(default),
                style = MaterialTheme.typography.titleLarge
            )
            HorizontalDivider()

            myChats.forEach { item ->
                val dismissState = rememberSwipeToDismissBoxState(initialValue = SwipeToDismissBoxValue.Settled)
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(color = MaterialTheme.colorScheme.errorContainer)
                            ,
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            IconButton(
                                onClick = {
                                    actions.deleteChat(item.id)
                                }
                            ) {
                                Icon(imageVector = Icons.Outlined.Delete, "Delete")
                            }
                        }

                    },
                    onDismiss = {
                        if (it == SwipeToDismissBoxValue.EndToStart) {
                            scope.launch {
                                dismissState.reset()
                                actions.deleteChat(item.id)
                            }
                        } else {
                            scope.launch { dismissState.reset() }
                        }

                    }
                ) {
                    NavigationDrawerItem(
                        label = { Text(item.title.orEmpty()) },
                        selected = false,
                        onClick = {
                            actions.fetchChat(item.id)
                        },
                        modifier = Modifier
                            .background(color = MaterialTheme.colorScheme.background)
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    }
}
package gr.indice.agents.dex.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import gr.indice.agents.dex.R
import gr.indice.agents.dex.ui.theme.default
import gr.indice.agents.dex.ui.theme.small
import gr.indice.agents.dex.utilities.pagerList
import gr.indice.agents.dex.utilities.toFormattedString
import gr.indice.agents.network.models.ConversationListItem
import kotlinx.coroutines.launch

object DrawerSheet {

    data class Actions(
        val deleteChat: (String) -> Unit,
        val fetchChat: (String) -> Unit,
        val newChat: () -> Unit
    )

    @Composable
    fun View(
        modifier: Modifier = Modifier,
        title: String,
        search: TextFieldState,
        myChats: LazyPagingItems<ConversationListItem>,
        actions: Actions
    ) {
        var isRefreshing by remember { mutableStateOf(false) }
        val refreshState = rememberPullToRefreshState()

        val onRefreshAction: () -> Unit = remember {
            lambda@ {
                isRefreshing = true
                myChats.refresh()
            }
        }

        LaunchedEffect(myChats.loadState) {
            if(myChats.loadState.refresh is LoadState.NotLoading || myChats.loadState.hasError){
                isRefreshing = false
            }
        }

        val scope = rememberCoroutineScope()
        ModalDrawerSheet(
            modifier = modifier
                .fillMaxHeight()
            ,
            drawerContainerColor = MaterialTheme.colorScheme.background
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(default),
                style = MaterialTheme.typography.titleLarge
            )

            HorizontalDivider()


            OutlinedTextField(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = default)
                    .padding(top = default)
                ,
                state = search,
                lineLimits = TextFieldLineLimits.SingleLine,
                shape = CircleShape,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        "Search"
                    )
                },
                placeholder = {
                    Text(stringResource(R.string.conversation_history_search))
                }
            )


            PullToRefreshBox(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(default)
                ,
                isRefreshing = isRefreshing,
                onRefresh = onRefreshAction,
                state = refreshState
            ) {
                Column(
                    modifier = Modifier
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        pagerList(
                            items = myChats,
                            checkForHeader = { currentItem, prevItem ->
                                prevItem == null || currentItem.createdAt.toFormattedString("dd MM yyyy") != prevItem.createdAt.toFormattedString("dd MM yyyy")
                            },
                            headerContent = {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = it.createdAt.toFormattedString("dd/MM/yyyy"),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            divider = {
                                HorizontalDivider()
                            },
                            itemContent = {  index, _ ->

                                val item = myChats[index]!!
                                val dismissState = rememberSwipeToDismissBoxState(initialValue = SwipeToDismissBoxValue.Settled)

                                var showAlert by remember { mutableStateOf(false) }

                                if (showAlert) {
                                    AlertDialog(
                                        modifier = Modifier,
                                        onDismissRequest = {},
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    showAlert = false
                                                    actions.deleteChat(item.id)
                                                }
                                            ) {
                                                Text(stringResource(R.string.alert_delete_btn_delete))
                                            }
                                        },
                                        dismissButton = {
                                            OutlinedButton(
                                                onClick = {
                                                    showAlert = false
                                                },
                                                border = null
                                            ) {
                                                Text(stringResource(R.string.alert_delete_btn_cancel))
                                            }
                                        },
                                        icon = {
                                           Icon(imageVector = Icons.Outlined.Delete, "Delete")
                                        },
                                        title = {
                                            Text(stringResource(R.string.alert_delete_title))
                                        },
                                        text = {
                                            Text(stringResource(R.string.alert_delete_text)
                                                .replace("{NAME}", item.title.orEmpty()))
                                        },
                                        shape= RoundedCornerShape(default),
                                        tonalElevation =  0.dp,
                                        iconContentColor = MaterialTheme.colorScheme.primary,
                                        properties = DialogProperties(
                                            dismissOnBackPress = false,
                                            dismissOnClickOutside = false,
                                            usePlatformDefaultWidth = true
                                        )
                                    )
                                }

                                SwipeToDismissBox(
                                    state = dismissState,
                                    modifier = Modifier.clip(RoundedCornerShape(default)),
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
                                                    showAlert = true
                                                    //actions.deleteChat(item.id)
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
                                                showAlert = true
                                                //actions.deleteChat(item.id)
                                            }
                                        } else {
                                            scope.launch { dismissState.reset() }
                                        }

                                    }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.background)
                                            .clickable {
                                                actions.fetchChat(item.id)
                                            }
                                            .padding(small)
                                        ,
                                        verticalArrangement = Arrangement.spacedBy(small)
                                    ) {
                                        Text(
                                            text = item.title.orEmpty(),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = item.lastActivityAt.toFormattedString("dd/MM/yyyy HH:mm"),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = FontStyle.Italic
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                FloatingActionButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd),
                    onClick = {
                        actions.newChat()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        "Add"
                    )
                }
            }
        }
    }
}
package gr.indice.agents.dex.ui.menu

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import gr.indice.agents.dex.R
import gr.indice.agents.dex.ui.theme.default
import gr.indice.agents.dex.ui.theme.large
import gr.indice.agents.dex.ui.theme.small
import gr.indice.agents.dex.utilities.LocalColors
import gr.indice.agents.dex.utilities.pagerList
import gr.indice.agents.dex.utilities.toFormattedString
import gr.indice.agents.network.models.ConversationListItem

object DrawerSheet {

    data class Actions(
        val deleteChat: (String) -> Unit,
        val fetchChat: (String) -> Unit,
        val newChat: () -> Unit,
        val close: () -> Unit
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

        val colors = LocalColors.current

        ModalDrawerSheet(
            modifier = modifier
                .fillMaxHeight()
            ,
            drawerContainerColor = colors.background
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(default)
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(default).weight(1f),
                    style = MaterialTheme.typography.titleLarge
                )

                IconButton(
                    onClick = actions.close
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu"
                    )
                }
            }

            HorizontalDivider()


            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = default)
                    .padding(top = default)
                ,
                state = search,
                lineLimits = TextFieldLineLimits.SingleLine,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.primary,
                    disabledBorderColor = colors.primary.copy(alpha = 0.5f),
                    focusedContainerColor = colors.background,
                    unfocusedContainerColor = colors.background,
                    disabledContainerColor = colors.background.copy(alpha = 0.5f),
                    focusedTextColor = colors.contentColor,
                    unfocusedTextColor = colors.contentColor,
                    disabledTextColor = colors.contentColor.copy(alpha = 0.5f),
                    focusedPlaceholderColor = colors.contentColor.copy(alpha = 0.4f),
                    unfocusedPlaceholderColor = colors.contentColor.copy(alpha = 0.4f),
                    disabledPlaceholderColor = colors.contentColor.copy(alpha = 0.4f)
                ),
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
                state = refreshState,
                indicator = {
                    Indicator(
                        modifier = Modifier.align(Alignment.TopCenter),
                        isRefreshing = isRefreshing,
                        state = refreshState,
                        color = colors.primary,
                        containerColor = colors.secondary
                    )
                }
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
                                val dismissState = rememberSwipeToDismissBoxState()
                                var swipeObserved by remember { mutableStateOf(false) }
                                var showAlert by remember { mutableStateOf(false) }

                                LaunchedEffect(dismissState.settledValue) {
                                    if (dismissState.settledValue == SwipeToDismissBoxValue.EndToStart) {
                                        if (swipeObserved) {
                                            showAlert = true
                                            dismissState.reset()
                                        } else {
                                            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                        }
                                    }
                                    swipeObserved = true
                                }

                                if (showAlert) {
                                    AlertDialog(
                                        modifier = Modifier,
                                        onDismissRequest = {},
                                        confirmButton = {
                                            Button(
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = colors.primary,
                                                    contentColor = colors.background,
                                                    disabledContainerColor = colors.primary.copy(alpha = 0.1f),
                                                    disabledContentColor = colors.contentColor.copy(alpha = 0.1f)
                                                ),
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
                                        shape = RoundedCornerShape(default),
                                        tonalElevation =  0.dp,
                                        iconContentColor = colors.primary,
                                        textContentColor = colors.contentColor,
                                        containerColor = colors.background,
                                        titleContentColor = colors.contentColor,
                                        properties = DialogProperties(
                                            dismissOnBackPress = false,
                                            dismissOnClickOutside = false,
                                            usePlatformDefaultWidth = true
                                        )
                                    )
                                }

                                SwipeToDismissBox(
                                    state = dismissState,
                                    modifier = Modifier
                                        .padding(bottom = small)
                                    ,
                                    enableDismissFromStartToEnd = false,
                                    backgroundContent = {
                                        val color by animateColorAsState(
                                            if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) colors.error
                                            else colors.error.copy(alpha = 0.7f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 1.dp)
                                                .background(
                                                    color = color,
                                                    shape = RoundedCornerShape(default)
                                                )
                                                .padding(horizontal = large)
                                            ,
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = "Delete",
                                                tint = colors.background
                                            )
                                        }
                                    }
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                        ,
                                        color = colors.background,
                                        onClick = { actions.fetchChat(item.id) },
                                        shape = RoundedCornerShape(default)

                                    ) {
                                        Column(
                                            modifier = Modifier.padding(small),
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
                            }
                        )
                    }
                }

                FloatingActionButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd),
                    containerColor = colors.primary,
                    contentColor = colors.background,
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
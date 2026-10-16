package gr.indice.agents.dex

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import gr.indice.agents.dex.ui.content.ChatScreen
import gr.indice.agents.dex.ui.menu.DrawerSheet
import gr.indice.agents.dex.ui.theme.default
import gr.indice.agents.dex.utilities.AgentUiConfig
import gr.indice.agents.dex.utilities.AgentUiDefault
import gr.indice.agents.dex.utilities.LocalColors
import gr.indice.agents.network.AgentClient
import kotlinx.coroutines.launch

object AgentUiScreen {

    @Composable
    fun View(
        config: AgentUiConfig = AgentUiDefault.default()
    ) {
        CompositionLocalProvider(LocalColors provides config.colors) {
            Content(config = config)
        }
    }

    @Composable
    internal fun Content(
        viewModel: AgentUiViewModel = viewModel(),
        config: AgentUiConfig
    ) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        val isReady by remember(uiState) {
            derivedStateOf {
                !uiState.initialling
            }
        }

        val drawerState = rememberDrawerState(DrawerValue.Closed) { isReady }
        val scope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = isReady,
            drawerContent = {

                val myChats = viewModel.conversationPagerFlow.collectAsLazyPagingItems()

                DrawerSheet.View(
                    title = stringResource(R.string.menu_title),
                    myChats = myChats,
                    search = viewModel.searchText,
                    actions = DrawerSheet.Actions(
                        deleteChat = viewModel::deleteChatHistory,
                        fetchChat = {
                            viewModel.loadFromHistory(it)
                            scope.launch { drawerState.close() }
                        },
                        newChat = {
                            viewModel.newChat()
                            scope.launch { drawerState.close() }
                        }
                    )
                )
            }
        ) {

            if (!isReady) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = config.colors.background)
                    ,
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier,
                        verticalArrangement = Arrangement.spacedBy(default),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = config.colors.primary
                        )
                        Text(
                            text = stringResource(R.string.loading),
                            color = config.colors.primary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                val chatList by viewModel.chatList.collectAsStateWithLifecycle()
                val questionLimit by viewModel.sessionQuestionsLimit.collectAsStateWithLifecycle()
                val agents by viewModel.agents.collectAsStateWithLifecycle()

                ChatScreen.View(
                    config = config,
                    chatList = chatList,
                    questionLimit = questionLimit,
                    uiState = uiState,
                    agents = agents,
                    actions = ChatScreen.Actions(
                        openMenu = {
                            scope.launch { drawerState.open() }
                        },
                        onSubmit = viewModel::ask,
                        likeResponse = viewModel::likeMessage,
                        stopStream = viewModel::stop,
                        onAgentSelection = viewModel::selectedAgent
                    )
                )
            }
        }
    }
}

@Preview
@Composable
private fun AgentUiScreenPreview() {
    remember { AgentClient.init("https://agents.indice.gr"); 0 }
    AgentUiScreen.View()
}
package gr.indice.agents.dex

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import gr.indice.agents.dex.ui.content.ChatScreen
import gr.indice.agents.dex.ui.menu.DrawerSheet
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
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {

                val myChats = viewModel.conversationPagerFlow.collectAsLazyPagingItems()

                DrawerSheet.View(
                    title = stringResource(R.string.menu_title),
                    myChats = myChats,
                    search = viewModel.searchText,
                    actions = DrawerSheet.Actions(
                        deleteChat =viewModel::deleteChatHistory,
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

            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val chatList by viewModel.chatList.collectAsStateWithLifecycle()
            val questionLimit by viewModel.sessionQuestionsLimit.collectAsStateWithLifecycle()
            val errorText by viewModel.errorMessage.collectAsStateWithLifecycle()
            val agents by viewModel.agents.collectAsStateWithLifecycle()

            ChatScreen.View(
                config = config,
                chatList = chatList,
                questionLimit = questionLimit,
                uiState = uiState,
                errorText = errorText,
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

@Preview
@Composable
private fun AgentUiScreenPreview() {
    remember { AgentClient.init("https://agents.indice.gr"); 0 }
    AgentUiScreen.View()
}
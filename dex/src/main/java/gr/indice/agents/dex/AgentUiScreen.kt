package gr.indice.agents.dex

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import gr.indice.agents.dex.ui.content.ChatScreen
import gr.indice.agents.dex.ui.menu.DrawerSheet
import gr.indice.agents.dex.ui.theme.DexTheme
import gr.indice.agents.network.AgentClient
import kotlinx.coroutines.launch

object AgentUiScreen {

    @Composable
    fun View(
        viewModel: AgentUiViewModel = viewModel()
    ) {
        DexTheme {

            val viewModel = viewModel<AgentUiViewModel>()

            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {

                    val myChats by viewModel.myChats.collectAsStateWithLifecycle()

                    DrawerSheet.View(
                        title = stringResource(R.string.menu_title),
                        myChats = myChats,
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

                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val chatList by viewModel.chatList.collectAsStateWithLifecycle()
                val questionLimit by viewModel.sessionQuestionsLimit.collectAsStateWithLifecycle()

                ChatScreen.View(
                    chatName = "Dex",
                    chatList = chatList,
                    questionLimit = questionLimit,
                    uiState = uiState,
                    actions = ChatScreen.Actions(
                        openMenu = {
                            scope.launch { drawerState.open() }
                        },
                        onSubmit = viewModel::ask,
                        likeResponse = viewModel::likeMessage
                    )
                )
            }
        }
    }
}

@Preview
@Composable
private fun AgentUiScreenPreview() {
    AgentClient.init("https://agents.indice.gr")
    AgentUiScreen.View()
}
package gr.indice.agents.dex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gr.indice.agents.dex.ui.content.ChatScreen
import gr.indice.agents.dex.ui.menu.DrawerSheet
import gr.indice.agents.dex.ui.theme.DexTheme
import gr.indice.agents.network.AgentClient
import kotlinx.coroutines.launch

class MainActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val viewModel by viewModels<MainActivityViewModel>()

        AgentClient.init("https://agents.indice.gr")

        setContent {
            DexTheme {
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
                                }
                            )
                        )
                    }
                ) {

                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val chatList by viewModel.chatList.collectAsStateWithLifecycle()
                    val questionLimit by viewModel.sessionQuestionsLimit.collectAsStateWithLifecycle()

                    ChatScreen.View(
                        chatName = stringResource(R.string.app_name),
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
}
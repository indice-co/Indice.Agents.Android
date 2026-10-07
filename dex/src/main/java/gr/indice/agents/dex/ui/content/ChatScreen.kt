package gr.indice.agents.dex.ui.content

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import gr.indice.agents.dex.R
import gr.indice.agents.dex.models.ChatItem
import gr.indice.agents.dex.models.UiState
import gr.indice.agents.dex.models.isUser
import gr.indice.agents.dex.models.shape
import gr.indice.agents.dex.ui.theme.default
import gr.indice.agents.dex.ui.theme.small
import gr.indice.agents.network.models.DexChatUsage
import kotlinx.coroutines.launch

object ChatScreen {

    data class Actions(
        val openMenu: () -> Unit,
        val onSubmit: (String) -> Unit,
        val likeResponse: (chatId: String, messageId: String, Boolean?) -> Unit
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun View(
        modifier: Modifier = Modifier,
        chatName: String,
        chatList: List<ChatItem>,
        questionLimit: DexChatUsage?,
        uiState: UiState,
        actions: Actions
    ) {
        val scope = rememberCoroutineScope()
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .imePadding(),
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(default),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                modifier = Modifier.size(40.dp),
                                painter = painterResource(R.drawable.dex_logo_default),
                                contentDescription = "DEX Logo",
                            )
                            Text(
                                text = chatName,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { actions.openMenu() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->

            val state = rememberLazyListState()

            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(default),
                verticalArrangement = Arrangement.spacedBy(small),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                LaunchedEffect(chatList, uiState.statusText) {
                    state.animateScrollToItem(state.layoutInfo.totalItemsCount + 1)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                    ,
                    verticalArrangement = Arrangement.spacedBy(default),
                    state = state
                ) {
                    items(chatList, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = if (item.isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Column {
                                Surface(
                                    shape = item.shape,
                                    color = if (item.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .padding(small)
                                            .animateContentSize(),
                                        text = AnnotatedString.fromHtml(item.value),
                                        color = if (item.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                //Show like btns when the response is completed
                                if (item is ChatItem.AgentItem && item.response.messages.lastOrNull()?.messageId != null) {

                                    var isLiked by remember { mutableStateOf(item.response.messages.lastOrNull()?.liked) }

                                    Row(
                                        modifier = Modifier,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val message = item.response.messages.lastOrNull()
                                                val chatId = item.response.conversationId
                                                val liked = if (isLiked == true) null else true

                                                if (chatId != null && message != null && message.messageId != null) {
                                                    actions.likeResponse(chatId, message.messageId, liked)
                                                    isLiked = liked
                                                }
                                            }
                                        ) {
                                            val vector = when(isLiked) {
                                                true -> Icons.Default.ThumbUp
                                                else -> Icons.Outlined.ThumbUp
                                            }
                                            Icon(imageVector = vector, "Like")
                                        }

                                        IconButton(
                                            onClick = {
                                                val message = item.response.messages.lastOrNull()
                                                val chatId = item.response.conversationId
                                                val liked = if (isLiked == false) null else false

                                                if (chatId != null && message != null && message.messageId != null) {
                                                    actions.likeResponse(chatId, message.messageId, liked)
                                                    isLiked = liked
                                                }
                                            }
                                        ) {
                                            val vector = when(isLiked) {
                                                false -> Icons.Default.ThumbDown
                                                else -> Icons.Outlined.ThumbDown
                                            }
                                            Icon(imageVector = vector, "Dislike")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    statusView(uiState.statusText)
                }

                InputView(
                    isConnected = uiState.isConnected,
                    placeholderText = stringResource(R.string.input_placeholder),
                    questionLimit = questionLimit,
                    onSubmit = actions.onSubmit
                )

            }
        }
    }

     private fun LazyListScope.statusView(value: String) {
         value.takeIf { it.isNotBlank() }?.let {
             item {
                Text(
                    modifier = Modifier
                        .clip(RoundedCornerShape(default))
                        .background(Color.Yellow, RoundedCornerShape(default))
                        .padding(small)
                    ,
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
             }
         }

    }

    @Composable
    private fun InputView(
        modifier: Modifier = Modifier,
        isConnected: Boolean,
        placeholderText: String,
        questionLimit: DexChatUsage?,
        maxLength: Int = 2000,
        totalLineHeight: Int = 10,
        onSubmit: (String) -> Unit
    ) {
        val input = rememberTextFieldState()

        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = default),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(default)
        ) {
            val interaction = remember { MutableInteractionSource() }

            BasicTextField(
                modifier = Modifier.weight(1f),
                state = input,
                interactionSource = interaction,
                inputTransformation = InputTransformation.maxLength(maxLength),
                lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = totalLineHeight),
                enabled = !isConnected,
                decorator = { decorator ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                2.dp,
                                OutlinedTextFieldDefaults.colors().focusedIndicatorColor,
                                OutlinedTextFieldDefaults.shape
                            )
                            .padding(small)
                            .heightIn(min = OutlinedTextFieldDefaults.MinHeight)
                            .animateContentSize()
                        ,
                        horizontalArrangement = Arrangement.spacedBy(small)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                decorator()

                                if (input.text.isEmpty()) {
                                    Text(
                                        text = placeholderText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            val text = questionLimit?.let {
                                "${it.questionsUsedCount} / ${it.questionsLimitCount}"
                            }

                            Column {
                                Spacer(Modifier.height(default))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = text.orEmpty(),
                                        style = MaterialTheme.typography.labelSmall
                                    )

                                    Text(
                                        text = "${input.text.count()} / $maxLength",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                            }

                        }

                        val color by animateColorAsState(
                            if (isConnected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.primary
                        )

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(40.dp)
                                .background(color = color, CircleShape)
                                .clickable(enabled = !isConnected) {
                                    onSubmit(input.text.toString())
                                    input.clearText()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "",
                                tint = Color.White
                            )
                        }
                    }

                }
            )
        }
    }
}
package gr.indice.agents.dex.ui.content

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
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.outlined.Error
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import gr.indice.agents.dex.R
import gr.indice.agents.dex.models.ChatContent
import gr.indice.agents.dex.models.ChatContentType
import gr.indice.agents.dex.models.ChatItem
import gr.indice.agents.dex.models.UiState
import gr.indice.agents.dex.models.isUser
import gr.indice.agents.dex.models.shape
import gr.indice.agents.dex.ui.elements.CalloutView
import gr.indice.agents.dex.ui.elements.ChatHtmlView
import gr.indice.agents.dex.ui.elements.ChatImageView
import gr.indice.agents.dex.ui.elements.CodeSnippet
import gr.indice.agents.dex.ui.elements.ConfirmationView
import gr.indice.agents.dex.ui.elements.FooterMessageView
import gr.indice.agents.dex.ui.elements.MultipleChoicesView
import gr.indice.agents.dex.ui.elements.UnavailableTypeView
import gr.indice.agents.dex.ui.theme.default
import gr.indice.agents.dex.ui.theme.small
import gr.indice.agents.network.models.DexChatUsage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

object ChatScreen {

    data class Actions(
        val openMenu: () -> Unit,
        val onSubmit: (String) -> Unit,
        val likeResponse: (chatId: String, messageId: String, Boolean?) -> Unit,
        val stopStream: () -> Unit
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun View(
        modifier: Modifier = Modifier,
        chatName: String,
        chatList: List<ChatItem>,
        questionLimit: DexChatUsage?,
        uiState: UiState,
        errorText: String,
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

                LaunchedEffect(chatList.count(), uiState.statusText, errorText) {
                    val lastVisible = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    var lastIndex = chatList.lastIndex
                    uiState.statusText.takeIf { it.isNotEmpty() }?.let {
                        lastIndex++
                    }
                    errorText.takeIf { it.isNotEmpty() }?.let {
                        lastIndex++
                    }

                    if (lastVisible == null || lastVisible >= lastIndex - 1) {
                        state.animateScrollToItem(lastIndex.coerceAtLeast(0))
                    }
                }

                val lastAgentMessageId by remember(chatList) {
                    derivedStateOf {
                        chatList
                            .filterIsInstance<ChatItem.AgentItem>()
                            .flatMap { it.response.messages }
                            .lastOrNull()?.messageId
                    }
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
                                    when(item) {
                                        is ChatItem.UserItem -> {
                                            Text(
                                                modifier = Modifier
                                                    .padding(small)
                                                    .animateContentSize(),
                                                text = AnnotatedString.fromHtml(item.value),
                                                color = Color.White
                                            )
                                        }
                                        is ChatItem.AgentItem -> {
                                            val currentMessageId = item.response.messages.lastOrNull()?.messageId
                                            AgentResponseContent(
                                                modifier = Modifier
                                                    .padding(small)
                                                    .animateContentSize(),
                                                content = item.chatContent,
                                                isActive = lastAgentMessageId == currentMessageId,
                                                onReply = actions.onSubmit
                                            )
                                        }
                                    }
                                }

                                if (item is ChatItem.AgentItem) {
                                    FooterMessageView.View(
                                        citations = item.response.messages.lastOrNull()?.citations.orEmpty(),
                                        message = item.response.messages.lastOrNull(),
                                        onLike = { liked ->
                                            item.response.messages.lastOrNull()?.messageId?.let { messageId ->
                                                val chatId = item.response.conversationId ?: return@let

                                                actions.likeResponse(chatId, messageId, liked)
                                            }
                                        }
                                    )
                                }

                            }
                        }
                    }

                    statusView(uiState.isLoading, uiState.statusText)
                    errorView(errorText)

                }

                InputView(
                    isConnected = uiState.isConnected,
                    placeholderText = stringResource(R.string.input_placeholder),
                    questionLimit = questionLimit,
                    onSubmit = actions.onSubmit,
                    stopStream = actions.stopStream
                )

            }
        }
    }

    @Composable
    private fun AgentResponseContent(
        modifier: Modifier = Modifier,
        content: List<ChatContent>,
        isActive: Boolean,
        onReply: (String) -> Unit
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(default)
        ) {
            content.forEach { item ->
                when(item.content) {
                    is ChatContentType.Text -> {
                        Text(text = item.content.value)
                    }
                    is ChatContentType.Markdown -> {
                        item.content.value.forEach { data ->
                            when(data) {
                                is ChatContentType.Markdown.Block.Code -> {
                                    CodeSnippet.View(code = data.code)
                                }
                                is ChatContentType.Markdown.Block.Text -> {
                                    Text(text = AnnotatedString.fromHtml(data.text))
                                }
                            }

                        }

                    }
                    is ChatContentType.Html -> {
                        ChatHtmlView(html = item.content.value)
                    }
                    is ChatContentType.ImageData,  is ChatContentType.ImageUrl -> {
                        ChatImageView(content = item.content, caption = item.caption)
                    }
                    is ChatContentType.MultipleChoice -> {
                        MultipleChoicesView.View(isActive = isActive, options = item.content.data) { onReply(it) }
                    }
                    is ChatContentType.Callout -> {
                        CalloutView.View(callout = item.content.value)
                    }
                    is ChatContentType.Confirmation -> {
                        ConfirmationView.View(data = item.content.data, isActive = isActive) { onReply(it) }
                    }
                    is ChatContentType.Unsupported -> {
                        UnavailableTypeView.View(mediaType = item.content.mediaType)
                    }
                    is ChatContentType.Unavailable -> {
                        UnavailableTypeView.View(mediaType = item.content.mediaType)
                    }
                }
            }
        }
    }

    private fun LazyListScope.statusView(isLoading: Boolean, status: String) {
        if (isLoading || status.isNotEmpty()) {
            item {

                var dotCount by remember { mutableIntStateOf(1) }

                LaunchedEffect(isLoading, status) {
                    if (isLoading && status.isEmpty()) {
                        while (true) {
                            delay(400.milliseconds)
                            dotCount = if (dotCount == 3) 1 else dotCount + 1
                        }
                    }
                }

                val text = if (!isLoading || status.isNotEmpty()) status else stringResource(R.string.loading_agent) + ".".repeat(dotCount)
                Text(
                    modifier = Modifier
                        .clip(RoundedCornerShape(default))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), RoundedCornerShape(default))
                        .padding(small)
                        .animateContentSize()
                    ,
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

        }
//         value.takeIf { it.isNotBlank() }?.let {
//             item {
//                Text(
//                    modifier = Modifier
//                        .clip(RoundedCornerShape(default))
//                        .background(Color.Yellow, RoundedCornerShape(default))
//                        .padding(small)
//                    ,
//                    text = value,
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onBackground
//                )
//             }
//         }

    }

    private fun LazyListScope.errorView(value: String) {
        value.takeIf { it.isNotEmpty() }?.let {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Color.Red, shape = RoundedCornerShape(default))
                        .padding(default),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(default)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Error,
                        contentDescription = "Error",
                        tint = Color.White
                    )
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
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
        onSubmit: (String) -> Unit,
        stopStream: () -> Unit
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
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                cursorBrush = SolidColor( MaterialTheme.colorScheme.onBackground),
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
                                        text = "${input.text.trim().count()} / $maxLength",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                            }

                        }

//                        val color by animateColorAsState(
//                            if (isConnected || input.text.isBlank())
//                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
//                            else
//                                MaterialTheme.colorScheme.primary
//                        )

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(40.dp)
                                .background(color = MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable(enabled = (!isConnected && input.text.isNotEmpty()) || isConnected) {
                                    if (isConnected) {
                                        stopStream()
                                    } else {
                                        onSubmit(input.text.trim().toString())
                                        input.clearText()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val icon = if (isConnected) Icons.Default.Square else Icons.Default.ArrowUpward
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = icon,
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
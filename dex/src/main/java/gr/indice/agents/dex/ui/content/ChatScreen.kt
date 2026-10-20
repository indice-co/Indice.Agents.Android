package gr.indice.agents.dex.ui.content

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.DragInteraction
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
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLinkStyles
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
import gr.indice.agents.dex.utilities.AgentUiConfig
import gr.indice.agents.dex.utilities.LocalColors
import gr.indice.agents.dex.utilities.UiImage
import gr.indice.agents.dex.utilities.UiImageView
import gr.indice.agents.dex.utilities.rememberRevealedText
import gr.indice.agents.network.models.AgentInfo
import gr.indice.agents.network.models.ChatPhase
import gr.indice.agents.network.models.DexChatUsage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import kotlin.time.Duration.Companion.milliseconds

object ChatScreen {

    private val markdownParser = Parser.builder().build()
    private val htmlRenderer = HtmlRenderer.builder().build()

    data class Actions(
        val openMenu: () -> Unit,
        val onSubmit: (String) -> Unit,
        val likeResponse: (chatId: String, messageId: String, Boolean?) -> Unit,
        val stopStream: () -> Unit,
        val onAgentSelection: (AgentInfo) -> Unit
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun View(
        modifier: Modifier = Modifier,
        config: AgentUiConfig,
        state: UiState,
        actions: Actions
    ) {
        val scope = rememberCoroutineScope()
        val colors = LocalColors.current
        val chatList = state.chat
        val phase = state.phase

        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .imePadding(),
            containerColor = colors.background,
            contentColor = colors.contentColor,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(default),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UiImageView(
                                modifier = Modifier.size(40.dp),
                                uiImage = config.icon
                            )
                            Text(
                                text = config.name,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { actions.openMenu() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.background,
                        navigationIconContentColor = colors.contentColor,
                        titleContentColor = colors.contentColor,
                        subtitleContentColor = colors.contentColor,
                        actionIconContentColor = colors.contentColor
                    )
                )
            }
        ) { innerPadding ->

            val listState = rememberLazyListState()

            val statusText = (phase as? ChatPhase.Streaming)?.status.orEmpty()
            val showStatus = phase is ChatPhase.Loading ||
                    (phase is ChatPhase.Streaming && (statusText.isNotEmpty() || chatList.lastOrNull() is ChatItem.UserItem))
            val errorText = (phase as? ChatPhase.Error)?.message.orEmpty()

            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(default),
                verticalArrangement = Arrangement.spacedBy(small),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                val lastItem = chatList.lastOrNull()
                val streamingId = (lastItem as? ChatItem.AgentItem)?.id?.takeIf { phase is ChatPhase.Streaming }
                val isStreaming by rememberUpdatedState(streamingId != null)

                LaunchedEffect(lastItem?.id) {
                    when (lastItem) {
                        is ChatItem.UserItem -> followList(listState, pinIndex = null)
                        is ChatItem.AgentItem -> if (isStreaming) {
                            followList(listState, pinIndex = chatList.lastIndex)
                        } else {
                            listState.scrollToItem(chatList.lastIndex)
                        }
                        null -> Unit
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
                    state = listState
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
                                    color = if (item.isUser) colors.primary else colors.secondary
                                ) {
                                    when(item) {
                                        is ChatItem.UserItem -> {
                                            Text(
                                                modifier = Modifier
                                                    .padding(small)
                                                    .animateContentSize(),
                                                text = AnnotatedString.fromHtml(item.value),
                                                color = colors.background
                                            )
                                        }
                                        is ChatItem.AgentItem -> {
                                            val currentMessageId = item.response.messages.lastOrNull()?.messageId
                                            val animate = rememberSaveable(item.id) { item.id == streamingId }
                                            AgentResponseContent(
                                                modifier = Modifier
                                                    .padding(small)
                                                    .animateContentSize(),
                                                content = item.chatContent,
                                                animate = animate,
                                                logo = config.icon,
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

                    statusView(showStatus, statusText)
                    errorView(errorText)

                }

                InputView(
                    isConnected = state.isBusy,
                    placeholderText = stringResource(R.string.input_placeholder),
                    questionLimit = state.usage,
                    agents = state.agents,
                    selectedAgent = state.selectedAgent,
                    onSubmit = actions.onSubmit,
                    stopStream = actions.stopStream,
                    onAgentSelection = actions.onAgentSelection
                )

                CompositionLocalProvider(LocalContentColor provides LocalContentColor.current.copy(alpha = 0.5f)) {
                    Text(
                        text = stringResource(R.string.agent_info_message)
                            .replace("{NAME}", config.name),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

            }
        }
    }

    @Composable
    private fun AgentResponseContent(
        modifier: Modifier = Modifier,
        content: List<ChatContent>,
        animate: Boolean,
        isActive: Boolean,
        logo: UiImage,
        onReply: (String) -> Unit
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(default)
        ) {
            content.forEach { item ->
                when(item.content) {
                    is ChatContentType.Text -> {
                        Text(text = rememberRevealedText(item.content.value, animate))
                    }
                    is ChatContentType.Markdown -> {
                        item.content.value.forEach { data ->
                            when(data) {
                                is ChatContentType.Markdown.Block.Code -> {
                                    CodeSnippet.View(code = rememberRevealedText(data.code, animate))
                                }
                                is ChatContentType.Markdown.Block.Text -> {
                                    MarkdownText(text = data.text, animate = animate)
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
                        UnavailableTypeView.View(logo = logo, mediaType = item.content.mediaType)
                    }
                    is ChatContentType.Unavailable -> {
                        UnavailableTypeView.View(logo = logo, mediaType = item.content.mediaType)
                    }
                }
            }
        }
    }


    private suspend fun followList(listState: LazyListState, pinIndex: Int?) = coroutineScope {
        var following = true
        launch {
            listState.interactionSource.interactions.collect {
                if (it is DragInteraction.Start) following = false
            }
        }

        snapshotFlow {
            val info = listState.layoutInfo
            val visible = info.visibleItemsInfo
            val last = visible.lastOrNull() ?: return@snapshotFlow 0

            val toEnd = if (last.index == info.totalItemsCount - 1) {
                last.offset + last.size - info.viewportSize.height
            } else {
                FAR
            }
            val toPin = when {
                pinIndex == null -> FAR
                else -> visible.firstOrNull { it.index == pinIndex }?.offset
                    ?: if (pinIndex > last.index) FAR else 0
            }
            minOf(toEnd, toPin)
        }.collect { delta ->
            when {
                !following || delta <= 0 -> Unit
                delta == FAR -> listState.animateScrollToItem(pinIndex ?: (listState.layoutInfo.totalItemsCount - 1))
                delta > SMOOTH_SCROLL_THRESHOLD -> listState.animateScrollBy(delta.toFloat())
                else -> listState.scrollBy(delta.toFloat())
            }
        }
    }

    private const val FAR = Int.MAX_VALUE
    private const val SMOOTH_SCROLL_THRESHOLD = 120

    @Composable
    private fun MarkdownText(text: String, animate: Boolean) {
        val revealed = rememberRevealedText(text, animate)
        val linkColor = LocalColors.current.primary
        val textStyle = LocalTextStyle.current

        val annotated = remember(revealed, linkColor, textStyle) {
            AnnotatedString.fromHtml(
                htmlString = htmlRenderer.render(markdownParser.parse(revealed)),
                linkStyles = TextLinkStyles(
                    style = textStyle.toSpanStyle().copy(color = linkColor)
                )
            )
        }
        Text(text = annotated)
    }

    private fun LazyListScope.statusView(isLoading: Boolean, status: String) {
        if (isLoading || status.isNotEmpty()) {
            item {
                val colors = LocalColors.current
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
                        .background(colors.secondary.copy(alpha = 0.8f), RoundedCornerShape(default))
                        .padding(small)
                        .animateContentSize()
                    ,
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primary.copy(alpha = 0.8f)
                )
            }

        }

    }

    private fun LazyListScope.errorView(value: String) {
        value.takeIf { it.isNotEmpty() }?.let {
            item {
                val colors = LocalColors.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = colors.error, shape = RoundedCornerShape(default))
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
        agents: List<AgentInfo>,
        selectedAgent: AgentInfo?,
        onSubmit: (String) -> Unit,
        stopStream: () -> Unit,
        onAgentSelection: (AgentInfo) -> Unit
    ) {
        val input = rememberTextFieldState()
        val colors = LocalColors.current

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
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.contentColor),
                cursorBrush = SolidColor( colors.contentColor),
                interactionSource = interaction,
                inputTransformation = InputTransformation.maxLength(maxLength),
                lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = totalLineHeight),
                enabled = !isConnected,
                decorator = { decorator ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, colors.primary, OutlinedTextFieldDefaults.shape)
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
                                        color = colors.contentColor.copy(alpha = 0.8f)
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

                                    DropdownSelection.View(
                                        options = agents,
                                        selected = selectedAgent,
                                        onSelection = onAgentSelection
                                    )

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


                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(40.dp)
                                .background(color = colors.primary, CircleShape)
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
                                tint = colors.background
                            )
                        }
                    }

                }
            )
        }
    }
}
package gr.indice.agents.dex.ui.elements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import gr.indice.agents.dex.models.domain
import gr.indice.agents.dex.models.faviconUrl
import gr.indice.agents.dex.ui.theme.DexTheme
import gr.indice.agents.dex.ui.theme.default
import gr.indice.agents.dex.ui.theme.small
import gr.indice.agents.dex.utilities.ChatImageLoader
import gr.indice.agents.network.models.Citation
import gr.indice.agents.network.models.DexChatMessage

internal object FooterMessageView {

    @Composable
    private fun FaviconIcon(
        modifier: Modifier = Modifier,
        citation: Citation,
        size: Dp = 24.dp
    ) {
        val context = LocalContext.current
        val imageLoader = remember { ChatImageLoader.get(context) }

        val urls = remember(citation) { citation.faviconUrl().orEmpty() }
        var index by remember(urls) { mutableIntStateOf(0) }
        val url = urls.getOrNull(index)

        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
            ,
            contentAlignment = Alignment.Center
        ) {
            if (url != null) {
                val request = remember(url) {
                    ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = citation.domain,
                    imageLoader = imageLoader,
                    onError = { index++ },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = citation.domain,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }


    @Composable
    fun View(
        modifier: Modifier = Modifier,
        citations: List<Citation>,
        message: DexChatMessage?,
        onLike: (Boolean?) -> Unit
    ) {

        var isExpanded by remember { mutableStateOf(false) }
        Column(
            modifier = modifier
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (citations.isNotEmpty()) {
                    CitationHeader(
                        citations = citations,
                        isExpanded = isExpanded,
                        onExpand = { isExpanded = !isExpanded }
                    )
                } else {
                    Spacer(Modifier)
                }

                if (message?.messageId != null) {

                    val isLiked = remember { message.liked }

                    Row(
                        modifier = Modifier.width(IntrinsicSize.Max),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                onLike(if (isLiked == true) null else true)
                            }
                        ) {
                            val vector = when (isLiked) {
                                true -> Icons.Default.ThumbUp
                                else -> Icons.Outlined.ThumbUp
                            }
                            Icon(imageVector = vector, "Like")
                        }

                        IconButton(
                            onClick = {
                                onLike(if (isLiked == false) null else false)
                            }
                        ) {
                            val vector = when (isLiked) {
                                false -> Icons.Default.ThumbDown
                                else -> Icons.Outlined.ThumbDown
                            }
                            Icon(imageVector = vector, "Dislike")
                        }
                    }
                }
            }

            AnimatedVisibility(isExpanded) {
                CitationsView(citations = citations)
            }

        }

    }

    @Composable
    private fun CitationHeader(
        modifier: Modifier = Modifier,
        citations: List<Citation>,
        maxIconsPreview: Int = 3,
        isExpanded: Boolean = false,
        onExpand: () -> Unit
    ) {
        val uniqueByDomain = remember(citations) { citations.distinctBy { it.domain } }

        val visibleCitations = uniqueByDomain.take(maxIconsPreview)
        val extra = uniqueByDomain.size - visibleCitations.size

        Column(
            modifier = modifier
        ) {
            Surface(
                shape = CircleShape,
                onClick = onExpand ,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = default, vertical = small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(small)
                ) {
                    Row(
                        modifier = Modifier,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((-small))
                    ) {
                        visibleCitations.forEach { FaviconIcon(citation = it) }
                        Row(modifier = Modifier.padding(start = default)) {
                            if (extra > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .border(
                                            2.dp,
                                            MaterialTheme.colorScheme.surface,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+$extra",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Text(
                                text = "${citations.count()} sources",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse sources" else "Expand sources",
                        modifier = Modifier.size(18.dp)
                    )

                }
            }
        }
    }

    @Composable
    private fun CitationsView(
        modifier: Modifier = Modifier,
        citations: List<Citation>
    ) {
        if (citations.isEmpty()) return
        val uriHandler = LocalUriHandler.current

        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(small)
        ) {
            citations.forEach { citation ->
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(citation.sourceUrl != null) {
                            citation.sourceUrl?.let {
                                uriHandler.openUri(it)
                            }
                        }
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .width(IntrinsicSize.Max)
                        .padding(small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(small)
                ) {
                    Text(
                        text = "${citation.number}.",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                    )
                    FaviconIcon(citation = citation)
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "${citation.title}.",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "OpenInNew",
                        tint = LocalContentColor.current.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CitationView() {
    DexTheme { 
        Surface(Modifier.fillMaxSize()) {
            FooterMessageView.View(
                message = null,
                onLike = {},
                citations = listOf(
                Citation(
                    chunkId = "934ed746-b61e-466f-a93d-fba932a0a16a",
                    documentId = "00000000-0000-0000-0000-000000000000",
                    number = 1,
                    score = 0.0,
                    sourceUrl = "https://agents.indice.gr/api/sources/Product.md",
                    title = "⚖️ 5.7 · Debuggability & observability",
                ),
                Citation(
                    chunkId = "bdebde45-848b-47a5-8a65-a6535c36f289",
                    documentId = "00000000-0000-0000-0000-000000000000",
                    number = 2,
                    score = 0.0,
                    sourceUrl = "https://agents.indice.gr/api/sources/Product.md",
                    title = "⚖️ 5.7 · Debuggability & observability vDebuggability & observability",
                ),
                Citation(
                    chunkId = "0036b651-bc57-41e0-a60b-6489836a44c2",
                    documentId = "00000000-0000-0000-0000-000000000000",
                    number = 3,
                    score = 0.0,
                    sourceUrl = "https://agents.indice.gr/api/sources/Product.md",
                    title = "🚦 2.6 · Identity validation pipeline",
                ),
                Citation(
                    chunkId = "5-4022-8403-19d0205f9f1f",
                    documentId = "00000000-0000-0000-0000-000000000000",
                    number = 4,
                    score = 0.0,
                    sourceUrl = "https://agents.indice.gr/api/sources/Product.md",
                    title = "📑 Overview",
                ),
                Citation(
                    chunkId = "7db8a420-e403-4231-8cf3-36ad18786860",
                    documentId = "00000000-0000-0000-0000-000000000000",
                    number = 5,
                    score = 0.0,
                    sourceUrl = "https://agents.indice.gr/api/sources/Product.md",
                    title = "🆔 1 · What is Indice IAM",
                ),
            )
            )
        }
    }
}
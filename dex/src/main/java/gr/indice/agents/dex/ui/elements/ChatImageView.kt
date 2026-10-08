package gr.indice.agents.dex.ui.elements

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import gr.indice.agents.dex.R
import gr.indice.agents.dex.models.ChatContentType
import gr.indice.agents.dex.utilities.ChatImageLoader

object ChatImageView {

    @Composable
    operator fun invoke(
        content: ChatContentType,
        caption: String?,
        modifier: Modifier = Modifier
    ) {
        val model: Any? = when (content) {
            is ChatContentType.ImageData -> content.data
            is ChatContentType.ImageUrl  -> content.url.toString()
            else -> null
        }
        if (model == null) {
            Image(painter = painterResource(R.drawable.dex_logo_default), "Image unavailable")
            return
        }

        val context = LocalContext.current
        val imageLoader = remember { ChatImageLoader.get(context) }
        val request = remember(content) {
            ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .build()
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AsyncImage(
                model = request,
                imageLoader = imageLoader,
                contentDescription = caption,
                contentScale = ContentScale.Fit,
                error = painterResource(R.drawable.dex_logo_default),
            )

            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

    }

}

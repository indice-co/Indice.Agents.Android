package gr.indice.agents.dex.utilities

import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage

@Composable
fun UiImageView(
    modifier: Modifier = Modifier,
    uiImage: UiImage
) {
   when(uiImage) {
       is UiImage.Resource -> {
           Image(
               modifier = modifier,
               painter = uiImage.painter,
               contentDescription = "Logo"
           )
       }
       is UiImage.Url -> {
           val context = LocalContext.current

           val imageLoader = remember { ChatImageLoader.get(context) }
           AsyncImage(
               model = uiImage.url,
               contentDescription = "Logo",
               imageLoader = imageLoader,
               modifier = modifier
           )
       }
       is UiImage.Vector -> {
           Icon(
               imageVector = uiImage.imageVector,
               contentDescription = "Logo",
               modifier = modifier,
               tint = LocalContentColor.current
           )
       }
   }
}
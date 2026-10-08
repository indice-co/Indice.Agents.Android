package gr.indice.agents.dex.ui.elements

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import gr.indice.agents.dex.R
import gr.indice.agents.dex.ui.theme.default
import gr.indice.agents.dex.ui.theme.small

object UnavailableTypeView {

    @Composable
    fun View(
        modifier: Modifier = Modifier,
        mediaType: String?
    ) {
        Row(
            modifier = modifier
                .background(color = Color.Red, shape = CircleShape)
                .padding(default),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(small)
        ) {
            Image(
                painter = painterResource(R.drawable.dex_logo_default),
                contentDescription = "DEX Logo",
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = stringResource(R.string.unavailable_media_type) + " ${mediaType ?: "-"}",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
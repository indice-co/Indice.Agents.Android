package gr.indice.agents.dex.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import gr.indice.agents.dex.models.Callout
import gr.indice.agents.dex.models.Severity
import gr.indice.agents.dex.models.foregroundColor
import gr.indice.agents.dex.models.image
import gr.indice.agents.dex.ui.theme.default
import gr.indice.agents.dex.ui.theme.small

object CalloutView {

    @Composable
    fun View(
        modifier: Modifier = Modifier,
        callout: Callout
    ) {
        val background = if (callout.severity == Severity.Info)
            Color.Gray.copy(alpha = 0.1f)
        else
            callout.severity.foregroundColor.copy(alpha = 0.2f)

        Row(
            modifier = modifier
                .background(color = background, shape = RoundedCornerShape(small))
                .padding(default)
            ,
            horizontalArrangement = Arrangement.spacedBy(default),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                modifier = Modifier
                    .size(24.dp),
                imageVector = callout.severity.image,
                contentDescription = callout.severity.name,
                tint = callout.severity.foregroundColor
            )

            Column(
                modifier = Modifier,
            ) {
                callout.title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(
                    text = callout.text,
                    style = MaterialTheme.typography.bodyMedium
                )

            }
        }
    }
}
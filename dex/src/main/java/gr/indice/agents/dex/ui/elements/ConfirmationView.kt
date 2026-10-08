package gr.indice.agents.dex.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import gr.indice.agents.dex.models.ConfirmData
import gr.indice.agents.dex.ui.theme.default

object ConfirmationView {

    @Composable
    fun View(
        modifier: Modifier = Modifier,
        data: ConfirmData,
        onSubmit: (String) -> Unit
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(default),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            data.prompt?.let {
                Text(text = it, style = MaterialTheme.typography.headlineSmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(default),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onSubmit(data.cancelText)
                    }
                ) {
                    Text(text = data.cancelText)
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onSubmit(data.confirmText)
                    }
                ) {
                    Text(text = data.confirmText)
                }
            }
        }
    }
}
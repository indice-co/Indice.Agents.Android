package gr.indice.agents.dex.ui.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gr.indice.agents.dex.models.ConfirmData
import gr.indice.agents.dex.ui.theme.default
import gr.indice.agents.dex.utilities.LocalColors

object ConfirmationView {

    @Composable
    fun View(
        modifier: Modifier = Modifier,
        data: ConfirmData,
        isActive: Boolean,
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
                val colors = LocalColors.current
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, if (isActive) colors.primary else colors.primary.copy(0.1f)),
                    enabled = isActive,
                    onClick = {
                        onSubmit(data.cancelText)
                    }
                ) {
                    Text(text = data.cancelText)
                }

                Button(
                    modifier = Modifier.weight(1f),
                    enabled = isActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.background,
                        disabledContainerColor = colors.primary.copy(alpha = 0.1f),
                        disabledContentColor = colors.contentColor.copy(alpha = 0.1f)
                    ),
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
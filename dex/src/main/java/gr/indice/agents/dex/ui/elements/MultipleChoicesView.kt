package gr.indice.agents.dex.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import gr.indice.agents.dex.R
import gr.indice.agents.dex.ui.theme.default
import gr.indice.agents.dex.ui.theme.small

object MultipleChoicesView {

    @Composable
    fun View(
        modifier: Modifier = Modifier,
        options: List<String>,
        onSubmit: (String) -> Unit
    ) {
        var selected by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(default)
        ) {
            options.forEach { option ->
                OutlinedButton(
                    onClick = {
                        selected = if (selected == option)
                            null
                        else
                            option
                    },
                ) {
                    Row(
                        modifier = Modifier,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(small)
                    ) {
                        val icon = if (selected == option) {
                            Icons.Outlined.CheckCircle
                        } else
                            Icons.Outlined.Circle

                        Icon(
                            imageVector = icon,
                            contentDescription = if (selected == option) "Selected" else "Unselected"
                        )

                        Text(text = option)
                    }
                }
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth(),
                enabled = selected != null,
                onClick = { selected?.let { onSubmit(it) } }
            ) {
                Text(stringResource(R.string.button_continue))
            }
        }

    }
}
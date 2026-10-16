package gr.indice.agents.dex.ui.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gr.indice.agents.dex.ui.theme.default
import gr.indice.agents.dex.ui.theme.small
import gr.indice.agents.dex.utilities.LocalColors
import gr.indice.agents.dex.utilities.iconVector
import gr.indice.agents.network.models.AgentInfo

object DropdownSelection {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun View(
        modifier: Modifier = Modifier,
        options: List<AgentInfo>,
        onSelection: (AgentInfo) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }
        var selectedOption by remember(options) { mutableStateOf(options.firstOrNull()) }

        val colors = LocalColors.current

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = modifier.width(IntrinsicSize.Max)
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.secondary, CircleShape)
                    .padding(small)
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                ,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(small)
            ) {
                selectedOption?.iconVector?.let {
                    Icon(
                        modifier = Modifier.size(default),
                        imageVector = it,
                        contentDescription = "Agent Icon"
                    )
                }
                Text(
                    modifier = Modifier.weight(1f),
                    text = selectedOption?.name.orEmpty().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )

                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)

            }

            ExposedDropdownMenu(
                modifier = Modifier
                    .width(IntrinsicSize.Max),
                expanded = expanded,
                shadowElevation = 2.dp,
                containerColor = colors.background,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(default))
                            .clickable {
                                selectedOption = option
                                expanded = false
                                onSelection(option)
                            }
                            .background(color = if (selectedOption == option) colors.secondary else Color.Unspecified, RoundedCornerShape(default))
                            .padding(default)
                        ,
                        horizontalArrangement = Arrangement.spacedBy(small)
                    ) {
                        option.iconVector?.let {
                            Icon(
                                modifier = Modifier.size(default),
                                imageVector = it,
                                contentDescription = "Agent Icon"
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(small)
                        ) {
                            Text(
                                text = option.name.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text=  option.description,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        if (selectedOption == option) {
                            Icon(
                                modifier = Modifier.size(default),
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = colors.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
package gr.indice.agents.dex.utilities

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImportContacts
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.graphics.vector.ImageVector
import gr.indice.agents.network.models.AgentInfo

val AgentInfo.iconVector: ImageVector? get() {
    return when(this.icon) {
        "sparkles" -> Icons.Outlined.Star
        "book" -> Icons.Outlined.ImportContacts
        "gear" -> Icons.Outlined.Settings
        else -> null
    }
}
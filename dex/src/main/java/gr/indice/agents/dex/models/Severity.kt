package gr.indice.agents.dex.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import gr.indice.agents.dex.models.Severity.Error
import gr.indice.agents.dex.models.Severity.Info
import gr.indice.agents.dex.models.Severity.Success
import gr.indice.agents.dex.models.Severity.Warning
import gr.indice.agents.dex.ui.theme.primaryColor

enum class Severity {
    Info, Success, Warning, Error
}

val Severity.image: ImageVector get() {
    return when(this) {
        Info -> Icons.Outlined.Info
        Success -> Icons.Default.CheckCircle
        Warning -> Icons.Default.Warning
        Error -> Icons.Default.Error
    }
}

val Severity.foregroundColor: Color
    get() = when(this) {
    Info -> primaryColor
    Success -> Color(0xFF00FF00)
    Warning -> Color(0xFFFFFF00)
    Error -> Color(0xFFFF0000)
}

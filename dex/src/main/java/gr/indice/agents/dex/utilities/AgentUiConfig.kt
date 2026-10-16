package gr.indice.agents.dex.utilities


import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import gr.indice.agents.dex.R
import gr.indice.agents.dex.ui.theme.LightBackground
import gr.indice.agents.dex.ui.theme.LightError
import gr.indice.agents.dex.ui.theme.LightOnBackground
import gr.indice.agents.dex.ui.theme.LightSecondaryContainer
import gr.indice.agents.dex.ui.theme.primaryColor


object AgentUiDefault {

    @Composable
    fun default(): AgentUiConfig {
        return AgentUiConfig(
            name = "Dex",
            icon = UiImage.Resource(painterResource(R.drawable.dex_logo_default)),
            colors = defaultColors()
        )
    }

    fun defaultColors(): AgentColors {
        return AgentColors(
            primary = primaryColor,
            secondary = LightSecondaryContainer,
            error = LightError,
            background = LightBackground,
            contentColor = LightOnBackground
        )
    }
}

data class AgentColors(
    val primary: Color,
    val secondary: Color,
    val error: Color,
    val background: Color,
    val contentColor: Color
)
@Stable
data class AgentUiConfig(
    val name: String,
    val icon: UiImage,
    val colors: AgentColors
)

fun solidSecondaryFrom(
    primary: Color,
    background: Color = Color.White,
    alpha: Float = 0.6f
): Color = primary.copy(alpha = alpha.coerceIn(0f, 1f)).compositeOver(background)

sealed interface UiImage {
    data class Resource(val painter: Painter): UiImage
    data class Vector(val imageVector: ImageVector): UiImage
    data class Url(val url: String): UiImage
}


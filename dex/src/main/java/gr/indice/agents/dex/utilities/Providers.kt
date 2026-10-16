package gr.indice.agents.dex.utilities

import androidx.compose.runtime.staticCompositionLocalOf

fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}

val LocalColors = staticCompositionLocalOf<AgentColors> {
    noLocalProvidedFor("LocalColors Provider")
}

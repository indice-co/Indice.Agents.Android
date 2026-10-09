package gr.indice.agents.dex.models


data class UiState(
    val events: String = "",
    val isConnected: Boolean = false,
    val statusText: String = "",
    val isLoading: Boolean = false
)
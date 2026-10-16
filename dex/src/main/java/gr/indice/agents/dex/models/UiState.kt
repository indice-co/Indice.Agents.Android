package gr.indice.agents.dex.models


data class UiState(
    val isConnected: Boolean = false,
    val statusText: String = "",
    val errorMessage: String? = null,
    val isCompleted: Boolean = false,
    val isLoading: Boolean = false,
    val initialling: Boolean = false
)
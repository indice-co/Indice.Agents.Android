package gr.indice.agents.network.models

data class ProblemDetails(
    val title: String?,
    val type: String?,
    val status: Int?,
    val instance: String?,
    val detail: String?,
    val data: String?,
    val errors: Map<String, List<String>>?
) {
    val description : String get() {
        if (!errors.isNullOrEmpty()) {
            return errors
                .flatMap { it.value }
                .joinToString(separator = "\n")
        }
        return detail ?: "Something went wrong"
    }
}

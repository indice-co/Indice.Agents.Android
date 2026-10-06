package gr.indice.agents.network.models

enum class DexChatPatchOp(val value: String) {
    Add("add"),
    Append("append"),
    Replace("replace");

    companion object {
        fun find(value: String): DexChatPatchOp? {
            return entries.find { it.value.equals(value, ignoreCase = true) }
        }
    }
}
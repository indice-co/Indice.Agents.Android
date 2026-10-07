package gr.indice.agents.network.adapters


import gr.indice.agents.network.models.DexChatPatchOp
import gr.indice.agents.network.models.DexChatResponse
import gr.indice.agents.network.models.Patch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

class PatchApplier {
    private val json = Json { ignoreUnknownKeys = true }
    private var document: JsonElement = JsonObject(emptyMap())
    private var previousPath: String? = null
    private var previousOperation: String? = null

    fun parseStartData(line: String): DexChatResponse = json.decodeFromString(line)
    fun parsePatch(line: String): Patch = json.decodeFromString<Patch>(line)

    fun apply(patch: Patch) {
        val (path, operationName) = resolveTarget(patch, previousPath, previousOperation)
        val operation = DexChatPatchOp.find(operationName.uppercase()) ?: return

        document = modify(node = document, path = segments(path), operation = operation, value = patch.value ?: JsonNull)

        previousPath = path
        previousOperation = operationName
    }


    fun result(): DexChatResponse = json.decodeFromJsonElement<DexChatResponse>(document)

    private fun resolveTarget(patch: Patch, previousPath: String?, previousOperation: String?): Pair<String, String> {
        val path = patch.path ?: previousPath
        val operation = patch.op ?: previousOperation
        if (path == null || operation == null) {
            throw Exception("The first delta must include a path and operation.")
        }
        return path to operation
    }

    private fun segments(path: String): List<String> {
        fun unescape(part: String): String {
            val result = StringBuilder()
            var index = 0

            while (index < part.length) {
                val character = part[index]

                if (character != '~') {
                    result.append(character)
                    index++
                    continue
                }

                when (part.getOrNull(index + 1)) {
                    '0' -> result.append('~')
                    '1' -> result.append('/')
                    else -> throw Exception("Invalid JSON Pointer escape.")
                }
                index += 2
            }

            return result.toString()
        }

        if(path.isEmpty()) return emptyList()

        if (!path.startsWith("/")) {
            throw Exception("Invalid JSON Pointer: $path")
        }

        return path.drop(1).split("/").map { unescape(it) }
    }

    private fun modify(node: JsonElement, path: List<String>, operation: DexChatPatchOp, value: JsonElement): JsonElement {
        if (path.isEmpty()) return applyToTarget(node, operation, value)

        val key = path.first()
        val tail = path.drop(1)

        return when (node) {
            is JsonObject -> modifyObject(node, key, tail, operation, value)
            is JsonArray -> modifyArray(node, key, tail, operation, value)
            else -> throw Exception("A patch parent must be an object or array.")
        }
    }

    private fun modifyObject(
        node: JsonObject,
        key: String,
        tail: List<String>,
        operation: DexChatPatchOp,
        value: JsonElement
    ): JsonElement {
        val fields = node.toMutableMap()
        val existing = fields[key]

        val child = if (tail.isEmpty()) {
            if (operation == DexChatPatchOp.Replace && existing == null) {
                throw Exception("replace requires an existing target.")
            }
            existing ?: if (operation == DexChatPatchOp.Append) JsonPrimitive("") else JsonNull
        } else {
            existing ?: throw Exception("Missing patch parent: $key")
        }

        fields[key] = modify(child, tail, operation, value)
        return JsonObject(fields)
    }

    private fun modifyArray(
        node: JsonArray,
        key: String,
        tail: List<String>,
        operation: DexChatPatchOp,
        value: JsonElement
    ): JsonElement {
        val items = node.toMutableList()
        val index = parseIndex(key, tail, operation, items.size)

        if (tail.isEmpty() && operation == DexChatPatchOp.Add) {
            if (index > items.size) throw Exception("Patch index is out of bounds.")
            items.add(index, value)
        } else {
            if (index !in items.indices) throw Exception("Patch index is out of bounds.")
            items[index] = modify(items[index], tail, operation, value)
        }

        return JsonArray(items)
    }

    private fun parseIndex(key: String, tail: List<String>, operation: DexChatPatchOp, size: Int): Int {
        if (key == "-" && tail.isEmpty() && operation == DexChatPatchOp.Add) return size

        val looksValid = key.isNotEmpty() &&
                key.all { it in '0'..'9' } &&
                (key == "0" || !key.startsWith("0"))

        return (if (looksValid) key.toIntOrNull() else null)
            ?: throw Exception("Invalid patch array index: $key")
    }

    private fun applyToTarget(node: JsonElement, operation: DexChatPatchOp, value: JsonElement): JsonElement {
        return when (operation) {
            DexChatPatchOp.Add, DexChatPatchOp.Replace -> value
            DexChatPatchOp.Append -> {
                if (!node.isText() || !value.isText()) {
                    throw Exception("append requires string operands.")
                }
                JsonPrimitive((node as JsonPrimitive).content + (value as JsonPrimitive).content)
            }
        }
    }

    private fun JsonElement.isText(): Boolean = this is JsonPrimitive && isString

}
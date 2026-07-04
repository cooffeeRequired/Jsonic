package cz.coffeerequired.jsonic.core

enum class PathTokenType {
    KEY, INDEX, LIST_INIT, LIST_ALL
}

data class PathToken(val key: String, val type: PathTokenType)

object PathEngine {
    private val tokenCache = object : LinkedHashMap<String, List<PathToken>>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<PathToken>>?): Boolean {
            return size > JsonicSettings.pathTokenCacheSize
        }
    }

    fun tokenize(path: String?): List<PathToken> {
        if (path.isNullOrEmpty()) return emptyList()
        val cleaned = convertPath(path.trim().trim('"'))
        val cacheKey = "$cleaned\u0000${JsonicSettings.pathDelimiter}"
        synchronized(tokenCache) {
            tokenCache[cacheKey]?.let { return it }
        }
        val tokens = parseTokens(cleaned, JsonicSettings.pathDelimiter)
        synchronized(tokenCache) { tokenCache[cacheKey] = tokens }
        return tokens
    }

    fun get(root: com.google.gson.JsonElement, path: String): com.google.gson.JsonElement? {
        var current: com.google.gson.JsonElement? = root
        for (token in tokenize(path)) {
            current = navigate(current ?: return null, token) ?: return null
        }
        return current
    }

    fun set(root: com.google.gson.JsonElement, path: String, value: com.google.gson.JsonElement) {
        val tokens = tokenize(path)
        if (tokens.isEmpty()) return
        if (tokens.last().type == PathTokenType.LIST_INIT) {
            appendToArray(root, tokens.dropLast(1), value)
            return
        }
        val parentTokens = tokens.dropLast(1)
        val last = tokens.last()
        var current = ensurePath(root, parentTokens)
        when (current) {
            is com.google.gson.JsonObject -> current.add(last.key, value)
            is com.google.gson.JsonArray -> {
                val index = last.key.toIntOrNull() ?: current.size()
                if (index >= current.size()) current.add(value) else current.set(index, value)
            }
        }
    }

    fun remove(root: com.google.gson.JsonElement, path: String) {
        val tokens = tokenize(path)
        if (tokens.isEmpty()) return
        val parentTokens = tokens.dropLast(1)
        val last = tokens.last()
        var current: com.google.gson.JsonElement? = root
        for (token in parentTokens) {
            current = navigate(current ?: return, token) ?: return
        }
        when (current) {
            is com.google.gson.JsonObject -> current.remove(last.key)
            is com.google.gson.JsonArray -> {
                val index = last.key.toIntOrNull() ?: return
                if (index in 0 until current.size()) current.remove(index)
            }
        }
    }

    private fun appendToArray(
        root: com.google.gson.JsonElement,
        parentTokens: List<PathToken>,
        value: com.google.gson.JsonElement
    ) {
        val parent = ensurePath(root, parentTokens)
        if (parent is com.google.gson.JsonArray) parent.add(value)
    }

    private fun ensurePath(root: com.google.gson.JsonElement, tokens: List<PathToken>): com.google.gson.JsonElement {
        if (tokens.isEmpty()) return root
        var current: com.google.gson.JsonElement = root
        for (token in tokens) {
            val next = navigate(current, token)
            if (next != null) {
                current = next
                continue
            }
            val created: com.google.gson.JsonElement = when (token.type) {
                PathTokenType.INDEX, PathTokenType.LIST_INIT, PathTokenType.LIST_ALL -> com.google.gson.JsonArray()
                else -> com.google.gson.JsonObject()
            }
            when (current) {
                is com.google.gson.JsonObject -> current.add(token.key, created)
                is com.google.gson.JsonArray -> {
                    val index = token.key.toIntOrNull() ?: current.size()
                    while (current.size() <= index) current.add(com.google.gson.JsonNull.INSTANCE)
                    current.set(index, created)
                }
            }
            current = created
        }
        return current
    }

    private fun navigate(current: com.google.gson.JsonElement, token: PathToken): com.google.gson.JsonElement? {
        return when (current) {
            is com.google.gson.JsonObject -> when (token.type) {
                PathTokenType.LIST_ALL -> current.get(token.key.replace("*", ""))
                else -> current.get(token.key)
            }
            is com.google.gson.JsonArray -> {
                val index = token.key.toIntOrNull() ?: return null
                if (index in 0 until current.size()) current.get(index) else null
            }
            else -> null
        }
    }

    private fun parseTokens(path: String, delim: String): List<PathToken> {
        val result = mutableListOf<PathToken>()
        for (segment in path.split(delim)) {
            if (segment.isEmpty()) continue
            when {
                segment.endsWith("[]") -> {
                    val name = segment.removeSuffix("[]")
                    if (name.isNotEmpty()) result += PathToken(name, PathTokenType.KEY)
                    result += PathToken("[]", PathTokenType.LIST_INIT)
                }
                segment.endsWith("*") -> result += PathToken(segment, PathTokenType.LIST_ALL)
                segment.matches(Regex("\\d+")) -> result += PathToken(segment, PathTokenType.INDEX)
                else -> result += PathToken(segment, PathTokenType.KEY)
            }
        }
        return result
    }

    private fun convertPath(input: String): String {
        val output = StringBuilder()
        for (segment in input.split(".")) {
            val arrayMatch = Regex("""^(.+)\[(\d+)]$""").find(segment)
            if (arrayMatch != null) {
                if (output.isNotEmpty()) output.append(JsonicSettings.pathDelimiter)
                output.append(arrayMatch.groupValues[1])
                output.append(JsonicSettings.pathDelimiter)
                output.append(arrayMatch.groupValues[2])
            } else if (segment.matches(Regex("""\[\d+]"""))) {
                if (output.isNotEmpty()) output.append(JsonicSettings.pathDelimiter)
                output.append(segment.substring(1, segment.length - 1))
            } else {
                if (output.isNotEmpty()) output.append(JsonicSettings.pathDelimiter)
                output.append(segment)
            }
        }
        return output.toString()
    }
}

package cz.coffeerequired.jsonic.core

import com.google.gson.JsonElement
import com.google.gson.JsonParser

private fun JsonElement.copyElement(): JsonElement = JsonParser.parseString(toString())

class JsonBuilder private constructor(private var root: JsonElement) {

    constructor() : this(JsonEngine.emptyObject())
    constructor(source: JsonElement, @Suppress("UNUSED_PARAMETER") copy: Boolean) : this(source.copyElement())

    fun path(path: String): JsonBuilderPath = JsonBuilderPath(this, path)

    fun merge(source: JsonElement, deep: Boolean = true): JsonBuilder {
        root = JsonMerge.merge(root, source, deep)
        return this
    }

    fun build(): JsonElement = root

    internal fun setAt(path: String, value: JsonElement) {
        PathEngine.set(root, path, value)
    }

    internal fun removeAt(path: String) {
        PathEngine.remove(root, path)
    }

    class JsonBuilderPath internal constructor(
        private val builder: JsonBuilder,
        private val path: String
    ) {
        fun set(value: Any?): JsonBuilder {
            builder.setAt(path, JsonEngine.toJson(value))
            return builder
        }

        fun remove(): JsonBuilder {
            builder.removeAt(path)
            return builder
        }
    }
}

object JsonMerge {
    fun merge(target: JsonElement, source: JsonElement, deep: Boolean): JsonElement {
        if (!deep) return source.copyElement()
        if (target.isJsonObject && source.isJsonObject) {
            val out = target.asJsonObject.copyElement().asJsonObject
            val src = source.asJsonObject
            for ((key, value) in src.entrySet()) {
                if (out.has(key) && out.get(key).isJsonObject && value.isJsonObject) {
                    out.add(key, merge(out.get(key), value, true))
                } else {
                    out.add(key, value.copyElement())
                }
            }
            return out
        }
        if (target.isJsonArray && source.isJsonArray) {
            val out = target.asJsonArray.copyElement().asJsonArray
            source.asJsonArray.forEach { out.add(it.copyElement()) }
            return out
        }
        return source.copyElement()
    }
}

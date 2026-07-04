package cz.coffeerequired.jsonic.core

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive

object JsonEngine {
    private val gson: Gson = GsonBuilder()
        .serializeNulls()
        .disableHtmlEscaping()
        .create()

    fun gson(): Gson = gson

    fun toJson(src: Any?): JsonElement {
        if (src == null || src === JsonNull.INSTANCE) return JsonNull.INSTANCE
        if (src is JsonElement) return src
        if (src is Boolean || src is Number) return gson.toJsonTree(src)
        if (src is String) {
            return try {
                JsonParser.parseString(src)
            } catch (_: Exception) {
                JsonPrimitive(src)
            }
        }
        return try {
            gson.toJsonTree(src)
        } catch (_: Exception) {
            val fallback = com.google.gson.JsonObject()
            fallback.addProperty("type", src.javaClass.name)
            fallback.addProperty("_toString", src.toString())
            fallback
        }
    }

    fun toJsonString(src: Any?): String = gson.toJson(toJson(src))

    @Suppress("UNCHECKED_CAST")
    fun <T> fromJson(json: JsonElement?): T? {
        if (json == null || json.isJsonNull) return null
        if (json is JsonPrimitive) {
            return when {
                json.isBoolean -> json.asBoolean as T
                json.isString -> json.asString as T
                json.isNumber -> json.asNumber as T
                else -> null
            }
        }
        return gson.fromJson(json, Any::class.java) as T
    }

    fun emptyObject(): JsonElement = com.google.gson.JsonObject()
    fun emptyArray(): JsonElement = com.google.gson.JsonArray()
}

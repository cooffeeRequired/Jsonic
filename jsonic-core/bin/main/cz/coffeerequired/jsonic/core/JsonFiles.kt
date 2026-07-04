package cz.coffeerequired.jsonic.core

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonParser
import com.google.gson.stream.JsonWriter
import org.bukkit.Bukkit
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.CompletableFuture

object JsonFiles {
    fun resolvePath(path: String): String {
        if (path.startsWith("~")) {
            val skript = Bukkit.getPluginManager().getPlugin("Skript")
            val base = skript?.dataFolder?.path ?: "plugins/Skript"
            return "$base/scripts/${path.substring(1)}"
        }
        return path
    }

    fun exists(path: String): Boolean = File(resolvePath(path)).exists()

    fun readAsync(path: String): CompletableFuture<JsonElement> {
        return CompletableFuture.supplyAsync {
            val file = File(resolvePath(path))
            if (!file.exists()) return@supplyAsync JsonNull.INSTANCE
            val ext = file.extension.lowercase()
            if (ext != "json" && ext != "jsonc") return@supplyAsync JsonNull.INSTANCE
            try {
                var raw = Files.readString(file.toPath(), StandardCharsets.UTF_8)
                if (ext == "jsonc") raw = JsonComments.prepare(raw)
                JsonParser.parseString(raw)
            } catch (_: Exception) {
                JsonNull.INSTANCE
            }
        }
    }

    fun writeAsync(path: String, content: JsonElement, pretty: Boolean = true): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val file = File(resolvePath(path))
                file.parentFile?.mkdirs()
                val json = if (pretty) JsonEngine.gson().toJson(content) else content.toString()
                Files.writeString(file.toPath(), json, StandardCharsets.UTF_8)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}

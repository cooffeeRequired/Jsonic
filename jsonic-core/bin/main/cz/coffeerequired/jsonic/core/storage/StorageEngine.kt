package cz.coffeerequired.jsonic.core.storage

import com.google.gson.JsonElement
import cz.coffeerequired.jsonic.core.JsonFiles
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class CachedJsonEntry(
    val id: String,
    var json: JsonElement,
    var filePath: String? = null,
    var watcherEnabled: Boolean = false
)

object StorageEngine {
    private val cache = ConcurrentHashMap<String, CachedJsonEntry>()

    fun exists(id: String): Boolean = cache.containsKey(id)

    fun get(id: String): CachedJsonEntry? = cache[id]

    fun getOrCreate(id: String): CachedJsonEntry {
        return cache.computeIfAbsent(id) { CachedJsonEntry(id, com.google.gson.JsonObject()) }
    }

    fun bindFile(id: String, path: String, enableWatcher: Boolean = false): CachedJsonEntry {
        val entry = getOrCreate(id)
        entry.filePath = path
        entry.watcherEnabled = enableWatcher
        JsonFiles.readAsync(path).thenAccept { json ->
            entry.json = json
            if (enableWatcher) {
                CacheWatcher.register(id, path) { updated ->
                    entry.json = updated
                    StorageEvents.fireChange(id, updated)
                }
            }
        }
        return entry
    }

    fun put(id: String, json: JsonElement): CachedJsonEntry {
        val entry = getOrCreate(id)
        entry.json = json
        return entry
    }

    fun remove(id: String) {
        CacheWatcher.unregister(id)
        cache.remove(id)
    }

    fun clear() {
        CacheWatcher.unregisterAll()
        cache.clear()
    }
}

object StorageEvents {
    private val listeners = ConcurrentHashMap<String, (String, JsonElement) -> Unit>()

    fun onChange(listener: (String, JsonElement) -> Unit) {
        listeners[UUID.randomUUID().toString()] = listener
    }

    fun fireChange(id: String, json: JsonElement) {
        listeners.values.forEach { it(id, json) }
    }
}

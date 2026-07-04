package cz.coffeerequired.jsonic.core.storage

import com.google.gson.JsonElement
import cz.coffeerequired.jsonic.core.JsonFiles
import cz.coffeerequired.jsonic.core.JsonicSettings
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object CacheWatcher {
    private val watchers = ConcurrentHashMap<String, Watcher>()
    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "Jsonic-CacheWatcher").apply { isDaemon = true }
    }

    fun register(id: String, path: String, onUpdate: (JsonElement) -> Unit) {
        unregister(id)
        val file = java.io.File(JsonFiles.resolvePath(path))
        watchers[id] = Watcher(file, onUpdate).also { it.start() }
    }

    fun unregister(id: String) {
        watchers.remove(id)?.stop()
    }

    fun unregisterAll() {
        watchers.keys.toList().forEach { unregister(it) }
        scheduler.shutdownNow()
    }

    private class Watcher(
        private val file: java.io.File,
        private val onUpdate: (JsonElement) -> Unit
    ) {
        @Volatile
        private var lastModified: Long = if (file.exists()) file.lastModified() else 0L
        private var task: ScheduledFuture<*>? = null

        fun start() {
            task = scheduler.scheduleAtFixedRate({
                if (!file.exists()) return@scheduleAtFixedRate
                val modified = file.lastModified()
                if (modified != lastModified) {
                    lastModified = modified
                    JsonFiles.readAsync(file.path).thenAccept(onUpdate)
                }
            }, JsonicSettings.watcherIntervalMs, JsonicSettings.watcherRefreshRateMs, TimeUnit.MILLISECONDS)
        }

        fun stop() {
            task?.cancel(false)
        }
    }
}

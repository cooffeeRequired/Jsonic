package cz.coffeerequired.jsonic

import cz.coffeerequired.jsonic.core.JsonicLogger
import cz.coffeerequired.jsonic.core.JsonicSettings
import cz.coffeerequired.jsonic.core.http.HttpClientProvider
import cz.coffeerequired.jsonic.core.storage.StorageEngine
import cz.coffeerequired.jsonic.server.ServerEngine
import cz.coffeerequired.jsonic.skript.JsonicAddon
import org.bstats.bukkit.Metrics
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

class JsonicPlugin : JavaPlugin() {

    override fun onLoad() {
        instance = this
        saveDefaultConfig()
        reloadLocalConfig()
        JsonicUpdater.applyScheduledUpdate(this)
    }

    override fun onEnable() {
        JsonicLogger.info("Enabling Jsonic…")
        JsonicUpdater.checkForUpdate(this)
        JsonicAddon.register(this)
        Metrics(this, 0)
        JsonicLogger.accent("Jsonic enabled.")
    }

    override fun onDisable() {
        ServerEngine.stop()
        StorageEngine.clear()
        HttpClientProvider.shutdown()
        JsonicLogger.info("Jsonic disabled.")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        when (args.getOrNull(0)?.lowercase()) {
            "reload" -> {
                reloadConfig()
                reloadLocalConfig()
                JsonicLogger.msg(sender, Level.INFO, "&aConfig reloaded.")
            }
            "debug" -> {
                JsonicSettings.debug = args.getOrNull(1)?.lowercase() in setOf("on", "true", "1")
                val state = if (JsonicSettings.debug) "&aON" else "&cOFF"
                JsonicLogger.msg(sender, Level.INFO, "Debug: $state")
            }
            "fulltest" -> {
                if (!sender.hasPermission("jsonic.test")) {
                    JsonicLogger.msg(sender, Level.SEVERE, "Nemáš oprávnění jsonic.test")
                    return true
                }
                JsonicFullTest.run(sender, this)
            }
            else -> JsonicLogger.msg(sender, Level.WARNING, "/jsonic reload|debug [on|off]|fulltest")
        }
        return true
    }

    private fun reloadLocalConfig() {
        val cfg = config
        JsonicSettings.debug = cfg.getBoolean("plugin.debug", false)
        JsonicSettings.autoUpdater = cfg.getBoolean("plugin.enabled-auto-updater", false)
        JsonicSettings.pathDelimiter = cfg.getString("json.path-delimiter", ".") ?: "."
        JsonicSettings.pathTokenCacheSize = cfg.getInt("json.path-token-cache-size", 1024)
        JsonicSettings.httpEnabled = cfg.getBoolean("plugin.enabled-http", true)
        JsonicSettings.httpMaxThreads = cfg.getInt("plugin.max-threads", 2)
        JsonicSettings.httpRequestTimeoutSeconds = cfg.getInt("plugin.http-request-timeout-seconds", 30)
        JsonicSettings.httpConnectTimeoutSeconds = cfg.getInt("plugin.http-connect-timeout-seconds", 10)
        JsonicSettings.serverEnabled = cfg.getBoolean("server.enabled", true)
        JsonicSettings.serverPort = cfg.getInt("server.port", 8080)
        JsonicSettings.serverHost = cfg.getString("server.host", "127.0.0.1") ?: "127.0.0.1"
        JsonicSettings.serverPublicBind = cfg.getBoolean("server.public-bind", false)
        JsonicSettings.serverHttpsEnabled = cfg.getBoolean("server.https.enabled", false)
        JsonicSettings.serverHttpsPort = cfg.getInt("server.https.port", 8443)
        JsonicSettings.serverHttpsCertFile = cfg.getString("server.https.cert-file")
        JsonicSettings.serverHttpsKeyFile = cfg.getString("server.https.key-file")
        JsonicSettings.serverHttpsKeyPassword = cfg.getString("server.https.key-password", "") ?: ""
        JsonicSettings.serverMiddlewareCors = cfg.getBoolean("server.middleware.cors", false)
        JsonicSettings.serverMiddlewareLogger = cfg.getBoolean("server.middleware.logger", false)
        JsonicSettings.serverMiddlewareJson = cfg.getBoolean("server.middleware.json", false)
        JsonicSettings.watcherIntervalMs = cfg.getLong("json.watcher.interval", 100)
        JsonicSettings.watcherRefreshRateMs = cfg.getLong("json.watcher.refresh-rate", 50)
        JsonicSettings.serverThreadModel = when (cfg.getString("server.thread-model", "main")?.lowercase()) {
            "async" -> JsonicSettings.ThreadModel.ASYNC
            else -> JsonicSettings.ThreadModel.MAIN
        }
    }

    companion object {
        @JvmStatic
        lateinit var instance: JsonicPlugin
            private set

        @JvmStatic
        fun log(level: Level, message: String) {
            JsonicLogger.log(level, message)
        }

        @JvmStatic
        fun debug(message: String) {
            JsonicLogger.debug(message)
        }
    }
}

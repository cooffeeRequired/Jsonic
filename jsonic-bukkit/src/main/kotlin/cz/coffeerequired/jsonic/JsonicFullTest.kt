package cz.coffeerequired.jsonic

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import cz.coffeerequired.jsonic.core.JsonBuilder
import cz.coffeerequired.jsonic.core.JsonComments
import cz.coffeerequired.jsonic.core.JsonEngine
import cz.coffeerequired.jsonic.core.JsonFiles
import cz.coffeerequired.jsonic.core.JsonicSettings
import cz.coffeerequired.jsonic.core.PathEngine
import cz.coffeerequired.jsonic.core.http.HttpEngine
import cz.coffeerequired.jsonic.core.http.HttpMethod
import cz.coffeerequired.jsonic.core.storage.StorageEngine
import cz.coffeerequired.jsonic.server.JsonicApp
import cz.coffeerequired.jsonic.server.MiddlewareType
import cz.coffeerequired.jsonic.server.ServerEngine
import org.bukkit.command.CommandSender
import org.bukkit.scheduler.BukkitRunnable
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest as JHttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.time.Duration
object JsonicFullTest {

    private data class Result(var pass: Int = 0, var fail: Int = 0, val failures: MutableList<String> = mutableListOf())

    fun run(sender: CommandSender, plugin: JsonicPlugin) {
        sender.sendMessage("§7[Jsonic] §fSpouštím full test…")
        object : BukkitRunnable() {
            override fun run() {
                val result = Result()
                runCoreTests(result)
                runHttpTests(result)
                runStorageTests(result)
                runServerTests(plugin, result)
                report(sender, result)
                triggerSkriptTests(plugin, sender)
            }
        }.runTaskAsynchronously(plugin)
    }

    private fun runCoreTests(result: Result) {
        check(result, "JsonEngine.parse") {
            val json = JsonEngine.toJson("""{"ok":true}""")
            json.isJsonObject && json.asJsonObject.get("ok").asBoolean
        }
        check(result, "JsonComments.jsonc") {
            val raw = """
                { "a": 1, // comment
                  "b": 2, }
            """.trimIndent()
            val cleaned = JsonComments.prepare(raw)
            val parsed = JsonParser.parseString(cleaned)
            parsed.isJsonObject
                    && parsed.asJsonObject.get("a").asInt == 1
                    && parsed.asJsonObject.get("b").asInt == 2
        }
        check(result, "PathEngine.set/get") {
            val root = JsonObject()
            PathEngine.set(root, "user.name", JsonEngine.toJson("Coffee"))
            PathEngine.get(root, "user.name")?.asString == "Coffee"
        }
        check(result, "PathEngine.remove") {
            val root = JsonObject()
            PathEngine.set(root, "x", JsonEngine.toJson(1))
            PathEngine.remove(root, "x")
            PathEngine.get(root, "x") == null
        }
        check(result, "JsonBuilder") {
            val built = JsonBuilder().path("tags[]").set("skript").build()
            built.isJsonObject
        }
        check(result, "JsonFiles.write/read") {
            val temp = Files.createTempFile("jsonic-test", ".json").toString()
            val payload = JsonEngine.toJson(mapOf("test" to true))
            val written = JsonFiles.writeAsync(temp, payload, true).join()
            val read = JsonFiles.readAsync(temp).join()
            Files.deleteIfExists(java.nio.file.Path.of(temp))
            written && read.isJsonObject && read.asJsonObject.get("test").asBoolean
        }
    }

    private fun runHttpTests(result: Result) {
        check(result, "HttpEngine.MOCK") {
            val req = HttpEngine.createRequest("https://example.com/api", HttpMethod.MOCK)
            val raw = HttpEngine.sendBlocking(req)
            raw != null && raw.statusCode == 200 && raw.body.contains("mock")
        }
        check(result, "HttpEngine.external") {
            val req = HttpEngine.createRequest("https://dummyjson.com/quotes/random", HttpMethod.GET)
            val raw = HttpEngine.sendBlocking(req)
            raw != null && raw.statusCode == 200
        }
    }

    private fun runStorageTests(result: Result) {
        check(result, "StorageEngine.put/get") {
            StorageEngine.put("fulltest", JsonEngine.toJson(mapOf("id" to "fulltest")))
            StorageEngine.get("fulltest")?.json?.isJsonObject == true
        }
        check(result, "StorageEngine.exists") {
            StorageEngine.exists("fulltest")
        }
    }

    private fun runServerTests(plugin: JsonicPlugin, result: Result) {
        if (!JsonicSettings.serverEnabled) {
            result.failures.add("ServerEngine (skipped — server disabled in config)")
            return
        }
        val port = 18091
        try {
            ServerEngine.stop()
            val app = JsonicApp()
                .port(port)
                .host("127.0.0.1")
                .use(MiddlewareType.CORS)
            app.registerGet("/health") { call ->
                call.queueJson(JsonEngine.toJson(mapOf("status" to "ok", "addon" to "jsonic")))
            }
            ServerEngine.start(plugin, app).join()
            check(result, "ServerEngine.start") {
                ServerEngine.isRunning()
            }
            check(result, "ServerEngine.http") {
                val client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build()
                val response = client.send(
                    JHttpRequest.newBuilder(URI.create("http://127.0.0.1:$port/health")).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
                )
                response.statusCode() == 200 && response.body().contains("ok")
            }
        } catch (e: Exception) {
            result.fail++
            result.failures.add("ServerEngine: ${e.message}")
        } finally {
            ServerEngine.stop()
        }
    }

    private fun triggerSkriptTests(plugin: JsonicPlugin, sender: CommandSender) {
        object : BukkitRunnable() {
            override fun run() {
                plugin.server.dispatchCommand(
                    plugin.server.consoleSender,
                    "sk reload jsonic/jsonic-fulltest"
                )
                object : BukkitRunnable() {
                    override fun run() {
                        plugin.server.dispatchCommand(
                            plugin.server.consoleSender,
                            "jsonicfulltest ${sender.name}"
                        )
                    }
                }.runTaskLater(plugin, 40L)
                sender.sendMessage("§7[Jsonic] §fSkript test suite se spouští…")
            }
        }.runTask(plugin)
    }

    private fun check(result: Result, name: String, block: () -> Boolean) {
        try {
            if (block()) {
                result.pass++
                JsonicPlugin.debug("[FullTest] OK: $name")
            } else {
                result.fail++
                result.failures.add(name)
                JsonicPlugin.log(java.util.logging.Level.WARNING, "[FullTest] FAIL: $name")
            }
        } catch (e: Exception) {
            result.fail++
            result.failures.add("$name (${e.message})")
            JsonicPlugin.log(java.util.logging.Level.WARNING, "[FullTest] FAIL: $name — ${e.message}")
        }
    }

    private fun report(sender: CommandSender, result: Result) {
        object : BukkitRunnable() {
            override fun run() {
                sender.sendMessage("§7[Jsonic FullTest] §aOK: ${result.pass} §cFAIL: ${result.fail}")
                if (result.fail == 0) {
                    sender.sendMessage("§aJava/core testy prošly.")
                } else {
                    result.failures.forEach { sender.sendMessage("§c✗ $it") }
                }
            }
        }.runTask(JsonicPlugin.instance)
    }
}

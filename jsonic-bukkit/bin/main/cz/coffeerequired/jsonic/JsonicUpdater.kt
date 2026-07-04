package cz.coffeerequired.jsonic

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import cz.coffeerequired.jsonic.core.JsonicLogger
import cz.coffeerequired.jsonic.core.JsonicSettings
import org.bukkit.plugin.java.JavaPlugin
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Formatter

object JsonicUpdater {

    private const val GITHUB_USER = "SkJsonTeam"
    private const val REPOSITORY = "Jsonic"

    @JvmStatic
    fun applyScheduledUpdate(plugin: JavaPlugin) {
        val updateFile = File(plugin.dataFolder, "updated.yml")
        if (!updateFile.exists()) return
        try {
            BufferedReader(FileReader(updateFile)).use { reader ->
                val targetFile = reader.readLine().substringAfter(": ").trim()
                val tempFile = reader.readLine().substringAfter(": ").trim()
                val normalizedTarget = targetFile.replaceFirst(".paper-remapped\\\\".toRegex(), "")
                Files.move(Path.of(tempFile), Path.of(normalizedTarget), StandardCopyOption.REPLACE_EXISTING)
                JsonicLogger.info("Update applied successfully.")
                Files.delete(updateFile.toPath())
            }
        } catch (e: Exception) {
            JsonicLogger.severe("Failed to apply scheduled update: ${e.message}")
        }
    }

    @JvmStatic
    fun checkForUpdate(plugin: JavaPlugin) {
        try {
            val url = URI("https://api.github.com/repos/$GITHUB_USER/$REPOSITORY/releases/latest")
            JsonicLogger.info("Checking for updates…")

            val conn = url.toURL().openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")

            val response = buildString {
                conn.inputStream.bufferedReader().useLines { lines -> lines.forEach { append(it) } }
            }
            conn.disconnect()

            val json = JsonParser.parseString(response).asJsonObject
            val latestVersion = json.get("tag_name").asString
            val currentVersion = plugin.pluginMeta.version

            when (compareVersions(currentVersion, latestVersion)) {
                -1 -> {
                    val assets = json.getAsJsonArray("assets")
                    if (assets == null || assets.size() == 0) {
                        JsonicLogger.warning("Release %s has no downloadable assets.", latestVersion)
                        return
                    }
                    val downloadUrl = assets[0].asJsonObject.get("browser_download_url").asString
                    JsonicLogger.info(
                        "Running &e'%s'&r — latest is &a'%s'",
                        currentVersion,
                        latestVersion
                    )
                    if (!JsonicSettings.autoUpdater) {
                        JsonicLogger.info("&c✖ &rAuto updater disabled in config.")
                        return
                    }
                    scheduleUpdate(plugin, downloadUrl)
                }
                1 -> JsonicLogger.info("Development build — no update required &a✔")
                else -> JsonicLogger.info("Jsonic is up-to-date &a✔")
            }
        } catch (e: Exception) {
            JsonicLogger.warning("Update check failed: ${e.message}")
        }
    }

    private fun scheduleUpdate(plugin: JavaPlugin, downloadUrl: String) {
        try {
            val pluginFile = File(plugin.javaClass.protectionDomain.codeSource.location.toURI())
            val tempPath = Files.createTempFile("jsonic-update", ".tmp")
            URI(downloadUrl).toURL().openStream().use { input ->
                Files.copy(input, tempPath, StandardCopyOption.REPLACE_EXISTING)
            }
            createUpdateYml(plugin, pluginFile.toPath(), tempPath)
        } catch (e: Exception) {
            JsonicLogger.severe("Failed to schedule update: ${e.message}")
        }
    }

    private fun createUpdateYml(plugin: JavaPlugin, targetPath: Path, tempPath: Path) {
        val updateFile = File(plugin.dataFolder, "updated.yml")
        BufferedWriter(FileWriter(updateFile)).use { writer ->
            writer.write("target_file: $targetPath")
            writer.newLine()
            writer.write("temp_file: $tempPath")
            writer.newLine()
            writer.write("old_file_hash: ${fileHash(targetPath.toFile())}")
            writer.newLine()
            writer.write("new_file_hash: ${fileHash(tempPath.toFile())}")
        }
        JsonicLogger.info("Update scheduled — will apply after server restart.")
    }

    private fun fileHash(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file.toPath()).use { input ->
            val buffer = ByteArray(1024)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                md.update(buffer, 0, read)
            }
        }
        Formatter().use { formatter ->
            for (b in md.digest()) {
                formatter.format("%02x", b)
            }
            return formatter.toString()
        }
    }

    private fun compareVersions(left: String, right: String): Int {
        val leftParts = left.replace(Regex("^[^0-9]*"), "").split(".")
        val rightParts = right.replace(Regex("^[^0-9]*"), "").split(".")
        val length = maxOf(leftParts.size, rightParts.size)
        for (i in 0 until length) {
            val leftPart = if (i < leftParts.size) parseVersionPart(leftParts[i]) else 0
            val rightPart = if (i < rightParts.size) parseVersionPart(rightParts[i]) else 0
            if (leftPart != rightPart) return leftPart.compareTo(rightPart)
        }
        return 0
    }

    private fun parseVersionPart(part: String): Int =
        part.replace(Regex("[^0-9].*"), "").toIntOrNull() ?: 0
}

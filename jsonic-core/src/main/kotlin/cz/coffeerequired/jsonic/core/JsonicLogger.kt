package cz.coffeerequired.jsonic.core

import cz.coffeerequired.jsonic.core.support.AnsiColorConverter
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import java.util.IllegalFormatException
import java.util.logging.Level
import java.util.logging.Logger

object JsonicLogger {

    const val BRAND: String = "&bJsonic&r"
    private const val BRAND_HEX: String = "&#6EE7B7"

    private val legacy = LegacyComponentSerializer.builder()
        .character('&')
        .hexColors()
        .build()

    private val logger: Logger
        get() = Bukkit.getLogger()

    @JvmStatic
    fun log(level: Level, message: String?, vararg args: Any?) {
        if (message == null) {
            logger.log(Level.WARNING, AnsiColorConverter.convertToAnsi("[$BRAND&e] Null message provided"))
            return
        }

        var msgText = message
        if (msgText.contains("{") || msgText.contains("}")) {
            msgText = msgText.replace("%{", "").replace("}%", "")
        }

        var prefix = BRAND
        var colorCode = "&r"
        when (level) {
            Level.SEVERE -> {
                prefix = "&bJsonic&c"
                colorCode = "&c"
            }
            Level.WARNING -> {
                prefix = "&bJsonic&e"
                colorCode = "&e"
            }
            Level.INFO -> prefix = BRAND
            else -> Unit
        }

        try {
            msgText = if (args.isNotEmpty()) String.format(msgText, *args) else msgText
        } catch (_: IllegalFormatException) {
            // keep raw message
        }

        val text = AnsiColorConverter.convertToAnsi("[$prefix] $colorCode$msgText")
        logger.log(level, text)
    }

    @JvmStatic
    fun info(message: String, vararg args: Any?) = log(Level.INFO, message, *args)

    @JvmStatic
    fun warning(message: String, vararg args: Any?) = log(Level.WARNING, message, *args)

    @JvmStatic
    fun severe(message: String, vararg args: Any?) = log(Level.SEVERE, message, *args)

    @JvmStatic
    fun debug(message: String, vararg args: Any?) {
        if (JsonicSettings.debug) {
            log(Level.INFO, "&8DEBUG → &7$message", *args)
        }
    }

    @JvmStatic
    fun accent(message: String, vararg args: Any?) {
        val hex = AnsiColorConverter.hexToAnsi("#6EE7B7")
        var msgText = message
        try {
            msgText = if (args.isNotEmpty()) String.format(msgText, *args) else msgText
        } catch (_: IllegalFormatException) {
            // keep raw message
        }
        logger.log(Level.INFO, AnsiColorConverter.convertToAnsi("[$BRAND] $hex$msgText"))
    }

    @JvmStatic
    fun logElement(category: String, count: Int) {
        if (count <= 0) return
        val label = coloredElement(category)
        info(
            "&8" + AnsiColorConverter.hexToAnsi("#6EE7B7") + " + %s &f%d" + AnsiColorConverter.RESET,
            label,
            count
        )
    }

    @JvmStatic
    fun msg(sender: CommandSender, level: Level, message: String, vararg args: Any?) {
        var formatted = message
        try {
            formatted = if (args.isNotEmpty()) String.format(message, *args) else message
        } catch (_: IllegalFormatException) {
            // keep raw message
        }
        val color = when (level) {
            Level.SEVERE -> "&c"
            Level.WARNING -> "&e"
            else -> "&7"
        }
        sender.sendMessage(legacy.deserialize("&l$BRAND&r$color $formatted"))
    }

    private fun coloredElement(input: String): String = when (input) {
        "Expressions" -> "&aExpressions"
        "Effects" -> "&bEffects"
        "Events" -> "&5Events"
        "Sections" -> "&fSections"
        "Conditions" -> "&4Conditions"
        "Functions" -> "&7Functions"
        "Structures" -> "&9Structures"
        "Types" -> "&6Types"
        "Event Values" -> "&aEvent Values"
        else -> input
    }
}

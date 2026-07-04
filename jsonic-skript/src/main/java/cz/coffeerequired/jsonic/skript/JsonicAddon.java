package cz.coffeerequired.jsonic.skript;

import ch.njol.skript.Skript;
import cz.coffeerequired.jsonic.core.JsonicLogger;
import cz.coffeerequired.jsonic.core.JsonicSettings;
import cz.coffeerequired.jsonic.skript.modules.CoreModule;
import cz.coffeerequired.jsonic.skript.modules.HttpModule;
import cz.coffeerequired.jsonic.skript.modules.ServerModule;
import cz.coffeerequired.jsonic.skript.modules.StorageModule;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.skriptlang.skript.addon.SkriptAddon;

public final class JsonicAddon {

    /** Skript treats {@code […]} as optional syntax — do not prefix patterns with {@code [jsonic]}. */
    public static final String PREFIX = "";
    private static SkriptAddon addon;
    private static JavaPlugin plugin;

    private JsonicAddon() {
    }

    public static SkriptAddon getAddon() {
        return addon;
    }

    public static JavaPlugin getPlugin() {
        return plugin;
    }

    public static void register(JavaPlugin plugin) {
        JsonicAddon.plugin = plugin;
        if (!Bukkit.getPluginManager().isPluginEnabled("Skript")) {
            JsonicLogger.severe("Skript not found — disabling Jsonic");
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }
        try {
            Class.forName("org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry");
        } catch (ClassNotFoundException e) {
            JsonicLogger.severe("Jsonic requires Skript 2.15+");
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }

        addon = Skript.instance().registerAddon(plugin.getClass(), plugin.getName());
        addon.localizer().setSourceDirectories(
                "lang",
                plugin.getDataFolder().getAbsolutePath() + "/lang"
        );

        JsonicLogger.info("Hooking into Skript…");
        JsonicRegister.reset();

        JsonicRegister.registerModule("Core", CoreModule::register);
        JsonicRegister.registerModule("Storage", StorageModule::register);
        if (JsonicSettings.INSTANCE.getHttpEnabled()) {
            JsonicRegister.registerModule("Http", HttpModule::register);
        } else {
            JsonicLogger.info("&7HTTP module skipped (&eplugin.enabled-http=false&7)");
        }
        if (JsonicSettings.INSTANCE.getServerEnabled()) {
            JsonicRegister.registerModule("Server", ServerModule::register);
        } else {
            JsonicLogger.info("&7Server module skipped (&eserver.enabled=false&7)");
        }

        JsonicRegister.printSummary();
        JsonicLogger.accent("Skript syntax registered.");
    }

    public static String[] prefixPatterns(String... patterns) {
        String[] prefixed = patterns.clone();
        for (int i = 0; i < prefixed.length; i++) {
            prefixed[i] = PREFIX + prefixed[i];
        }
        return prefixed;
    }
}

package cz.coffeerequired.jsonic.skript.server;

import ch.njol.skript.lang.Trigger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Named middleware bodies from {@code define middleware "name":}. */
public final class MiddlewareDefinitionRegistry {

    private static final Map<String, Trigger> DEFINED = new ConcurrentHashMap<>();

    private MiddlewareDefinitionRegistry() {
    }

    public static void define(String name, Trigger trigger) {
        DEFINED.put(normalize(name), trigger);
    }

    public static void remove(String name) {
        DEFINED.remove(normalize(name));
    }

    public static Trigger get(String name) {
        return DEFINED.get(normalize(name));
    }

    public static boolean isDefined(String name) {
        return DEFINED.containsKey(normalize(name));
    }

    public static void clear() {
        DEFINED.clear();
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase().replace('-', '_');
    }
}

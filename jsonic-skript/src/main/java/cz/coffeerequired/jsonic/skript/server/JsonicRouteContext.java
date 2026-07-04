package cz.coffeerequired.jsonic.skript.server;

import cz.coffeerequired.jsonic.server.JsonicApp;
import cz.coffeerequired.jsonic.server.JsonicCall;

public final class JsonicRouteContext {

    private static final ThreadLocal<JsonicCall> CURRENT = new ThreadLocal<>();

    private JsonicRouteContext() {
    }

    public static void set(JsonicCall call) {
        CURRENT.set(call);
    }

    public static JsonicCall get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}

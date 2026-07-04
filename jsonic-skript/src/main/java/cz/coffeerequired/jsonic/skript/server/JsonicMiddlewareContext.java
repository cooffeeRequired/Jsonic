package cz.coffeerequired.jsonic.skript.server;

/** Per-request state while a Skript middleware chain runs. */
public final class JsonicMiddlewareContext {

    private static final ThreadLocal<Boolean> CONTINUE = new ThreadLocal<>();

    private JsonicMiddlewareContext() {
    }

    public static void begin() {
        CONTINUE.set(true);
    }

    public static void setContinue(boolean value) {
        CONTINUE.set(value);
    }

    public static boolean finish() {
        Boolean value = CONTINUE.get();
        clear();
        return value == null || value;
    }

    public static void clear() {
        CONTINUE.remove();
    }
}

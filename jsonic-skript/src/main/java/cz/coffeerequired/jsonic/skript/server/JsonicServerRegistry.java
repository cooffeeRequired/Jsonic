package cz.coffeerequired.jsonic.skript.server;

import cz.coffeerequired.jsonic.server.JsonicApp;

public final class JsonicServerRegistry {

    private static JsonicApp defaultApp;
    private static JsonicApp runningApp;

    private JsonicServerRegistry() {
    }

    public static JsonicApp getDefaultApp() {
        return defaultApp;
    }

    public static void setDefaultApp(JsonicApp app) {
        defaultApp = app;
    }

    public static JsonicApp getRunningApp() {
        return runningApp;
    }

    public static void setRunningApp(JsonicApp app) {
        runningApp = app;
    }
}

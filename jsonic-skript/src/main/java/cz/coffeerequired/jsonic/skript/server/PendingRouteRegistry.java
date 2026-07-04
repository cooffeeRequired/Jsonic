package cz.coffeerequired.jsonic.skript.server;

import ch.njol.skript.lang.Trigger;
import cz.coffeerequired.jsonic.core.JsonicLogger;
import cz.coffeerequired.jsonic.core.JsonicSettings;
import cz.coffeerequired.jsonic.server.JsonPathUtils;
import cz.coffeerequired.jsonic.server.JsonicApp;
import cz.coffeerequired.jsonic.server.JsonicCall;
import cz.coffeerequired.jsonic.server.JsonicRouteMiddleware;
import cz.coffeerequired.jsonic.server.MiddlewareType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Routes collected during parse; flushed after all {@code define middleware} structures loaded. */
public final class PendingRouteRegistry {

    private static final Map<JsonicApp, List<PendingRoute>> BY_APP = new ConcurrentHashMap<>();

    private PendingRouteRegistry() {
    }

    public static void add(JsonicApp app, PendingRoute route) {
        if (app == null) {
            return;
        }
        BY_APP.computeIfAbsent(app, ignored -> new CopyOnWriteArrayList<>()).add(route);
        if (JsonicSettings.INSTANCE.getDebug()) {
            JsonicLogger.debug("Pending route: %s %s", route.method, route.path);
        }
    }

    public static void flushTo(JsonicApp app) {
        if (app == null) {
            return;
        }
        List<PendingRoute> pending = BY_APP.get(app);
        if (pending == null || pending.isEmpty()) {
            if (JsonicSettings.INSTANCE.getDebug()) {
                JsonicLogger.debug("PendingRouteRegistry.flushTo: no pending routes");
            }
            return;
        }
        app.clearRoutes();
        for (PendingRoute route : pending) {
            route.register(app);
        }
    }

    public static void clear(JsonicApp app) {
        if (app != null) {
            BY_APP.remove(app);
        }
    }

    public static final class PendingRoute {
        private final String method;
        private final String path;
        private final Consumer<JsonicCall> handler;
        private final List<MiddlewareType> builtInMiddlewares;
        private final List<String> skriptMiddlewareNames;
        private final List<JsonicRouteMiddleware> inlineSkriptMiddlewares;

        public PendingRoute(
                String method,
                String path,
                Consumer<JsonicCall> handler,
                List<MiddlewareType> builtInMiddlewares,
                List<String> skriptMiddlewareNames,
                List<JsonicRouteMiddleware> inlineSkriptMiddlewares
        ) {
            this.method = method;
            this.path = path;
            this.handler = handler;
            this.builtInMiddlewares = List.copyOf(builtInMiddlewares);
            this.skriptMiddlewareNames = List.copyOf(skriptMiddlewareNames);
            this.inlineSkriptMiddlewares = List.copyOf(inlineSkriptMiddlewares);
        }

        void register(JsonicApp app) {
            List<JsonicRouteMiddleware> resolved = new ArrayList<>(inlineSkriptMiddlewares);
            for (String name : skriptMiddlewareNames) {
                Trigger trigger = MiddlewareDefinitionRegistry.get(name);
                if (trigger != null) {
                    resolved.add(JsonicMiddlewareRunner.fromTrigger(trigger));
                }
            }
            app.registerRouteNamedWithSkriptMiddleware(
                    method,
                    JsonPathUtils.normalize(path),
                    handler,
                    builtInMiddlewares,
                    resolved
            );
        }
    }
}

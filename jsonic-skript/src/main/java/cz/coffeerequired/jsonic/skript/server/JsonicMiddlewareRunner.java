package cz.coffeerequired.jsonic.skript.server;

import ch.njol.skript.lang.Trigger;
import cz.coffeerequired.jsonic.server.JsonicCall;
import cz.coffeerequired.jsonic.server.JsonicRouteMiddleware;

public final class JsonicMiddlewareRunner {

    private JsonicMiddlewareRunner() {
    }

    public static JsonicRouteMiddleware fromTrigger(Trigger trigger) {
        return call -> {
            JsonicRouteContext.set(call);
            JsonicMiddlewareContext.begin();
            try {
                trigger.execute(new JsonicRouteEvent(call));
                return JsonicMiddlewareContext.finish();
            } finally {
                JsonicMiddlewareContext.clear();
                JsonicRouteContext.clear();
            }
        };
    }
}

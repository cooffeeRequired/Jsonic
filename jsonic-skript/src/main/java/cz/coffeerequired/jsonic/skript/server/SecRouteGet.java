package cz.coffeerequired.jsonic.skript.server;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.util.Kleenean;
import cz.coffeerequired.jsonic.core.JsonicLogger;
import cz.coffeerequired.jsonic.core.JsonicSettings;
import cz.coffeerequired.jsonic.server.JsonPathUtils;
import cz.coffeerequired.jsonic.server.JsonicApp;
import cz.coffeerequired.jsonic.server.JsonicCall;
import cz.coffeerequired.jsonic.server.MiddlewareType;
import cz.coffeerequired.jsonic.server.JsonicRouteMiddleware;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

import static ch.njol.skript.util.LiteralUtils.canInitSafely;
import static ch.njol.skript.util.LiteralUtils.defendExpression;

@Name("HTTP route section")
@Description({
        "Defines a GET/POST/PUT/PATCH/DELETE route handler.",
        "Body runs per request; use `reply …` to respond."
})
@Since("1.0")
@Examples("""
        post "/login":
            set {_user} to request param "username"
            reply json "{\\"token\\":\\"abc\\"}"
        """)
public class SecRouteGet extends Section {

    private static final String[] METHODS = {"GET", "POST", "PUT", "PATCH", "DELETE"};

    private Expression<String> pathExpr;
    private Trigger routeTrigger;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult,
                        SectionNode sectionNode, List<TriggerItem> triggerItems) {
        pathExpr = defendExpression(expressions[0]);
        if (!canInitSafely(pathExpr)) {
            return false;
        }
        routeTrigger = loadCode(sectionNode, "jsonic route", JsonicRouteEvent.class);
        JsonicApp app = SecJsonicServer.PARSING.get();
        if (app == null) {
            app = JsonicServerRegistry.getDefaultApp();
        }
        String pathLiteral = literalPath(pathExpr);
        if (app == null) {
            if (JsonicSettings.INSTANCE.getDebug()) {
                JsonicLogger.debug("SecRouteGet skipped (no app): %s", METHODS[matchedPattern]);
            }
            return true;
        }
        if (pathLiteral == null) {
            if (JsonicSettings.INSTANCE.getDebug()) {
                JsonicLogger.debug("SecRouteGet skipped (non-literal path): %s %s", METHODS[matchedPattern], pathExpr);
            }
            return true;
        }
        String fullPath = JsonPathUtils.join(RouteGroupContext.pathPrefix(), pathLiteral);
        List<MiddlewareType> middlewares = RouteGroupContext.middlewares();
        List<String> skriptMiddlewareNames = RouteGroupContext.skriptMiddlewareNames();
        List<JsonicRouteMiddleware> skriptMiddlewares = RouteGroupContext.skriptMiddlewares();
        Consumer<JsonicCall> handler = call -> {
            JsonicRouteContext.set(call);
            try {
                routeTrigger.execute(new JsonicRouteEvent(call));
            } finally {
                JsonicRouteContext.clear();
            }
        };
        PendingRouteRegistry.add(app, new PendingRouteRegistry.PendingRoute(
                METHODS[matchedPattern],
                fullPath,
                handler,
                middlewares,
                skriptMiddlewareNames,
                skriptMiddlewares
        ));
        return true;
    }

    @Nullable
    private static String literalPath(Expression<String> expr) {
        if (expr instanceof Literal<?> literal) {
            return (String) literal.getSingle();
        }
        return null;
    }

    @Override
    protected TriggerItem walk(Event event) {
        return getNext();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "jsonic route section";
    }
}

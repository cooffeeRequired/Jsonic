package cz.coffeerequired.jsonic.skript.server;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.util.Kleenean;
import cz.coffeerequired.jsonic.server.JsonicApp;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Name("Jsonic server section")
@Description({
        "Declares an embedded HTTP server (port, optional host) and its routes.",
        "Routes are registered at parse time; call `start jsonic server` to listen."
})
@Since("1.0")
@Examples("""
        jsonic server on port 8080 host "127.0.0.1":
            get "/health":
                reply json "{\\"ok\\":true}"
        """)
public class SecJsonicServer extends Section {

    public static final ThreadLocal<JsonicApp> PARSING = new ThreadLocal<>();

    private Expression<Number> portExpr;
    private Expression<String> hostExpr;
    private JsonicApp app;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult,
                        SectionNode sectionNode, List<TriggerItem> triggerItems) {
        if (matchedPattern == 0) {
            portExpr = (Expression<Number>) expressions[0];
            hostExpr = (Expression<String>) expressions[1];
        } else {
            portExpr = (Expression<Number>) expressions[0];
        }
        app = new JsonicApp();
        PARSING.set(app);
        PendingRouteRegistry.clear(app);
        RouteGroupContext.clear();
        try {
            loadCode(sectionNode);
        } finally {
            PARSING.remove();
            RouteGroupContext.clear();
        }
        JsonicServerRegistry.setDefaultApp(app);
        return portExpr != null;
    }

    @Override
    protected TriggerItem walk(Event event) {
        Number port = portExpr.getSingle(event);
        if (port != null) {
            app.port(port.intValue());
            if (hostExpr != null) {
                String host = hostExpr.getSingle(event);
                if (host != null) {
                    app.host(host);
                }
            }
            JsonicServerRegistry.setDefaultApp(app);
        }
        PendingRouteRegistry.flushTo(app);
        return walk(event, false);
    }

    public JsonicApp getApp() {
        return app;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "jsonic server section";
    }
}

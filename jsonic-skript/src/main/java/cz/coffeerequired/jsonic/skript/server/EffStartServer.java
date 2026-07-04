package cz.coffeerequired.jsonic.skript.server;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.coffeerequired.jsonic.server.JsonicApp;
import cz.coffeerequired.jsonic.server.ServerEngine;
import cz.coffeerequired.jsonic.skript.JsonicAddon;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Start jsonic server")
@Description({
        "Starts the embedded HTTP server with routes from the default or given jsonic server section.",
        "Flushes pending routes registered at parse time."
})
@Since("1.0")
@Examples("""
        on script load:
            start jsonic server
        """)
public class EffStartServer extends Effect {

    private Expression<JsonicApp> appExpr;

    @Override
    protected void execute(Event event) {
        JsonicApp app = appExpr != null ? appExpr.getSingle(event) : JsonicServerRegistry.getDefaultApp();
        if (app == null) return;
        PendingRouteRegistry.flushTo(app);
        ServerEngine.INSTANCE.start(JsonicAddon.getPlugin(), app);
        JsonicServerRegistry.setRunningApp(app);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "start jsonic server";
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (exprs.length > 0 && exprs[0] != null) {
            appExpr = (Expression<JsonicApp>) exprs[0];
        }
        return true;
    }
}

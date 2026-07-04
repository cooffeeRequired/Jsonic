package cz.coffeerequired.jsonic.skript.server;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.coffeerequired.jsonic.server.JsonicCall;
import cz.coffeerequired.jsonic.skript.JsonicAddon;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import static ch.njol.skript.util.LiteralUtils.canInitSafely;
import static ch.njol.skript.util.LiteralUtils.defendExpression;

@Name("Reply with file")
@Description({
        "Sends a file from the plugin data folder as the HTTP response.",
        "Content-Type is inferred from the file extension."
})
@Since("1.0")
@Examples("""
        get "/":
            reply file "web/index.html"
        get "/missing":
            reply file "web/404.html" with status 404
        """)
public class EffReplyFile extends Effect {

    private Expression<String> pathExpr;
    private Expression<Number> statusExpr;

    @Override
    protected void execute(Event event) {
        JsonicCall call = JsonicRouteContext.get();
        if (call == null) return;
        String path = pathExpr.getSingle(event);
        if (path == null) return;
        if (statusExpr != null) {
            Number status = statusExpr.getSingle(event);
            if (status != null) {
                call.queueStatus(status.intValue());
            }
        }
        JavaPlugin plugin = JsonicAddon.getPlugin();
        if (plugin == null) return;
        call.queueFile(path, plugin.getDataFolder());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "reply file " + pathExpr.toString(event, debug);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        pathExpr = defendExpression(exprs[0]);
        if (matchedPattern == 1 && exprs.length > 1) {
            statusExpr = defendExpression(exprs[1]);
            return canInitSafely(pathExpr) && canInitSafely(statusExpr);
        }
        return canInitSafely(pathExpr);
    }
}

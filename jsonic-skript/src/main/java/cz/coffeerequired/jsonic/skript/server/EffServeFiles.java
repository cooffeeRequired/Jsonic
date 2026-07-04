package cz.coffeerequired.jsonic.skript.server;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import cz.coffeerequired.jsonic.server.JsonPathUtils;
import cz.coffeerequired.jsonic.server.JsonicApp;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import static ch.njol.skript.util.LiteralUtils.canInitSafely;
import static ch.njol.skript.util.LiteralUtils.defendExpression;

@Name("Serve static files")
@Description({
        "Mounts a folder from the plugin data directory at a URL prefix.",
        "Parse-time effect inside a jsonic server section."
})
@Since("1.0")
@Examples("""
        jsonic server on port 8080:
            serve files from "web" at "/"
        """)
public class EffServeFiles extends Effect {

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        Expression<String> folderExpr;
        Expression<String> urlExpr;
        if (matchedPattern == 0) {
            folderExpr = defendExpression(expressions[0]);
            urlExpr = defendExpression(expressions[1]);
        } else {
            urlExpr = defendExpression(expressions[0]);
            folderExpr = defendExpression(expressions[1]);
        }
        if (!canInitSafely(folderExpr) || !canInitSafely(urlExpr)) {
            return false;
        }
        JsonicApp app = SecJsonicServer.PARSING.get();
        if (app == null) {
            app = JsonicServerRegistry.getDefaultApp();
        }
        String folder = literalString(folderExpr);
        String url = literalString(urlExpr);
        if (app != null && folder != null && url != null) {
            String mountUrl = JsonPathUtils.join(RouteGroupContext.pathPrefix(), url);
            app.registerStaticMount(mountUrl, folder);
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        // Registered at parse time inside jsonic server section.
    }

    @Nullable
    private static String literalString(Expression<String> expr) {
        if (expr instanceof Literal<?> literal) {
            return (String) literal.getSingle();
        }
        return null;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "serve static files";
    }
}

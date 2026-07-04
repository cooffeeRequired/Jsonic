package cz.coffeerequired.jsonic.skript.server;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import static ch.njol.skript.util.LiteralUtils.canInitSafely;
import static ch.njol.skript.util.LiteralUtils.defendExpression;

@Name("Route or query param")
@Description({
        "Returns a path parameter or query string value from the current HTTP request.",
        "Only valid inside a jsonic route handler."
})
@Since("1.0")
@Examples("""
        post "/login":
            set {_user} to request param "username"
            set {_id} to route param "id"
        """)
public class ExprRouteParam extends SimpleExpression<String> {

    private Expression<String> nameExpr;

    @Override
    protected String[] get(Event event) {
        String name = nameExpr.getSingle(event);
        if (name == null) return new String[0];
        var call = JsonicRouteContext.get();
        if (call == null) return new String[0];
        String value = call.param(name);
        return value == null ? new String[0] : new String[]{value};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "route param " + nameExpr.toString(event, debug);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        nameExpr = defendExpression(exprs[0]);
        return canInitSafely(nameExpr);
    }
}

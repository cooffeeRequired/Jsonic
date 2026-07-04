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

@Name("Bearer token")
@Description({
        "Returns the token from the `Authorization: Bearer …` header.",
        "Empty if the header is missing or not a Bearer token."
})
@Since("1.0")
@Examples("""
        define middleware "requireAuth":
            if bearer token is "":
                stop middleware
            continue middleware
        """)
public class ExprBearerToken extends SimpleExpression<String> {

    @Override
    protected String[] get(Event event) {
        var call = JsonicRouteContext.get();
        if (call == null) {
            return new String[0];
        }
        String auth = call.header("Authorization");
        if (auth == null) {
            return new String[0];
        }
        if (auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = auth.substring(7).trim();
            return token.isEmpty() ? new String[0] : new String[]{token};
        }
        return new String[0];
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
        return "bearer token";
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }
}

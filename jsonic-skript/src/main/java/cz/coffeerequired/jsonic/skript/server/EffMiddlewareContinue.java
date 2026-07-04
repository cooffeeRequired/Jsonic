package cz.coffeerequired.jsonic.skript.server;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import static ch.njol.skript.util.LiteralUtils.canInitSafely;
import static ch.njol.skript.util.LiteralUtils.defendExpression;

@Name("Continue or stop middleware")
@Description({
        "Controls the middleware chain inside middleware sections.",
        "`continue middleware` proceeds; `stop middleware` aborts the chain."
})
@Since("1.0")
@Examples("""
        define middleware "auth":
            if bearer token is "":
                reply with status 401
                stop middleware
            continue middleware
        """)
public class EffMiddlewareContinue extends Effect {

    private Expression<Boolean> boolExpr;
    private boolean continueChain = true;
    private boolean useExpression;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        if (matchedPattern == 2) {
            continueChain = false;
            useExpression = false;
            return true;
        }
        if (matchedPattern == 1) {
            boolExpr = defendExpression(expressions[0]);
            useExpression = true;
            return canInitSafely(boolExpr);
        }
        useExpression = false;
        continueChain = true;
        return true;
    }

    @Override
    protected void execute(Event event) {
        if (useExpression) {
            Boolean value = boolExpr.getSingle(event);
            JsonicMiddlewareContext.setContinue(value == null || value);
        } else {
            JsonicMiddlewareContext.setContinue(continueChain);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "continue middleware";
    }
}

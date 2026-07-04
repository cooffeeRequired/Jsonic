package cz.coffeerequired.jsonic.skript.server;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.util.Kleenean;
import cz.coffeerequired.jsonic.server.MiddlewareType;
import cz.coffeerequired.jsonic.server.MiddlewareTypes;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import static ch.njol.skript.util.LiteralUtils.canInitSafely;
import static ch.njol.skript.util.LiteralUtils.defendExpression;

@Name("Apply middleware")
@Description({
        "Applies a built-in middleware (cors, json, logger, auth) or a defined middleware by name.",
        "Parse-time effect inside a jsonic server or route group section."
})
@Since("1.0")
@Examples("""
        group "/api":
            middleware "json"
            middleware "requireAuth"
        """)
public class EffApplyMiddleware extends Effect {

    private Expression<String> nameExpr;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        nameExpr = defendExpression(expressions[0]);
        if (!canInitSafely(nameExpr)) {
            return false;
        }
        String name = literalString(nameExpr);
        if (name == null) {
            return false;
        }
        return applyNamed(name);
    }

    static boolean applyNamed(String name) {
        MiddlewareType builtIn = MiddlewareTypes.fromName(name);
        if (builtIn != null) {
            RouteGroupContext.addBuiltIn(builtIn);
            return true;
        }
        Trigger defined = MiddlewareDefinitionRegistry.get(name);
        if (defined != null) {
            RouteGroupContext.addSkript(JsonicMiddlewareRunner.fromTrigger(defined));
            return true;
        }
        RouteGroupContext.addSkriptMiddlewareName(name);
        return true;
    }

    @Nullable
    private static String literalString(Expression<String> expr) {
        if (expr instanceof Literal<?> literal) {
            return (String) literal.getSingle();
        }
        return null;
    }

    @Override
    protected void execute(Event event) {
        // Registered at parse time inside jsonic server / route group.
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "apply jsonic middleware";
    }
}

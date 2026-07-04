package cz.coffeerequired.jsonic.skript.json;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.google.gson.JsonElement;
import cz.coffeerequired.jsonic.core.JsonBuilder;
import cz.coffeerequired.jsonic.core.JsonEngine;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import static ch.njol.skript.util.LiteralUtils.canInitSafely;
import static ch.njol.skript.util.LiteralUtils.defendExpression;

@Name("Set json builder path")
@Description({
        "Sets a value on a JsonBuilder at the given path.",
        "Intermediate objects/arrays are created automatically."
})
@Since("1.0")
@Examples("""
        set {_b} to jsonic builder
        set path "meta.version" in {_b} to 2
        set {_b}'s path "items.0" to "apple"
        """)
public class EffSetJsonPath extends Effect {

    private Expression<JsonBuilder> builderExpr;
    private Expression<String> pathExpr;
    private Expression<?> valueExpr;

    @Override
    protected void execute(Event event) {
        JsonBuilder builder = builderExpr.getSingle(event);
        String path = pathExpr.getSingle(event);
        Object value = valueExpr.getSingle(event);
        if (builder == null || path == null) return;
        builder.path(path).set(value);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "set path in builder";
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (matchedPattern == 0) {
            builderExpr = defendExpression(exprs[0]);
            pathExpr = defendExpression(exprs[1]);
            valueExpr = defendExpression(exprs[2]);
        } else {
            pathExpr = defendExpression(exprs[0]);
            builderExpr = defendExpression(exprs[1]);
            valueExpr = defendExpression(exprs[2]);
        }
        return canInitSafely(builderExpr) && canInitSafely(pathExpr) && canInitSafely(valueExpr);
    }
}

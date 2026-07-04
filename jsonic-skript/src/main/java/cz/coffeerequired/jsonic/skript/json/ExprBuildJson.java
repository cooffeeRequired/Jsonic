package cz.coffeerequired.jsonic.skript.json;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.google.gson.JsonElement;
import cz.coffeerequired.jsonic.core.JsonBuilder;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Build json from builder")
@Description("Materializes a JsonBuilder into a JsonElement.")
@Since("1.0")
@Examples("set {_json} to build json from {_builder}")
public class ExprBuildJson extends SimpleExpression<JsonElement> {

    private Expression<JsonBuilder> builderExpr;

    @Override
    protected JsonElement[] get(Event event) {
        JsonBuilder builder = builderExpr.getSingle(event);
        if (builder == null) return new JsonElement[0];
        return new JsonElement[]{builder.build()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends JsonElement> getReturnType() {
        return JsonElement.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "build json from " + builderExpr.toString(event, debug);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        builderExpr = (Expression<JsonBuilder>) exprs[0];
        return builderExpr != null;
    }
}

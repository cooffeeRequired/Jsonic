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
import cz.coffeerequired.jsonic.core.PathEngine;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import static ch.njol.skript.util.LiteralUtils.canInitSafely;
import static ch.njol.skript.util.LiteralUtils.defendExpression;

@Name("Json path value")
@Description({
        "Returns the value at a dot-delimited JSON path.",
        "Aliases: `path \"a.b\" of {_json}` and `value at path \"a.b\" in {_json}`."
})
@Since("1.0")
@Examples("""
        set {_name} to path "users.admin.name" of {_json}
        set {_role} to value at path "users.%{_user}%.role" in {_json}
        """)
public class ExprJsonPath extends SimpleExpression<Object> {

    private Expression<String> pathExpr;
    private Expression<JsonElement> jsonExpr;

    @Override
    protected Object[] get(Event event) {
        String path = pathExpr.getSingle(event);
        JsonElement json = jsonExpr.getSingle(event);
        if (path == null || json == null) return new Object[0];
        JsonElement value = PathEngine.INSTANCE.get(json, path);
        if (value == null) return new Object[0];
        return new Object[]{cz.coffeerequired.jsonic.core.JsonEngine.INSTANCE.fromJson(value)};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "path " + pathExpr.toString(event, debug) + " of " + jsonExpr.toString(event, debug);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        pathExpr = defendExpression(exprs[0]);
        jsonExpr = defendExpression(exprs[1]);
        return canInitSafely(pathExpr) && canInitSafely(jsonExpr);
    }
}

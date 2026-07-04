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
import cz.coffeerequired.jsonic.core.JsonEngine;
import cz.coffeerequired.jsonic.core.PathEngine;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import static ch.njol.skript.util.LiteralUtils.canInitSafely;
import static ch.njol.skript.util.LiteralUtils.defendExpression;

@Name("Set json path in-place")
@Description({
        "Writes a value directly into an existing JsonElement tree (in-place).",
        "Use JsonBuilder syntax for building new JSON; this modifies loaded/cached JSON."
})
@Since("1.0")
@Examples("""
        set path "users.%{_login}%.lastSeen" in {_json} to now
        set path "settings.theme" in webDb() to "dark"
        """)
public class EffSetJsonPathInJson extends Effect {

    private Expression<String> pathExpr;
    private Expression<JsonElement> jsonExpr;
    private Expression<?> valueExpr;

    @Override
    protected void execute(Event event) {
        String path = pathExpr.getSingle(event);
        JsonElement json = jsonExpr.getSingle(event);
        Object value = valueExpr.getSingle(event);
        if (path == null || json == null) return;
        PathEngine.INSTANCE.set(json, path, JsonEngine.INSTANCE.toJson(value));
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "set path in json";
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        pathExpr = defendExpression(exprs[0]);
        jsonExpr = defendExpression(exprs[1]);
        valueExpr = defendExpression(exprs[2]);
        return canInitSafely(pathExpr) && canInitSafely(jsonExpr) && canInitSafely(valueExpr);
    }
}

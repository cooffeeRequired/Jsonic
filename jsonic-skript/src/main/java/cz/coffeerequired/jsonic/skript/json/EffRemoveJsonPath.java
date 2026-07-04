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
import cz.coffeerequired.jsonic.core.PathEngine;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import static ch.njol.skript.util.LiteralUtils.canInitSafely;
import static ch.njol.skript.util.LiteralUtils.defendExpression;

@Name("Remove json path")
@Description("Removes a key or array index at the given path from an existing JsonElement.")
@Since("1.0")
@Examples("""
        remove path "temp" from {_json}
        delete path "users.%{_name}%" in {_json}
        """)
public class EffRemoveJsonPath extends Effect {

    private Expression<String> pathExpr;
    private Expression<JsonElement> jsonExpr;

    @Override
    protected void execute(Event event) {
        String path = pathExpr.getSingle(event);
        JsonElement json = jsonExpr.getSingle(event);
        if (path == null || json == null) return;
        PathEngine.INSTANCE.remove(json, path);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "remove path from json";
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        pathExpr = defendExpression(exprs[0]);
        jsonExpr = defendExpression(exprs[1]);
        return canInitSafely(pathExpr) && canInitSafely(jsonExpr);
    }
}

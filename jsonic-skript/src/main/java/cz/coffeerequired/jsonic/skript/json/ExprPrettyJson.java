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
import cz.coffeerequired.jsonic.core.JsonEngine;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Pretty printed json")
@Description("Returns a formatted JSON string (Gson pretty print).")
@Since("1.0")
@Examples("send {_json} as pretty printed json")
public class ExprPrettyJson extends SimpleExpression<String> {

    private Expression<JsonElement> jsonExpr;

    @Override
    protected String[] get(Event event) {
        JsonElement json = jsonExpr.getSingle(event);
        if (json == null) return new String[0];
        return new String[]{JsonEngine.INSTANCE.gson().toJson(json)};
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
        return jsonExpr.toString(event, debug) + " as pretty json";
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        jsonExpr = (Expression<JsonElement>) exprs[0];
        return jsonExpr != null;
    }
}

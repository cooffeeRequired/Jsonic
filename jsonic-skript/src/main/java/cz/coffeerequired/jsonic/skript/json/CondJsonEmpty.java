package cz.coffeerequired.jsonic.skript.json;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Json is empty")
@Description("Checks whether a JSON object or array has zero entries.")
@Since("1.0")
@Examples("""
        if {_json} is empty json:
            send "no data"
        if {_json} is not empty json:
            send "has content"
        """)
public class CondJsonEmpty extends Condition {

    private Expression<JsonElement> jsonExpr;
    private boolean negated;

    @Override
    public boolean check(Event event) {
        JsonElement json = jsonExpr.getSingle(event);
        if (json == null) return negated;
        boolean empty = (json.isJsonObject() && json.getAsJsonObject().size() == 0)
                || (json.isJsonArray() && json.getAsJsonArray().size() == 0);
        return negated != empty;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return jsonExpr.toString(event, debug) + (negated ? " is not empty json" : " is empty json");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        jsonExpr = (Expression<JsonElement>) exprs[0];
        negated = matchedPattern == 1;
        return jsonExpr != null;
    }
}

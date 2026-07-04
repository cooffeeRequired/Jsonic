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
import cz.coffeerequired.jsonic.core.JsonFiles;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import static ch.njol.skript.util.LiteralUtils.canInitSafely;
import static ch.njol.skript.util.LiteralUtils.defendExpression;

@Name("Json from source")
@Description({
        "Parses JSON from a string/object or reads a JSON file from the plugin data folder.",
        "Use `parse … as json` for inline strings; `json from file …` for files."
})
@Since("1.0")
@Examples("""
        set {_data} to parse "{\\"ok\\": true}" as json
        set {_data} to json from file "web/data.jsonc"
        """)
public class ExprJsonFrom extends SimpleExpression<JsonElement> {

    private Expression<?> source;
    private Mode mode;

    private enum Mode { ANY, FILE }

    @Override
    protected JsonElement[] get(Event event) {
        if (mode == Mode.FILE) {
            String path = ((Expression<String>) source).getSingle(event);
            if (path == null) return new JsonElement[0];
            try {
                return new JsonElement[]{JsonFiles.INSTANCE.readAsync(path).join()};
            } catch (Exception e) {
                return new JsonElement[0];
            }
        }
        Object[] values = source.getArray(event);
        if (values == null) return new JsonElement[0];
        JsonElement[] out = new JsonElement[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = JsonEngine.INSTANCE.toJson(values[i]);
        }
        return out;
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
        return "json from " + source.toString(event, debug);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (matchedPattern <= 1) {
            mode = Mode.FILE;
            source = defendExpression(exprs[0]);
            return canInitSafely((Expression<String>) source);
        }
        mode = Mode.ANY;
        source = defendExpression(exprs[0]);
        return canInitSafely(source);
    }
}

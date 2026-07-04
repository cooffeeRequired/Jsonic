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
import cz.coffeerequired.jsonic.core.JsonFiles;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Save json to file")
@Description({
        "Writes a JsonElement to a file under the plugin data folder.",
        "Creates parent directories when needed."
})
@Since("1.0")
@Examples("save webDb() to json file \"web/data.jsonc\"")
public class EffSaveJsonFile extends Effect {

    private Expression<JsonElement> jsonExpr;
    private Expression<String> pathExpr;

    @Override
    protected void execute(Event event) {
        JsonElement json = jsonExpr.getSingle(event);
        String path = pathExpr.getSingle(event);
        if (json == null || path == null) return;
        JsonFiles.INSTANCE.writeAsync(path, json, true).join();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "save json to file";
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        jsonExpr = (Expression<JsonElement>) exprs[0];
        pathExpr = (Expression<String>) exprs[1];
        return jsonExpr != null && pathExpr != null;
    }
}

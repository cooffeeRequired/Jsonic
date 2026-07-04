package cz.coffeerequired.jsonic.skript.storage;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.google.gson.JsonElement;
import cz.coffeerequired.jsonic.core.storage.StorageEngine;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Json storage cache")
@Description({
        "Returns the in-memory JSON bound to a cache id.",
        "Bind a file first with `bind json file … as …`."
})
@Since("1.0")
@Examples("""
        bind json file "web/data.jsonc" as "webdb"
        set {_json} to json cache "webdb"
        """)
public class ExprStorageJson extends SimpleExpression<JsonElement> {

    private Expression<String> idExpr;

    @Override
    protected JsonElement[] get(Event event) {
        String id = idExpr.getSingle(event);
        if (id == null) return new JsonElement[0];
        var entry = StorageEngine.INSTANCE.getOrCreate(id);
        return new JsonElement[]{entry.getJson()};
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
        return "json cache " + idExpr.toString(event, debug);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        idExpr = (Expression<String>) exprs[0];
        return idExpr != null;
    }
}

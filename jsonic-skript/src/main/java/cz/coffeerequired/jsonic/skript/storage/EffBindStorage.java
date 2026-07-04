package cz.coffeerequired.jsonic.skript.storage;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.coffeerequired.jsonic.core.storage.StorageEngine;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Bind json file to cache")
@Description({
        "Loads a JSON file into memory under a cache id.",
        "Optional `and watch` keeps the cache synced when the file changes on disk."
})
@Since("1.0")
@Examples("""
        bind json file "web/data.jsonc" as "webdb"
        bind json file "web/data.jsonc" to json cache "webdb" and watch storage watcher
        """)
public class EffBindStorage extends Effect {

    private Expression<String> fileExpr;
    private Expression<String> idExpr;
    private boolean watch;

    @Override
    protected void execute(Event event) {
        String file = fileExpr.getSingle(event);
        String id = idExpr.getSingle(event);
        if (file == null || id == null) return;
        StorageEngine.INSTANCE.bindFile(id, file, watch);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "bind json storage";
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        fileExpr = (Expression<String>) exprs[0];
        idExpr = (Expression<String>) exprs[1];
        watch = matchedPattern == 2 || parseResult.hasTag("let") || parseResult.hasTag("watch");
        return fileExpr != null && idExpr != null;
    }
}

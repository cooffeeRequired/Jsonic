package cz.coffeerequired.jsonic.skript.storage;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.coffeerequired.jsonic.core.storage.StorageEngine;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Json cache exists")
@Description("Checks whether a JSON storage cache id is registered.")
@Since("1.0")
@Examples("""
        if json cache "webdb" exists:
            save {_json} to json file "web/data.jsonc"
        """)
public class CondStorageExists extends Condition {

    private Expression<String> idExpr;
    private boolean negated;

    @Override
    public boolean check(Event event) {
        String id = idExpr.getSingle(event);
        if (id == null) return negated;
        boolean exists = StorageEngine.INSTANCE.exists(id);
        return negated != exists;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "json cache " + idExpr.toString(event, debug) + (negated ? " does not exist" : " exists");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        idExpr = (Expression<String>) exprs[0];
        negated = matchedPattern == 1;
        return idExpr != null;
    }
}

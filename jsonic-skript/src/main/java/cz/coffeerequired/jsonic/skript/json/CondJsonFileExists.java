package cz.coffeerequired.jsonic.skript.json;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.coffeerequired.jsonic.core.JsonFiles;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Json file exists")
@Description("Checks whether a JSON file exists in the plugin data folder.")
@Since("1.0")
@Examples("""
        if json file "web/data.jsonc" exists:
            webDbLoad()
        """)
public class CondJsonFileExists extends Condition {

    private Expression<String> pathExpr;
    private boolean negated;

    @Override
    public boolean check(Event event) {
        String path = pathExpr.getSingle(event);
        if (path == null) return negated;
        boolean exists = JsonFiles.INSTANCE.exists(path);
        return negated != exists;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "json file " + pathExpr.toString(event, debug) + (negated ? " does not exist" : " exists");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        pathExpr = (Expression<String>) exprs[0];
        negated = matchedPattern == 1;
        return pathExpr != null;
    }
}

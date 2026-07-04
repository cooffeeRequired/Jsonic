package cz.coffeerequired.jsonic.skript.json;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import cz.coffeerequired.jsonic.core.JsonBuilder;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Jsonic json builder")
@Description("Creates a mutable JsonBuilder for incremental JSON construction.")
@Since("1.0")
@Examples("""
        set {_b} to jsonic builder
        set {_b}'s path "user.name" to "Alex"
        set {_json} to build json from {_b}
        """)
public class ExprJsonBuilder extends SimpleExpression<JsonBuilder> {

    @Override
    protected JsonBuilder[] get(Event event) {
        return new JsonBuilder[]{new JsonBuilder()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends JsonBuilder> getReturnType() {
        return JsonBuilder.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "jsonic builder";
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }
}

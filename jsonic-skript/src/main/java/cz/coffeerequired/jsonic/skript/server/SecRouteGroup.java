package cz.coffeerequired.jsonic.skript.server;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static ch.njol.skript.util.LiteralUtils.canInitSafely;
import static ch.njol.skript.util.LiteralUtils.defendExpression;

@Name("Route group")
@Description({
        "Groups routes under a URL prefix.",
        "Aliases: `group \"/api\":` and `resource \"/api\":`."
})
@Since("1.0")
@Examples("""
        jsonic server on port 8080:
            group "/api":
                get "/users":
                    reply json "{\\"users\\":[]}"
        """)
public class SecRouteGroup extends Section {

    private Expression<String> prefixExpr;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult,
                        SectionNode sectionNode, List<TriggerItem> triggerItems) {
        prefixExpr = defendExpression(expressions[0]);
        if (!canInitSafely(prefixExpr)) {
            return false;
        }
        String prefix = literalString(prefixExpr);
        if (prefix == null) {
            return false;
        }
        RouteGroupContext.enterGroup(prefix);
        loadCode(sectionNode);
        RouteGroupContext.leaveGroup();
        return true;
    }

    @Nullable
    private static String literalString(Expression<String> expr) {
        if (expr instanceof Literal<?> literal) {
            return (String) literal.getSingle();
        }
        return null;
    }

    @Override
    protected TriggerItem walk(Event event) {
        return getNext();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "jsonic route group";
    }
}

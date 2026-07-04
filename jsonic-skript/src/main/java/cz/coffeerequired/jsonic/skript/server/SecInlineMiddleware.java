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
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static ch.njol.skript.util.LiteralUtils.canInitSafely;
import static ch.njol.skript.util.LiteralUtils.defendExpression;

@Name("Inline middleware section")
@Description({
        "Applies middleware to routes in the current group.",
        "Empty body (`middleware \"name\":` with no lines) applies a named/built-in middleware.",
        "With a body, runs Skript code per request before the route handler."
})
@Since("1.0")
@Examples("""
        group "/api":
            middleware "json"
            middleware "requireAuth":
                if bearer token is "":
                    stop middleware
                continue middleware
        """)
public class SecInlineMiddleware extends Section {

    private Expression<String> nameExpr;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult,
                        SectionNode sectionNode, List<TriggerItem> triggerItems) {
        nameExpr = defendExpression(expressions[0]);
        if (!canInitSafely(nameExpr)) {
            return false;
        }
        String name = literalString(nameExpr);
        if (name == null) {
            return false;
        }
        if (!sectionNode.iterator().hasNext()) {
            return EffApplyMiddleware.applyNamed(name);
        }
        Trigger trigger = loadCode(sectionNode, "jsonic middleware " + name, JsonicRouteEvent.class);
        RouteGroupContext.addSkript(JsonicMiddlewareRunner.fromTrigger(trigger));
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
        return "jsonic inline middleware";
    }
}

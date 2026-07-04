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
import cz.coffeerequired.jsonic.server.JsonicApp;
import cz.coffeerequired.jsonic.server.JsonicCall;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

import static ch.njol.skript.util.LiteralUtils.canInitSafely;
import static ch.njol.skript.util.LiteralUtils.defendExpression;

@Name("Jsonic error handler")
@Description({
        "Registers a custom handler for an HTTP error status (e.g. 404, 500).",
        "Runs when a route leaves no body or sets a non-2xx status without a reply."
})
@Since("1.0")
@Examples("""
        jsonic server on port 8080:
            error "404":
                reply json "{\\"error\\":\\"not_found\\"}"
                reply with status 404
        """)
public class SecJsonicError extends Section {

    private Expression<String> codeExpr;
    private Trigger errorTrigger;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult,
                        SectionNode sectionNode, List<TriggerItem> triggerItems) {
        codeExpr = defendExpression(expressions[0]);
        if (!canInitSafely(codeExpr)) {
            return false;
        }
        errorTrigger = loadCode(sectionNode, "jsonic error", JsonicRouteEvent.class);
        JsonicApp app = SecJsonicServer.PARSING.get();
        if (app == null) {
            app = JsonicServerRegistry.getDefaultApp();
        }
        Integer statusCode = literalStatus(codeExpr);
        if (app != null && statusCode != null) {
            Consumer<JsonicCall> handler = call -> {
                JsonicRouteContext.set(call);
                try {
                    errorTrigger.execute(new JsonicRouteEvent(call));
                } finally {
                    JsonicRouteContext.clear();
                }
            };
            app.registerError(statusCode, handler);
        }
        return true;
    }

    @Nullable
    private static Integer literalStatus(Expression<String> expr) {
        if (!(expr instanceof Literal<?> literal)) {
            return null;
        }
        String raw = (String) literal.getSingle();
        if (raw == null) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    protected TriggerItem walk(Event event) {
        return getNext();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "jsonic error section";
    }
}

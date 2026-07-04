package cz.coffeerequired.jsonic.skript.server;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.google.gson.JsonElement;
import cz.coffeerequired.jsonic.core.JsonEngine;
import cz.coffeerequired.jsonic.server.JsonicCall;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import static ch.njol.skript.util.LiteralUtils.canInitSafely;
import static ch.njol.skript.util.LiteralUtils.defendExpression;

@Name("Reply to HTTP request")
@Description({
        "Queues a response inside a route or error handler.",
        "`reply json …` sends JSON; `reply …` sends plain text; `reply with status …` sets status only."
})
@Since("1.0")
@Examples("""
        get "/ping":
            reply json "{\\"pong\\":true}"
        post "/bad":
            reply with status 400
        """)
public class EffReplyJson extends Effect {

    private Expression<?> payloadExpr;
    private Expression<Number> statusExpr;
    private Mode mode = Mode.JSON;

    private enum Mode { JSON, TEXT, STATUS }

    @Override
    protected void execute(Event event) {
        JsonicCall call = JsonicRouteContext.get();
        if (call == null) return;
        switch (mode) {
            case JSON -> {
                Object val = payloadExpr.getSingle(event);
                JsonElement json = val instanceof JsonElement je ? je : JsonEngine.INSTANCE.toJson(val);
                call.queueJson(json);
            }
            case TEXT -> call.queueText(String.valueOf(payloadExpr.getSingle(event)));
            case STATUS -> {
                Number status = statusExpr.getSingle(event);
                if (status != null) call.queueStatus(status.intValue());
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "reply";
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (matchedPattern == 0) {
            mode = Mode.JSON;
            payloadExpr = defendExpression(exprs[0]);
            return canInitSafely(payloadExpr);
        }
        if (matchedPattern == 1) {
            mode = Mode.TEXT;
            payloadExpr = defendExpression(exprs[0]);
            return canInitSafely(payloadExpr);
        }
        mode = Mode.STATUS;
        statusExpr = defendExpression(exprs[0]);
        return canInitSafely(statusExpr);
    }
}

package cz.coffeerequired.jsonic.skript.http;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.coffeerequired.jsonic.core.http.HttpEngine;
import cz.coffeerequired.jsonic.core.http.HttpMethod;
import cz.coffeerequired.jsonic.core.http.HttpRequest;
import cz.coffeerequired.jsonic.core.http.RawHttpResult;
import cz.coffeerequired.jsonic.skript.JsonicAddon;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

@Name("Execute jsonic request")
@Description({
        "Sends a prepared jsonic request.",
        "Default: blocking on main thread (async HTTP worker).",
        "Use `as non blocking` for async with `on http response received` event."
})
@Since("1.0")
@Examples("""
        execute {_req}
        send {_req} as non blocking

        on http response received:
            send event-response's body
        """)
public class EffExecuteHttp extends Effect {

    private Expression<HttpRequest> requestExpr;
    private boolean async;

    @Override
    protected void execute(Event event) {
        HttpRequest request = requestExpr.getSingle(event);
        if (request == null) return;

        if (async) {
            JsonicHttpResponseEvent httpEvent = new JsonicHttpResponseEvent();
            request.setEvent(httpEvent);
            HttpEngine.INSTANCE.sendAsync(request, (result, error) ->
                    Bukkit.getScheduler().runTask(plugin(), () -> {
                        HttpEngine.INSTANCE.applyResponse(request, result, error);
                        if (result != null) {
                            httpEvent.setResponse(request.getResponse());
                        }
                        httpEvent.callEvent();
                    }));
        } else if (request.getMethod() == HttpMethod.MOCK) {
            RawHttpResult result = HttpEngine.INSTANCE.sendBlocking(request);
            HttpEngine.INSTANCE.applyResponse(request, result, null);
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin(), () -> {
                RawHttpResult result = HttpEngine.INSTANCE.sendBlocking(request);
                Bukkit.getScheduler().runTask(plugin(), () ->
                        HttpEngine.INSTANCE.applyResponse(request, result, null));
            });
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "execute " + requestExpr.toString(event, debug);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        requestExpr = (Expression<HttpRequest>) exprs[0];
        async = parseResult.hasTag("non") || parseResult.hasTag("not");
        return requestExpr != null;
    }

    private static JavaPlugin plugin() {
        return JsonicAddon.getPlugin();
    }
}

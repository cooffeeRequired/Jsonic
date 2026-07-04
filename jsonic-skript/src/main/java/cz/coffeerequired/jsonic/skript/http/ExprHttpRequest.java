package cz.coffeerequired.jsonic.skript.http;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import cz.coffeerequired.jsonic.core.http.HttpEngine;
import cz.coffeerequired.jsonic.core.http.HttpMethod;
import cz.coffeerequired.jsonic.core.http.HttpRequest;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import static ch.njol.skript.util.LiteralUtils.canInitSafely;
import static ch.njol.skript.util.LiteralUtils.defendExpression;

@Name("Jsonic HTTP request")
@Description({
        "Prepares an outbound HTTP request (GET, POST, PUT, PATCH, DELETE, …).",
        "Configure headers/body on the request, then `execute` or `send` it."
})
@Since("1.0")
@Examples("""
        set {_req} to jsonic GET request to "https://httpbin.org/get"
        set {_req} to prepare POST request on "https://httpbin.org/post"
        """)
public class ExprHttpRequest extends SimpleExpression<HttpRequest> {

    private HttpMethod method;
    private Expression<String> urlExpr;

    @Override
    protected HttpRequest[] get(Event event) {
        String url = urlExpr.getSingle(event);
        if (method == null || url == null) return new HttpRequest[0];
        return new HttpRequest[]{HttpEngine.INSTANCE.createRequest(url, method)};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends HttpRequest> getReturnType() {
        return HttpRequest.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "jsonic " + method + " request to " + urlExpr.toString(event, debug);
    }

    private static final HttpMethod[] METHODS = HttpMethod.values();

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        method = METHODS[matchedPattern % METHODS.length];
        urlExpr = defendExpression(exprs[0]);
        return canInitSafely(urlExpr);
    }
}

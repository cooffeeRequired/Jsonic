package cz.coffeerequired.jsonic.skript.http;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import cz.coffeerequired.jsonic.core.http.HttpRequest;
import cz.coffeerequired.jsonic.core.http.HttpResponseData;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Last response of request")
@Description("Returns the response stored on a request after a blocking execute/send.")
@Since("1.0")
@Examples("""
        execute {_req}
        set {_res} to last response of {_req}
        """)
public class ExprLastResponse extends SimpleExpression<HttpResponseData> {

    private Expression<HttpRequest> requestExpr;

    @Override
    protected HttpResponseData[] get(Event event) {
        HttpRequest request = requestExpr.getSingle(event);
        if (request == null) return new HttpResponseData[0];
        return new HttpResponseData[]{request.getResponse()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends HttpResponseData> getReturnType() {
        return HttpResponseData.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "last response of " + requestExpr.toString(event, debug);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        requestExpr = (Expression<HttpRequest>) exprs[0];
        return requestExpr != null;
    }
}

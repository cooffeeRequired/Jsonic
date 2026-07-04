package cz.coffeerequired.jsonic.skript.http;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import cz.coffeerequired.jsonic.core.http.HttpResponseData;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Response body")
@Description("Returns the raw response body string.")
@Since("1.0")
@Examples("send body of {_response}")
public class ExprResponseBody extends SimpleExpression<String> {

    private Expression<HttpResponseData> responseExpr;

    @Override
    protected String[] get(Event event) {
        HttpResponseData response = responseExpr.getSingle(event);
        if (response == null) return new String[0];
        return new String[]{response.getBody()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "body of " + responseExpr.toString(event, debug);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        responseExpr = (Expression<HttpResponseData>) exprs[0];
        return responseExpr != null;
    }
}

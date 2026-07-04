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

@Name("Response status code")
@Description("Returns the HTTP status code (100–599) from a jsonic response.")
@Since("1.0")
@Examples("""
        if status code of {_response} is 200:
            send "OK"
        """)
public class ExprResponseStatusCode extends SimpleExpression<Number> {

    private Expression<HttpResponseData> responseExpr;

    @Override
    protected Number[] get(Event event) {
        HttpResponseData response = responseExpr.getSingle(event);
        if (response == null) return new Number[0];
        return new Number[]{response.getStatusCode()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "status code of " + responseExpr.toString(event, debug);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        responseExpr = (Expression<HttpResponseData>) exprs[0];
        return responseExpr != null;
    }
}

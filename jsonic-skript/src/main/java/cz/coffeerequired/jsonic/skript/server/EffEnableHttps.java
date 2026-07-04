package cz.coffeerequired.jsonic.skript.server;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import cz.coffeerequired.jsonic.server.JsonicApp;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import static ch.njol.skript.util.LiteralUtils.canInitSafely;
import static ch.njol.skript.util.LiteralUtils.defendExpression;

@Name("Enable HTTPS")
@Description({
        "Enables TLS for the jsonic server using PEM cert/key files in the plugin data folder.",
        "Parse-time effect inside a jsonic server section."
})
@Since("1.0")
@Examples("""
        jsonic server on port 8080:
            enable https on port 8443 cert "certs/server.pem" key "certs/server.key"
        """)
public class EffEnableHttps extends Effect {

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        Expression<Number> portExpr = null;
        Expression<String> certExpr;
        Expression<String> keyExpr;
        if (matchedPattern == 0) {
            portExpr = defendExpression(expressions[0]);
            certExpr = defendExpression(expressions[1]);
            keyExpr = defendExpression(expressions[2]);
            if (!canInitSafely(portExpr) || !canInitSafely(certExpr) || !canInitSafely(keyExpr)) {
                return false;
            }
        } else {
            certExpr = defendExpression(expressions[0]);
            keyExpr = defendExpression(expressions[1]);
            if (!canInitSafely(certExpr) || !canInitSafely(keyExpr)) {
                return false;
            }
        }
        applyHttps(portExpr, certExpr, keyExpr);
        return true;
    }

    @Override
    protected void execute(Event event) {
        // Configured at parse time inside jsonic server section.
    }

    private void applyHttps(@Nullable Expression<Number> port, Expression<String> cert, Expression<String> key) {
        JsonicApp app = SecJsonicServer.PARSING.get();
        if (app == null) {
            app = JsonicServerRegistry.getDefaultApp();
        }
        if (app == null) {
            return;
        }
        String certPath = literalString(cert);
        String keyPath = literalString(key);
        if (certPath == null || keyPath == null) {
            return;
        }
        app.setHttpsEnabled(true);
        app.setHttpsCertPath(certPath);
        app.setHttpsKeyPath(keyPath);
        Integer httpsPort = literalInt(port);
        if (httpsPort != null) {
            app.setHttpsPort(httpsPort);
        }
    }

    @Nullable
    private static String literalString(Expression<String> expr) {
        if (expr instanceof Literal<?> literal) {
            return (String) literal.getSingle();
        }
        return null;
    }

    @Nullable
    private static Integer literalInt(@Nullable Expression<Number> expr) {
        if (expr instanceof Literal<?> literal) {
            Number n = (Number) literal.getSingle();
            return n == null ? null : n.intValue();
        }
        return null;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "enable https for jsonic server";
    }
}

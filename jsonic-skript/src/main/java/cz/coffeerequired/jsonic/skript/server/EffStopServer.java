package cz.coffeerequired.jsonic.skript.server;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.coffeerequired.jsonic.server.ServerEngine;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Stop jsonic server")
@Description("Stops the running embedded jsonic HTTP server.")
@Since("1.0")
@Examples("stop jsonic server")
public class EffStopServer extends Effect {

    @Override
    protected void execute(Event event) {
        ServerEngine.INSTANCE.stop();
        JsonicServerRegistry.setRunningApp(null);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "stop jsonic server";
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }
}

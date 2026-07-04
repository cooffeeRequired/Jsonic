package cz.coffeerequired.jsonic.skript.server;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("WebSocket message")
@Description({
        "Fired when a WebSocket client sends a text message.",
        "Use `event-message` for the message text."
})
@Since("1.0")
@Examples("""
        on jsonic ws message:
            broadcast event-message
        """)
public class WebSocketMessageEvent extends SkriptEvent {

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof JsonicWebSocketMessageEvent;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "jsonic ws message";
    }
}

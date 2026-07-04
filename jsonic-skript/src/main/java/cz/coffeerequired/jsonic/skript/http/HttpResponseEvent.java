package cz.coffeerequired.jsonic.skript.http;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Http response received")
@Description({
        "Fired after a non-blocking jsonic request completes.",
        "Use `event-response` for the HttpResponseData value."
})
@Since("1.0")
@Examples("""
        on http response received:
            send event-response's status code
            send event-response's body
        """)
public class HttpResponseEvent extends SkriptEvent {

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof JsonicHttpResponseEvent;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "jsonic http response";
    }
}

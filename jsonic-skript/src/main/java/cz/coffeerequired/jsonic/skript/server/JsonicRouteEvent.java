package cz.coffeerequired.jsonic.skript.server;

import cz.coffeerequired.jsonic.server.JsonicCall;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class JsonicRouteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final JsonicCall call;

    public JsonicRouteEvent(JsonicCall call) {
        super(false);
        this.call = call;
    }

    public JsonicCall getCall() {
        return call;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

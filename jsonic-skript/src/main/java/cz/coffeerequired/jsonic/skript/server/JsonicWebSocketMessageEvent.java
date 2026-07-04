package cz.coffeerequired.jsonic.skript.server;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class JsonicWebSocketMessageEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final String path;
    private final String message;

    public JsonicWebSocketMessageEvent(String path, String message) {
        super(false);
        this.path = path;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

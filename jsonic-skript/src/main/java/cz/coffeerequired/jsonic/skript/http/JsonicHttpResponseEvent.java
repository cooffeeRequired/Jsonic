package cz.coffeerequired.jsonic.skript.http;

import cz.coffeerequired.jsonic.core.http.HttpResponseData;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class JsonicHttpResponseEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private HttpResponseData response = HttpResponseData.Companion.empty();

    public JsonicHttpResponseEvent() {
        super(false);
    }

    public HttpResponseData getResponse() {
        return response;
    }

    public void setResponse(HttpResponseData response) {
        this.response = response;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

package cz.coffeerequired.jsonic.skript.modules;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.lang.ParseContext;
import ch.njol.yggdrasil.Fields;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import cz.coffeerequired.jsonic.core.http.HttpMethod;
import cz.coffeerequired.jsonic.core.http.HttpRequest;
import cz.coffeerequired.jsonic.core.http.HttpResponseData;
import cz.coffeerequired.jsonic.core.http.RequestStatus;
import cz.coffeerequired.jsonic.skript.JsonicSkriptRegister;
import cz.coffeerequired.jsonic.skript.http.*;
import org.jetbrains.annotations.NotNull;

import java.io.StreamCorruptedException;

public final class HttpModule {

    private HttpModule() {
    }

    public static void register(JsonicSkriptRegister register) {
        register.type(new ClassInfo<>(HttpRequest.class, "jsonicrequest")
                .user("jsonic ?requests?")
                .name("jsonic request")
                .since("1.0")
                .parser(new Parser<>() {
                    @Override
                    public @NotNull String toString(HttpRequest request, int i) {
                        return request.toString();
                    }

                    @Override
                    public @NotNull String toVariableNameString(HttpRequest request) {
                        return request.toString();
                    }

                    @Override
                    public boolean canParse(@NotNull ParseContext context) {
                        return false;
                    }
                }));

        register.type(new ClassInfo<>(HttpResponseData.class, "jsonicresponse")
                .user("jsonic ?responses?")
                .name("jsonic response")
                .since("1.0")
                .parser(new Parser<>() {
                    @Override
                    public @NotNull String toString(HttpResponseData response, int i) {
                        return "response " + response.getStatusCode();
                    }

                    @Override
                    public @NotNull String toVariableNameString(HttpResponseData response) {
                        return toString(response, 0);
                    }

                    @Override
                    public boolean canParse(@NotNull ParseContext context) {
                        return false;
                    }
                })
                .serializer(new Serializer<>() {
                    @Override
                    public Fields serialize(HttpResponseData value) {
                        Fields fields = new Fields();
                        fields.putObject("statusCode", value.getStatusCode());
                        fields.putObject("body", value.getBody());
                        fields.putObject("headersJson", value.getHeadersJson().toString());
                        fields.putObject("status", value.getStatus().name());
                        return fields;
                    }

                    @Override
                    public void deserialize(HttpResponseData value, @NotNull Fields fields) {
                        assert false;
                    }

                    @Override
                    public HttpResponseData deserialize(@NotNull Fields fields) throws StreamCorruptedException {
                        int statusCode = (int) fields.getObject("statusCode");
                        String body = (String) fields.getObject("body");
                        String headersRaw = (String) fields.getObject("headersJson");
                        String statusName = (String) fields.getObject("status");
                        fields.removeField("statusCode");
                        fields.removeField("body");
                        fields.removeField("headersJson");
                        fields.removeField("status");
                        JsonElement headers = headersRaw == null || headersRaw.isBlank()
                                ? com.google.gson.JsonNull.INSTANCE
                                : JsonParser.parseString(headersRaw);
                        RequestStatus status = statusName == null
                                ? RequestStatus.UNKNOWN
                                : RequestStatus.valueOf(statusName);
                        return new HttpResponseData(statusCode, body == null ? "" : body, headers, status);
                    }

                    @Override
                    public boolean mustSyncDeserialization() {
                        return false;
                    }

                    @Override
                    protected boolean canBeInstantiated() {
                        return false;
                    }
                }));

        register.expression(ExprHttpRequest.class, HttpRequest.class, httpRequestPatterns());

        register.expression(ExprLastResponse.class, HttpResponseData.class,
                "last response of %jsonicrequest%",
                "%jsonicrequest%'s last response");

        register.expression(ExprResponseBody.class, String.class,
                "%jsonicresponse%'s body",
                "body of %jsonicresponse%");

        register.expression(ExprResponseStatusCode.class, Number.class,
                "%jsonicresponse%'s status code",
                "status code of %jsonicresponse%");

        register.effect(EffExecuteHttp.class,
                "execute %jsonicrequest% [as [(:non|:not)(-| )blocking]]",
                "send %jsonicrequest% [as [(:non|:not)(-| )blocking]]");

        register.event("Http response received", HttpResponseEvent.class,
                JsonicHttpResponseEvent.class,
                "[on] received [http] response",
                "[on] http response received");

        register.eventValue(JsonicHttpResponseEvent.class, HttpResponseData.class,
                JsonicHttpResponseEvent::getResponse,
                "event-response", "event-response");
    }

    private static String[] httpRequestPatterns() {
        String[] templates = {
                "jsonic %s request to %%string%%",
                "jsonic %s request on %%string%%",
                "prepare %s request on %%string%%",
                "prepare [a] %s request [to [url]] %%string%%"
        };
        HttpMethod[] methods = HttpMethod.values();
        String[] patterns = new String[templates.length * methods.length];
        int index = 0;
        for (String template : templates) {
            for (HttpMethod method : methods) {
                patterns[index++] = String.format(template, method.name());
            }
        }
        return patterns;
    }
}

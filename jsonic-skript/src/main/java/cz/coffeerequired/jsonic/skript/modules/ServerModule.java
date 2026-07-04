package cz.coffeerequired.jsonic.skript.modules;

import cz.coffeerequired.jsonic.server.JsonicApp;
import cz.coffeerequired.jsonic.skript.JsonicAddon;
import cz.coffeerequired.jsonic.skript.JsonicSkriptRegister;
import cz.coffeerequired.jsonic.skript.server.*;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.lang.structure.Structure;

public final class ServerModule {

    private ServerModule() {
    }

    public static void register(JsonicSkriptRegister register) {
        register.structure(StructDefineMiddleware.class, "define middleware %string%:");

        register.type(new ch.njol.skript.classes.ClassInfo<>(JsonicApp.class, "jsonicserver")
                .user("jsonic ?servers?")
                .name("jsonic server")
                .since("1.0")
                .parser(new ch.njol.skript.classes.Parser<>() {
                    @Override
                    public @org.jetbrains.annotations.NotNull String toString(JsonicApp o, int flags) {
                        return "jsonic server on port " + o.getPort();
                    }

                    @Override
                    public @org.jetbrains.annotations.NotNull String toVariableNameString(JsonicApp o) {
                        return toString(o, 0);
                    }

                    @Override
                    public boolean canParse(@org.jetbrains.annotations.NotNull ch.njol.skript.lang.ParseContext context) {
                        return false;
                    }
                }));

        register.section(SecJsonicServer.class,
                "jsonic server on port %number% host %string%",
                "jsonic server on port %number%");

        register.section(SecRouteGet.class,
                "get %string%:",
                "post %string%:",
                "put %string%:",
                "patch %string%:",
                "delete %string%:");

        register.section(SecRouteGroup.class,
                "group %string%:",
                "resource %string%:");

        register.effect(EffApplyMiddleware.class,
                "middleware %string%");

        register.section(SecInlineMiddleware.class,
                "middleware %string%:");

        register.effect(EffMiddlewareContinue.class,
                "continue middleware",
                "continue middleware %boolean%",
                "stop middleware",
                "return %boolean% in middleware");

        register.section(SecJsonicError.class,
                "error %string%:");

        register.effect(EffServeFiles.class,
                "serve files from %string% at %string%",
                "serve files at %string% from %string%");

        register.effect(EffEnableHttps.class,
                "enable https on port %number% cert %string% key %string%",
                "enable https cert %string% key %string%");

        register.expression(ExprRouteParam.class, String.class,
                "route param %string%",
                "request param %string%");

        register.expression(ExprRequestHeader.class, String.class,
                "request header %string%");

        register.expression(ExprBearerToken.class, String.class,
                "bearer token",
                "auth token");

        register.effect(EffStartServer.class,
                "start jsonic server",
                "start %jsonicserver%");

        register.effect(EffStopServer.class,
                "stop jsonic server");

        register.effect(EffReplyJson.class,
                "reply json %objects%",
                "reply %objects%",
                "reply with status %number%");

        register.effect(EffReplyFile.class,
                "reply file %string%",
                "reply file %string% with status %number%");

        register.event("WebSocket message", WebSocketMessageEvent.class,
                JsonicWebSocketMessageEvent.class,
                "[on] jsonic ws message",
                "[on] websocket message");

        register.eventValue(JsonicWebSocketMessageEvent.class, String.class,
                event -> event.getMessage(),
                "event-message", "ws-message");
    }
}

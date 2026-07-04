package cz.coffeerequired.jsonic.skript.server;

import cz.coffeerequired.jsonic.server.JsonPathUtils;
import cz.coffeerequired.jsonic.server.JsonicRouteMiddleware;
import cz.coffeerequired.jsonic.server.MiddlewareType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/** Parse-time stack for nested route groups and middleware chain. */
public final class RouteGroupContext {

    private static final ThreadLocal<Deque<Frame>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private RouteGroupContext() {
    }

    public static void enterGroup(String prefix) {
        Frame parent = STACK.get().peek();
        String combined = parent == null
                ? JsonPathUtils.normalize(prefix)
                : JsonPathUtils.join(parent.pathPrefix, prefix);
        STACK.get().push(new Frame(combined));
    }

    public static void leaveGroup() {
        Deque<Frame> stack = STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public static void addBuiltIn(MiddlewareType type) {
        Deque<Frame> stack = STACK.get();
        if (stack.isEmpty()) {
            stack.push(new Frame(""));
        }
        stack.peek().builtIn.add(type);
    }

    /** @deprecated use {@link #addBuiltIn(MiddlewareType)} */
    @Deprecated
    public static void addMiddleware(MiddlewareType type) {
        addBuiltIn(type);
    }

    public static void addSkript(JsonicRouteMiddleware middleware) {
        Deque<Frame> stack = STACK.get();
        if (stack.isEmpty()) {
            stack.push(new Frame(""));
        }
        stack.peek().inlineSkript.add(middleware);
    }

    public static String pathPrefix() {
        Frame frame = STACK.get().peek();
        return frame == null ? "" : frame.pathPrefix;
    }

    public static List<MiddlewareType> middlewares() {
        List<MiddlewareType> merged = new ArrayList<>();
        for (Frame frame : STACK.get()) {
            merged.addAll(frame.builtIn);
        }
        return Collections.unmodifiableList(merged);
    }

    public static List<JsonicRouteMiddleware> skriptMiddlewares() {
        List<JsonicRouteMiddleware> merged = new ArrayList<>();
        for (Frame frame : STACK.get()) {
            merged.addAll(frame.inlineSkript);
        }
        return Collections.unmodifiableList(merged);
    }

    public static void addSkriptMiddlewareName(String name) {
        Deque<Frame> stack = STACK.get();
        if (stack.isEmpty()) {
            stack.push(new Frame(""));
        }
        stack.peek().skriptMiddlewareNames.add(name);
    }

    public static List<String> skriptMiddlewareNames() {
        List<String> merged = new ArrayList<>();
        for (Frame frame : STACK.get()) {
            merged.addAll(frame.skriptMiddlewareNames);
        }
        return Collections.unmodifiableList(merged);
    }

    public static void clear() {
        STACK.remove();
    }

    private static final class Frame {
        final String pathPrefix;
        final List<MiddlewareType> builtIn = new ArrayList<>();
        final List<String> skriptMiddlewareNames = new ArrayList<>();
        final List<JsonicRouteMiddleware> inlineSkript = new ArrayList<>();

        Frame(String pathPrefix) {
            this.pathPrefix = pathPrefix;
        }
    }
}

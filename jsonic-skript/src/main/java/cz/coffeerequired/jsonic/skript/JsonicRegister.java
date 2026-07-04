package cz.coffeerequired.jsonic.skript;

import cz.coffeerequired.jsonic.core.JsonicLogger;
import cz.coffeerequired.jsonic.core.JsonicSettings;
import cz.coffeerequired.jsonic.core.support.AnsiColorConverter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class JsonicRegister {

    public enum ElementKind {
        EXPRESSIONS("Expressions"),
        EFFECTS("Effects"),
        CONDITIONS("Conditions"),
        SECTIONS("Sections"),
        STRUCTURES("Structures"),
        TYPES("Types"),
        EVENTS("Events"),
        EVENT_VALUES("Event Values");

        private final String label;

        ElementKind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private static final EnumMap<ElementKind, List<String>> globalElements = new EnumMap<>(ElementKind.class);
    private static final Map<String, Map<ElementKind, Integer>> moduleCounts = new LinkedHashMap<>();

    static {
        for (ElementKind kind : ElementKind.values()) {
            globalElements.put(kind, new ArrayList<>());
        }
    }

    private JsonicRegister() {
    }

    public static void reset() {
        for (List<String> list : globalElements.values()) {
            list.clear();
        }
        moduleCounts.clear();
    }

    public static void track(String moduleName, ElementKind kind, Class<?> element) {
        globalElements.get(kind).add(element.getSimpleName());
        moduleCounts
                .computeIfAbsent(moduleName, ignored -> new EnumMap<>(ElementKind.class))
                .merge(kind, 1, Integer::sum);
        JsonicLogger.debug(
                "&8Registering %s: &7%s &8(%s)",
                kind.label(),
                element.getSimpleName(),
                moduleName
        );
    }

    public static void registerModule(String moduleName, Consumer<JsonicSkriptRegister> registrar) {
        JsonicLogger.info(
                "Registering module: %s%s&r",
                AnsiColorConverter.hexToAnsi("#6EE7B7"),
                moduleName
        );
        JsonicSkriptRegister register = new JsonicSkriptRegister(moduleName);
        registrar.accept(register);
        printModuleSummary(moduleName);
    }

    private static void printModuleSummary(String moduleName) {
        Map<ElementKind, Integer> counts = moduleCounts.get(moduleName);
        if (counts == null || counts.isEmpty()) {
            return;
        }
        for (ElementKind kind : ElementKind.values()) {
            int count = counts.getOrDefault(kind, 0);
            if (count > 0) {
                JsonicLogger.logElement(kind.label(), count);
            }
        }
    }

    public static void printSummary() {
        int total = globalElements.values().stream().mapToInt(List::size).sum();
        JsonicLogger.accent("Registered %d Skript element(s) across %d module(s)", total, moduleCounts.size());

        for (ElementKind kind : ElementKind.values()) {
            int count = globalElements.get(kind).size();
            if (count > 0) {
                JsonicLogger.logElement(kind.label(), count);
            }
        }

        if (JsonicSettings.INSTANCE.getDebug()) {
            JsonicLogger.debug("&8Element index: %s", globalElements);
        }
    }

    public static Map<ElementKind, List<String>> snapshot() {
        EnumMap<ElementKind, List<String>> copy = new EnumMap<>(ElementKind.class);
        for (Map.Entry<ElementKind, List<String>> entry : globalElements.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return copy;
    }
}

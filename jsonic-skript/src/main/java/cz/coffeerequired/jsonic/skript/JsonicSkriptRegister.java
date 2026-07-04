package cz.coffeerequired.jsonic.skript;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.registrations.Classes;
import org.bukkit.event.Event;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.util.Priority;

public class JsonicSkriptRegister {

    private final String moduleName;

    public JsonicSkriptRegister() {
        this("Jsonic");
    }

    public JsonicSkriptRegister(String moduleName) {
        this.moduleName = moduleName;
    }

    private SyntaxRegistry syntaxRegistry() {
        return JsonicAddon.getAddon().syntaxRegistry();
    }

    private EventValueRegistry eventValueRegistry() {
        return JsonicAddon.getAddon().registry(EventValueRegistry.class);
    }

    public <E extends Structure> void structure(Class<E> c, String... patterns) {
        syntaxRegistry().register(
                SyntaxRegistry.STRUCTURE,
                DefaultSyntaxInfos.Structure.builder(c)
                        .addPatterns(JsonicAddon.prefixPatterns(patterns))
                        .build()
        );
        JsonicRegister.track(moduleName, JsonicRegister.ElementKind.STRUCTURES, c);
    }

    public <E extends Effect> void effect(Class<E> c, String... patterns) {
        syntaxRegistry().register(
                SyntaxRegistry.EFFECT,
                SyntaxInfo.builder(c).addPatterns(JsonicAddon.prefixPatterns(patterns)).build()
        );
        JsonicRegister.track(moduleName, JsonicRegister.ElementKind.EFFECTS, c);
    }

    public <E extends Condition> void condition(Class<E> c, String... patterns) {
        syntaxRegistry().register(
                SyntaxRegistry.CONDITION,
                SyntaxInfo.builder(c).addPatterns(JsonicAddon.prefixPatterns(patterns)).build()
        );
        JsonicRegister.track(moduleName, JsonicRegister.ElementKind.CONDITIONS, c);
    }

    public <E extends Section> void section(Class<E> c, String... patterns) {
        syntaxRegistry().register(
                SyntaxRegistry.SECTION,
                SyntaxInfo.builder(c).addPatterns(JsonicAddon.prefixPatterns(patterns)).build()
        );
        JsonicRegister.track(moduleName, JsonicRegister.ElementKind.SECTIONS, c);
    }

    public <E extends Expression<T>, T> void expression(Class<E> c, Class<T> returnType, String... patterns) {
        expression(c, returnType, SyntaxInfo.SIMPLE, patterns);
    }

    public <E extends Expression<T>, T> void expression(Class<E> c, Class<T> returnType, Priority priority, String... patterns) {
        syntaxRegistry().register(
                SyntaxRegistry.EXPRESSION,
                SyntaxInfo.Expression.builder(c, returnType)
                        .priority(priority)
                        .addPatterns(JsonicAddon.prefixPatterns(patterns))
                        .build()
        );
        JsonicRegister.track(moduleName, JsonicRegister.ElementKind.EXPRESSIONS, c);
    }

    public <T> void type(ClassInfo<T> classInfo) {
        Classes.registerClass(classInfo);
        JsonicRegister.track(moduleName, JsonicRegister.ElementKind.TYPES, classInfo.getC());
    }

    public void event(String name, Class<? extends SkriptEvent> c, Class<? extends Event> eventClass, String... patterns) {
        String[] prefixed = JsonicAddon.prefixPatterns(patterns);
        for (int i = 0; i < prefixed.length; i++) {
            prefixed[i] = BukkitSyntaxInfos.fixPattern(prefixed[i]);
        }
        syntaxRegistry().register(
                BukkitSyntaxInfos.Event.KEY,
                BukkitSyntaxInfos.Event.builder(c, name)
                        .addEvent(eventClass)
                        .addPatterns(prefixed)
                        .addSince("1.0")
                        .build()
        );
        JsonicRegister.track(moduleName, JsonicRegister.ElementKind.EVENTS, c);
    }

    public <E extends Event, V> void eventValue(
            Class<E> eventClass,
            Class<V> valueClass,
            Converter<E, V> converter,
            String... patterns
    ) {
        eventValueRegistry().register(
                EventValue.builder(eventClass, valueClass)
                        .getter(converter)
                        .patterns(patterns)
                        .time(EventValue.Time.NOW)
                        .build()
        );
        JsonicRegister.track(moduleName, JsonicRegister.ElementKind.EVENT_VALUES, valueClass);
    }
}

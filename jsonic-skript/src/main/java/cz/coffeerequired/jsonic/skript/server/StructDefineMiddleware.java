package cz.coffeerequired.jsonic.skript.server;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.structure.Structure;

@Name("Define middleware")
@Description({
        "Top-level structure defining a reusable middleware chain by name.",
        "Reference it inside routes with `middleware \"name\"` or `middleware \"name\":`."
})
@Since("1.0")
@Examples("""
        define middleware "requireAuth":
            if bearer token is "":
                reply json "{\\"error\\":\\"unauthorized\\"}"
                reply with status 401
                stop middleware
            continue middleware
        """)
public class StructDefineMiddleware extends Structure {

    public static final Priority PRIORITY = new Priority(100);

    private final JsonicRouteSectionHelper loader = new JsonicRouteSectionHelper();

    private SectionNode source;
    private String name;

    @Override
    public boolean init(Literal<?>[] literals, int matchedPattern, ParseResult parseResult,
                        @Nullable EntryContainer entryContainer) {
        if (entryContainer == null) {
            return false;
        }
        source = entryContainer.getSource();
        if (literals.length == 0 || literals[0] == null) {
            return false;
        }
        name = (String) literals[0].getSingle();
        return name != null && !name.isBlank();
    }

    @Override
    public boolean load() {
        Trigger trigger = loader.load(source, name);
        if (trigger == null) {
            return false;
        }
        MiddlewareDefinitionRegistry.define(name, trigger);
        return true;
    }

    @Override
    public void unload() {
        if (name != null) {
            MiddlewareDefinitionRegistry.remove(name);
        }
    }

    @Override
    public Priority getPriority() {
        return PRIORITY;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "define jsonic middleware \"" + name + "\"";
    }
}

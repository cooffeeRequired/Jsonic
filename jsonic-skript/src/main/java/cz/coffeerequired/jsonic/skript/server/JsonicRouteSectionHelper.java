package cz.coffeerequired.jsonic.skript.server;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Nullable;

/** Loads route/middleware triggers outside of a normal section init. */
final class JsonicRouteSectionHelper extends Section {

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed,
                          ParseResult parseResult, SectionNode sectionNode,
                          java.util.List<TriggerItem> triggerItems) {
        return false;
    }

    @Override
    protected TriggerItem walk(Event event) {
        return null;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "jsonic route trigger loader";
    }

    Trigger load(SectionNode node, String name) {
        return loadCode(node, "jsonic middleware " + name, JsonicRouteEvent.class);
    }
}

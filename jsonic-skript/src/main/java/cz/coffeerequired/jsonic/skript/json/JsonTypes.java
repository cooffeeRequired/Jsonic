package cz.coffeerequired.jsonic.skript.json;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.yggdrasil.Fields;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import cz.coffeerequired.jsonic.core.JsonBuilder;
import cz.coffeerequired.jsonic.core.JsonEngine;
import cz.coffeerequired.jsonic.core.PathEngine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.StreamCorruptedException;

public final class JsonTypes {

    private JsonTypes() {
    }

    public static void register(cz.coffeerequired.jsonic.skript.JsonicSkriptRegister register) {
        register.type(new ClassInfo<>(JsonElement.class, "jsonelement")
                .user("jsonelement", "json", "json ?elements?")
                .name("jsonelement")
                .description("JSON value used by Jsonic")
                .since("1.0")
                .parser(JSON_PARSER)
                .serializer(JSON_SERIALIZER)
                .changer(JSON_CHANGER));

        register.type(new ClassInfo<>(JsonBuilder.class, "jsonbuilder")
                .user("json ?builders?")
                .name("json builder")
                .description("Fluent JSON builder handle")
                .since("1.0")
                .parser(BUILDER_PARSER));
    }

    private static final Parser<JsonElement> JSON_PARSER = new Parser<>() {
        @Override
        public @NotNull String toString(JsonElement o, int flags) {
            return o.toString();
        }

        @Override
        public @NotNull String toVariableNameString(JsonElement o) {
            return toString(o, 0);
        }

        @Override
        public boolean canParse(@NotNull ParseContext context) {
            return false;
        }
    };

    private static final Serializer<JsonElement> JSON_SERIALIZER = new Serializer<>() {
        @Override
        public Fields serialize(JsonElement o) {
            Fields fields = new Fields();
            fields.putObject("json", o.toString());
            return fields;
        }

        @Override
        public void deserialize(JsonElement o, @NotNull Fields f) {
            assert false;
        }

        @Override
        public JsonElement deserialize(@NotNull Fields fields) throws StreamCorruptedException {
            Object field = fields.getObject("json");
            fields.removeField("json");
            return field == null ? JsonNull.INSTANCE : JsonEngine.INSTANCE.toJson(field);
        }

        @Override
        public boolean mustSyncDeserialization() {
            return false;
        }

        @Override
        protected boolean canBeInstantiated() {
            return false;
        }
    };

    private static final Changer<JsonElement> JSON_CHANGER = new Changer<>() {
        @Override
        public @Nullable Class<?> @NotNull [] acceptChange(@NotNull ChangeMode mode) {
            return switch (mode) {
                case SET, ADD -> CollectionUtils.array(Object.class);
                default -> null;
            };
        }

        @Override
        public void change(JsonElement[] what, @Nullable Object[] delta, ChangeMode mode) {
            if (what.length == 0 || delta == null || delta.length == 0) return;
            JsonElement root = what[0];
            for (Object change : delta) {
                if (!(change instanceof PathChange pc)) continue;
                if (mode == ChangeMode.SET) {
                    PathEngine.INSTANCE.set(root, pc.path(), JsonEngine.INSTANCE.toJson(pc.value()));
                } else if (mode == ChangeMode.ADD) {
                    PathEngine.INSTANCE.set(root, pc.path() + "[]", JsonEngine.INSTANCE.toJson(pc.value()));
                }
            }
        }
    };

    private static final Parser<JsonBuilder> BUILDER_PARSER = new Parser<>() {
        @Override
        public @NotNull String toString(JsonBuilder o, int flags) {
            return "jsonic builder";
        }

        @Override
        public @NotNull String toVariableNameString(JsonBuilder o) {
            return "jsonic builder";
        }

        @Override
        public boolean canParse(@NotNull ParseContext context) {
            return false;
        }
    };

    public record PathChange(String path, Object value) {
    }
}

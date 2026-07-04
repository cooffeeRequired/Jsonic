package cz.coffeerequired.jsonic.skript.modules;

import cz.coffeerequired.jsonic.skript.JsonicSkriptRegister;
import cz.coffeerequired.jsonic.skript.storage.CondStorageExists;
import cz.coffeerequired.jsonic.skript.storage.EffBindStorage;
import cz.coffeerequired.jsonic.skript.storage.ExprStorageJson;
import org.skriptlang.skript.registration.SyntaxInfo;

public final class StorageModule {

    private StorageModule() {
    }

    public static void register(JsonicSkriptRegister register) {
        register.expression(ExprStorageJson.class, com.google.gson.JsonElement.class,
                SyntaxInfo.SIMPLE,
                "json storage of id %string%",
                "cached json [with id] %string%",
                "json cache %string%");

        register.condition(CondStorageExists.class,
                "json cache %string% exists",
                "json cache %string% does(n't| not) exist");

        register.effect(EffBindStorage.class,
                "(bind|link) json file %string% as %string%",
                "bind json file %string% to [json] cache %string%",
                "bind json file %string% as %string% and [let] watch [storage] watcher");
    }
}

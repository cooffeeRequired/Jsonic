package cz.coffeerequired.jsonic.skript.modules;

import cz.coffeerequired.jsonic.skript.JsonicSkriptRegister;
import cz.coffeerequired.jsonic.skript.http.*;
import cz.coffeerequired.jsonic.skript.json.*;
import cz.coffeerequired.jsonic.skript.storage.*;

public final class CoreModule {

    private CoreModule() {
    }

    public static void register(JsonicSkriptRegister register) {
        JsonTypes.register(register);

        register.expression(ExprJsonFrom.class, com.google.gson.JsonElement.class,
                "json from file %strings%",
                "json from [the] file %strings%",
                "json from %objects%",
                "parse %objects% as json");

        register.expression(ExprJsonPath.class, Object.class,
                "path %string% of %jsonelement%",
                "value at path %string% in %jsonelement%");

        register.expression(ExprJsonBuilder.class, cz.coffeerequired.jsonic.core.JsonBuilder.class,
                "jsonic builder",
                "[a] jsonic json builder");

        register.expression(ExprBuildJson.class, com.google.gson.JsonElement.class,
                "build json from %jsonbuilder%");

        register.expression(ExprPrettyJson.class, String.class,
                "%jsonelement% as pretty [printed] json");

        // --- JSON builder (EffSetJsonPath, ExprBuildJson) ---
        register.effect(EffSetJsonPath.class,
                "set %jsonbuilder%'s path %string% to %objects%",
                "set path %string% in %jsonbuilder% to %objects%");

        // --- JSON in-place (EffRemoveJsonPath, EffSetJsonPathInJson) ---
        register.effect(EffRemoveJsonPath.class,
                "remove path %string% from %jsonelement%",
                "delete path %string% in %jsonelement%");

        register.effect(EffSetJsonPathInJson.class,
                "set path %string% in %jsonelement% to %objects%");

        // --- soubor (EffSaveJsonFile, CondJsonFileExists) ---
        register.effect(EffSaveJsonFile.class,
                "save %jsonelement% to [json] file %~string%",
                "write %jsonelement% to file %~string%");

        register.condition(CondJsonEmpty.class,
                "%jsonelement% is empty json",
                "%jsonelement% is(n't| not) empty json");

        register.condition(CondJsonFileExists.class,
                "json file %~string% exists",
                "json file %~string% does(n't| not) exist");
    }
}

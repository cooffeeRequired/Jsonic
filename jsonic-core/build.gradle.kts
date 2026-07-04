plugins {
    kotlin("jvm")
}

dependencies {
    implementation("com.google.code.gson:gson:${property("gsonVersion")}")
    compileOnly("io.papermc.paper:paper-api:${property("paperApiVersion")}")
    compileOnly("com.github.SkriptLang:Skript:${property("skriptVersion")}") {
        isTransitive = false
    }
}

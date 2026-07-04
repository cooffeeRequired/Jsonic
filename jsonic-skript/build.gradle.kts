plugins {
    kotlin("jvm")
    java
}

dependencies {
    api(project(":jsonic-core"))
    api(project(":jsonic-server"))
    compileOnly("io.papermc.paper:paper-api:${property("paperApiVersion")}")
    compileOnly("com.github.SkriptLang:Skript:${property("skriptVersion")}") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

import java.security.MessageDigest
import java.math.BigInteger

val skriptVersion = project.property("skriptVersion") as String

plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper")
}

dependencies {
    implementation(project(":jsonic-core"))
    implementation(project(":jsonic-server"))
    implementation(project(":jsonic-skript"))
    compileOnly("io.papermc.paper:paper-api:${property("paperApiVersion")}")
    compileOnly("com.github.SkriptLang:Skript:${property("skriptVersion")}") {
        isTransitive = false
    }
    implementation("org.bstats:bstats-bukkit:${property("bstatsVersion")}")
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

fun generateShortRev(): String {
    val md = MessageDigest.getInstance("SHA-1")
    val digest = md.digest(System.currentTimeMillis().toString().toByteArray())
    return BigInteger(1, digest).toString(16).padStart(40, '0').substring(0, 8)
}

val shortRev = generateShortRev()

tasks.processResources {
    filesMatching(listOf("plugin.yml", "lang/default.lang")) {
        expand(mapOf("version" to project.version, "rev" to shortRev))
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("jsonic.jar")
    relocate("com.google", "cz.coffeerequired.jsonic.shadowed.google")
    relocate("org.bstats", "cz.coffeerequired.jsonic.shadowed.bstats")
    relocate("io.ktor", "cz.coffeerequired.jsonic.shadowed.ktor")
    relocate("io.netty", "cz.coffeerequired.jsonic.shadowed.netty")
    exclude("META-INF/*.MF", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

tasks.register<Copy>("syncRunServerScripts") {
    group = "run"
    description = "Copy Skript test scripts into the runServer folder"
    from("src/test/scripts")
    into("run/plugins/Skript/scripts/jsonic")
}

tasks.register<Copy>("syncRunServerWeb") {
    group = "run"
    description = "Copy static web assets into Jsonic plugin data folder"
    from("src/test/web")
    into("run/plugins/Jsonic")
}

tasks.register<Copy>("syncRunServerPlugin") {
    group = "run"
    description = "Copy jsonic.jar into the runServer plugins folder"
    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar)
    into("run/plugins")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.named("runServer") {
    dependsOn("syncRunServerPlugin", "syncRunServerScripts", "syncRunServerWeb")
}

// run-paper auto-loads build/libs/jsonic.jar; syncRunServerPlugin also copies to run/plugins → duplicate "Jsonic"
runPaper {
    disablePluginJarDetection()
}

tasks.register<Exec>("mockHttpServer") {
    group = "run"
    description = "Start Bun mock HTTP server for Skript client tests (port 18101); no-op if already running"
    workingDir = rootProject.projectDir
    commandLine("bun", "jsonic-bukkit/dev/mock-http/server.ts")
    isIgnoreExitValue = false
    // Server běží na popředí — ukončení Ctrl+C
}

tasks {
    runServer {
        minecraftVersion("26.2")
        downloadPlugins {
            url("https://github.com/SkriptLang/Skript/releases/download/$skriptVersion/Skript-$skriptVersion.jar")
        }
    }
}

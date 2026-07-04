plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "jsonic"

include("jsonic-core")
include("jsonic-server")
include("jsonic-skript")
include("jsonic-bukkit")

plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":jsonic-core"))
    implementation("io.ktor:ktor-server-core:${property("ktorVersion")}")
    implementation("io.ktor:ktor-server-netty:${property("ktorVersion")}")
    implementation("io.ktor:ktor-server-cors:${property("ktorVersion")}")
    implementation("io.ktor:ktor-server-call-logging:${property("ktorVersion")}")
    implementation("io.ktor:ktor-server-websockets:${property("ktorVersion")}")
    implementation("io.ktor:ktor-network-tls-certificates:${property("ktorVersion")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    compileOnly("io.papermc.paper:paper-api:${property("paperApiVersion")}")
}

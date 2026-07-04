# Jsonic

<img width="995" height="419" alt="fef320a6-15dd-4ea2-bfd6-fbc79ec22b5d" src="https://github.com/user-attachments/assets/e2ffc410-b65e-47ee-8fbb-0ecbaa564942" />

**Blazing fast JSON, HTTP, WebSocket, and storage utilities for Skript.**  

Jsonic is the next-generation addon designed to replace and extend the functionality of SkJson.  
It provides modern and reliable tools for working with web requests, JSON, sockets, storage, and even a lightweight webserver – all directly from Skript.

---

## ✨ Features
- **HTTP Client** – send GET, POST, PUT, DELETE requests with full control over headers and body.  
- **JSON Utilities** – parse, build, and manipulate JSON easily.  
- **WebSockets** – open, listen, and communicate over WebSockets.  
- **Lightweight WebServer** – spin up a simple HTTP server for custom endpoints.  
- **Storage API** – handle persistent data storage in JSON or other formats.  

---

## 📦 Requirements
- **Minecraft:** 1.18.5+  
- **Java:** JDK 17 or newer  
- **Skript:** Latest stable release  

---
## 🔔 Project Status

Jsonic **1.0.0-SNAPSHOT** — greenfield addon s Fluent API (Kotlin core + Skript Sections).

### Moduly
- `jsonic-core` — JSON engine, HTTP client, storage
- `jsonic-server` — embedded Ktor web server (Hono-style)
- `jsonic-skript` — Skript syntaxe (vlastní syntaxe začínající na `jsonic …`, `json cache`, …)
- `jsonic-bukkit` — plugin → `jsonic.jar`

### Build
```bash
cd Jsonic && ./gradlew shadowJar
```

Výstup: `jsonic-bukkit/build/libs/jsonic.jar`

Migrace ze SkJson: viz [MIGRATION.md](MIGRATION.md)

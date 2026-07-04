# Migrace SkJson → Jsonic

| SkJson | Jsonic |
|--------|--------|
| `[skjson]` prefix | `[jsonic]` prefix |
| `jsonelement` | `json` |
| `prepare GET request on "url"` | `jsonic GET request to "url"` |
| `execute {_req} as non blocking` | `execute {_req} as non blocking` |
| `on http response` | `on jsonic http response` / `on received http response` |
| `set value at path "x" in {_json} to y` | `set {_b}'s path "x" to y` + `build json from {_b}` |
| `json cache "id" exists` | `json cache "id" exists` (stejné) |
| `bind json file ...` | `bind json file ... as "id" and watch storage watcher` |
| *(neexistuje)* | `jsonic server on port 8080:` + `start jsonic server` |

Jsonic je **úmyslně nekompatibilní** 1:1 — nové projekty používají Jsonic, existující skripty mohou zůstat na SkJson 6.0.

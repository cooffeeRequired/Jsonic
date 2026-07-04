#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REPO_ROOT="$(cd "$ROOT/.." && pwd)"
cd "$ROOT"

echo "=== 1/3 Build Jsonic ==="
./gradlew :jsonic-bukkit:shadowJar --no-daemon

echo "=== 2/3 Deploy JAR + test skripty ==="
mkdir -p "$REPO_ROOT/run/plugins"
cp -f jsonic-bukkit/build/libs/jsonic.jar "$REPO_ROOT/run/plugins/jsonic.jar"
mkdir -p "$REPO_ROOT/run/plugins/Skript/scripts/jsonic"
mkdir -p "$ROOT/jsonic-bukkit/run/plugins/Skript/scripts/jsonic"
cp -f jsonic-bukkit/src/test/scripts/*.sk "$REPO_ROOT/run/plugins/Skript/scripts/jsonic/"
cp -f jsonic-bukkit/src/test/scripts/*.sk "$ROOT/jsonic-bukkit/run/plugins/Skript/scripts/jsonic/"

echo "=== 3/3 Hotovo ==="
echo "Spusť server (např. cd Jsonic && ./gradlew :jsonic-bukkit:runServer)"
echo "Na serveru: /sk reload jsonic"
echo "Plný test:  /jsonic fulltest"

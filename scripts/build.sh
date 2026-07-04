#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew :jsonic-bukkit:shadowJar --no-daemon
echo "Built: Jsonic/jsonic-bukkit/build/libs/jsonic.jar"

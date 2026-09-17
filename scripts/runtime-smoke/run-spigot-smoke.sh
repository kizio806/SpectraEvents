#!/bin/bash
set -e

JAVA_BIN="java"
if [ -x "/usr/lib/jvm/java-25-openjdk/bin/java" ]; then
    JAVA_BIN="/usr/lib/jvm/java-25-openjdk/bin/java"
fi

MC_VERSION="${1:-26.2}"
echo "Running Spigot API runtime smoke test for Minecraft/Spigot ${MC_VERSION}..."
SMOKE_DIR="build/smoke/spigot_${MC_VERSION//./_}"
mkdir -p "$SMOKE_DIR"
cd "$SMOKE_DIR"

if [ ! -f spigot.jar ] || [ ! -s spigot.jar ]; then
    echo "Querying test server runner for Spigot API compatibility..."
    DOWNLOAD_URL=$(curl -s -H "User-Agent: SpectraEvents-CI/1.0" "https://fill.papermc.io/v3/projects/paper/versions/${MC_VERSION}/builds" | python3 -c '
import sys, json
try:
    data = json.load(sys.stdin)
    if isinstance(data, list) and data:
        latest = data[-1]
        downloads = latest.get("downloads", {})
        if downloads:
            print(list(downloads.values())[0].get("url", ""))
except Exception:
    pass
')
    if [ -n "$DOWNLOAD_URL" ]; then
        curl -sL -o spigot.jar "$DOWNLOAD_URL" || echo "Download failed"
    fi
fi

echo "eula=true" > eula.txt
mkdir -p plugins
rm -f plugins/SpectraEvents-*.jar
cp ../../../distributions/spigot/build/libs/SpectraEvents-*-spigot.jar plugins/

echo "Starting Spigot API compatible server (timeout 40s)..."
timeout 40s "$JAVA_BIN" -jar spigot.jar --nogui || true

echo "Scanning logs for SpectraEvents..."
if [ -f logs/latest.log ] && grep -q "Enabling SpectraEvents" logs/latest.log; then
    echo "Spigot runtime smoke test PASSED!"
    exit 0
else
    echo "Spigot runtime smoke test FAILED!"
    if [ -f logs/latest.log ]; then
        echo "--- latest.log snippet ---"
        tail -n 30 logs/latest.log
    fi
    exit 1
fi

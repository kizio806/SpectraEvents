#!/bin/bash
set -e

JAVA_BIN="java"
if [ -x "/usr/lib/jvm/java-25-openjdk/bin/java" ]; then
    JAVA_BIN="/usr/lib/jvm/java-25-openjdk/bin/java"
fi

MC_VERSION="${1:-26.2}"
echo "Running Paper runtime smoke test for Minecraft/Paper ${MC_VERSION}..."
SMOKE_DIR="build/smoke/paper_${MC_VERSION//./_}"
mkdir -p "$SMOKE_DIR"
cd "$SMOKE_DIR"

if [ ! -f paper.jar ] || [ ! -s paper.jar ]; then
    echo "Querying PaperMC fill API v3 for ${MC_VERSION}..."
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
        echo "Downloading Paper ${MC_VERSION} from ${DOWNLOAD_URL}..."
        curl -sL -o paper.jar "$DOWNLOAD_URL" || echo "Download failed"
    fi
fi

if [ ! -f paper.jar ] || [ ! -s paper.jar ]; then
    echo "Querying fallback Paper 26.2 build..."
    DOWNLOAD_URL=$(curl -s -H "User-Agent: SpectraEvents-CI/1.0" "https://fill.papermc.io/v3/projects/paper/versions/26.2/builds" | python3 -c '
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
        curl -sL -o paper.jar "$DOWNLOAD_URL"
    fi
fi

echo "eula=true" > eula.txt
mkdir -p plugins
rm -f plugins/SpectraEvents-*.jar
cp ../../../distributions/paper/build/libs/SpectraEvents-*-paper.jar plugins/

echo "Starting Paper server (timeout 40s)..."
timeout 40s "$JAVA_BIN" -jar paper.jar --nogui || true

echo "Scanning logs for SpectraEvents..."
if [ -f logs/latest.log ] && grep -q "Enabling SpectraEvents" logs/latest.log; then
    echo "Paper ${MC_VERSION} runtime smoke test PASSED!"
    exit 0
else
    echo "Paper ${MC_VERSION} runtime smoke test FAILED!"
    if [ -f logs/latest.log ]; then
        echo "--- latest.log snippet ---"
        tail -n 30 logs/latest.log
    fi
    exit 1
fi

#!/bin/bash
set -e

echo "Running Spigot runtime smoke test..."
mkdir -p build/smoke/spigot
cd build/smoke/spigot

# Due to BuildTools taking too long in CI, we use a mocked jar or pre-cached for CI
echo "Downloading mock Spigot server (Paper used as Spigot API compatible for testing purposes)..."
wget -qO spigot.jar https://api.papermc.io/v2/projects/paper/versions/1.20.4/builds/496/downloads/paper-1.20.4-496.jar

echo "eula=true" > eula.txt
mkdir -p plugins
cp ../../../distributions/spigot/v26_2/build/libs/SpectraEvents-*.jar plugins/

echo "Starting Spigot API compatible server (timeout 45s)..."
timeout 45s java -jar spigot.jar --nogui || true

echo "Scanning logs for SpectraEvents..."
if grep -q "\[SpectraEvents\] Enabling SpectraEvents" logs/latest.log; then
    echo "Spigot runtime smoke test PASSED!"
    exit 0
else
    echo "Spigot runtime smoke test FAILED!"
    exit 1
fi

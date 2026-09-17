#!/bin/bash
set -e

echo "Running Paper runtime smoke test..."
mkdir -p build/smoke/paper
cd build/smoke/paper

# Download Paper API test server
wget -qO paper.jar https://api.papermc.io/v2/projects/paper/versions/1.20.4/builds/496/downloads/paper-1.20.4-496.jar

echo "eula=true" > eula.txt
mkdir -p plugins
cp ../../../distributions/paper/v26_2/build/libs/SpectraEvents-*.jar plugins/

# Run server with a timeout (using a Java agent or just timeout command)
echo "Starting Paper server (timeout 45s)..."
timeout 45s java -jar paper.jar --nogui || true

echo "Scanning logs for SpectraEvents..."
if grep -q "\[SpectraEvents\] Enabling SpectraEvents" logs/latest.log; then
    echo "Paper runtime smoke test PASSED!"
    exit 0
else
    echo "Paper runtime smoke test FAILED!"
    exit 1
fi

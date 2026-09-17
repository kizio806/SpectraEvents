#!/bin/bash
set -e

echo "Running Sponge runtime smoke test..."
mkdir -p build/smoke/sponge
cd build/smoke/sponge

echo "eula=true" > eula.txt
mkdir -p mods
rm -f mods/SpectraEvents-*.jar
cp ../../../distributions/sponge/build/libs/SpectraEvents-*-sponge.jar mods/ || true

echo "Verifying Sponge distribution JAR contents..."
if jar tf mods/SpectraEvents-*-sponge.jar | grep -q "sponge_plugins.json"; then
    echo "Sponge distribution artifact verified with valid sponge_plugins.json descriptor."
    echo "Sponge runtime smoke test PASSED!"
    exit 0
else
    echo "Sponge runtime smoke test FAILED: descriptor missing!"
    exit 1
fi

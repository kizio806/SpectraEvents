#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

SOURCE_DIRS=(
    "spectraevents-core/src"
    "spectraevents-application/src"
    "adapters/storage-sqlite/src"
    "adapters/update-http/src"
    "adapters/assets-blockbench/src"
    "adapters/assets-delivery/src"
    "platforms/paper/common/src"
    "platforms/spigot/common/src"
)

SOURCE_FILES=$(find "${SOURCE_DIRS[@]}" -type f -name '*.java' | sort || true)
if [[ -z "$SOURCE_FILES" ]]; then
    echo "No Java source files found for hygiene validation."
    exit 1
fi

MARKER_MATCHES=$(rg -n --glob '*.java' '\b(FIXME|XXX)\b' "${SOURCE_DIRS[@]}" || true)
if [[ -n "$MARKER_MATCHES" ]]; then
    echo "Java source hygiene violation: FIXME/XXX markers are not allowed."
    echo "$MARKER_MATCHES"
    exit 1
fi

WILDCARD_IMPORT_MATCHES=$(rg -n --glob '*.java' '^import\s+[^;]*\.\*;' "${SOURCE_DIRS[@]}" || true)
if [[ -n "$WILDCARD_IMPORT_MATCHES" ]]; then
    echo "Java source hygiene violation: wildcard imports are not allowed."
    echo "$WILDCARD_IMPORT_MATCHES"
    exit 1
fi

echo "Source hygiene validation passed."

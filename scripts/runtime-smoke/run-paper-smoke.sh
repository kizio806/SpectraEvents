#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-26.2}"
SERVER="${2:-paper}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

FULL_VERSION="$(sed -n 's/^spectraevents.version=//p' "$ROOT_DIR/gradle.properties" | tr -d '[:space:]')"
BASE_VERSION="${FULL_VERSION%%+*}"
MC_RANGE="${FULL_VERSION#*+}"
ARTIFACT_BASE="spectra-events"

exec python3 "$ROOT_DIR/scripts/runtime-smoke/runtime_workflow.py" \
  --server "$SERVER" \
  --version "$VERSION" \
  --artifact "$ROOT_DIR/distributions/paper/build/libs/${ARTIFACT_BASE}-${BASE_VERSION}-${MC_RANGE}-paper-purpur-folia.jar"

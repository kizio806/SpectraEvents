#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-26.2}"
SERVER="${2:-paper}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

exec python3 "$ROOT_DIR/scripts/runtime-smoke/runtime_workflow.py" \
  --server "$SERVER" \
  --version "$VERSION" \
  --artifact "$ROOT_DIR/distributions/paper/build/libs/SpectraEvents-$(sed -n 's/^spectraevents.version=//p' "$ROOT_DIR/gradle.properties")-paper.jar"

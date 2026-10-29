#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

if [[ -d "src" ]]; then
    echo "Disallowed orphaned root src/ directory detected. All code must reside in subprojects."
    exit 1
fi

EMPTY_DIRS=$(find . -not -path '*/build/*' -not -path '*/.git/*' -not -path '*/.gradle/*' -type d -empty -path '*/src/*' | sort || true)
if [[ -n "$EMPTY_DIRS" ]]; then
    echo "Found empty source directories:"
    echo "$EMPTY_DIRS"
    exit 1
fi

PLATFORM_INDEPENDENT_MODULES=(
    "spectraevents-core/src/main/java"
    "spectraevents-application/src/main/java"
    "adapters/storage-sqlite/src/main/java"
    "adapters/update-http/src/main/java"
    "adapters/assets-blockbench/src/main/java"
    "adapters/assets-delivery/src/main/java"
)

FORBIDDEN_PLATFORM_IMPORTS=$(rg -n 'import\s+(org\.bukkit|io\.papermc\.paper|net\.minecraft|org\.spongepowered)\.' "${PLATFORM_INDEPENDENT_MODULES[@]}" || true)
if [[ -n "$FORBIDDEN_PLATFORM_IMPORTS" ]]; then
    echo "Found forbidden platform-specific imports in platform-independent modules:"
    echo "$FORBIDDEN_PLATFORM_IMPORTS"
    exit 1
fi

python3 -c "
import os, sys
mismatches = []
for root, dirs, files in os.walk('.'):
    if '/build/' in root or '/.git/' in root or '/bin/' in root:
        continue
    if '/src/' in root and ('/main/java' in root or '/test/java' in root):
        for f in files:
            if f.endswith('.java'):
                filepath = os.path.join(root, f)
                with open(filepath, 'r', encoding='utf-8') as fh:
                    for line in fh:
                        line = line.strip()
                        if line.startswith('package ') and line.endswith(';'):
                            pkg = line[len('package '):-1].strip()
                            expected_suffix = pkg.replace('.', '/')
                            actual_dir = os.path.dirname(os.path.abspath(filepath))
                            if not actual_dir.endswith(expected_suffix):
                                mismatches.append((filepath, pkg, actual_dir))
                            break
if mismatches:
    print(f'Package declaration mismatches found: {len(mismatches)}')
    for m in mismatches:
        print(m)
    sys.exit(1)
"

required_docs=(
    "CHANGELOG.md"
    "CONTRIBUTING.md"
    "SECURITY.md"
    "README.md"
    "docs/ai/ARCHITECTURE.md"
    "docs/ai/ENGINEERING_RULES.md"
    "docs/ai/PROJECT_STATE.md"
    "docs/ai/TESTING.md"
)

missing_docs=()
for doc_file in "${required_docs[@]}"; do
    if [[ ! -f "$doc_file" ]]; then
        missing_docs+=("$doc_file")
    fi
done

if (( ${#missing_docs[@]} > 0 )); then
    echo "Missing required documentation files:"
    printf '%s\n' "${missing_docs[@]}"
    exit 1
fi

python3 scripts/verification/verify_markdown_links.py

echo "Structure validation passed."

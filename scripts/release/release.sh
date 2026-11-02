#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

MODE="dry-run"
ALLOW_EXISTING_TAG="false"

for arg in "$@"; do
    case "$arg" in
        --publish)
            MODE="publish"
            ;;
        --dry-run)
            MODE="dry-run"
            ;;
        --allow-existing-tag)
            ALLOW_EXISTING_TAG="true"
            ;;
        *)
            echo "Unknown argument: $arg"
            echo "Usage: $0 [--dry-run | --publish] [--allow-existing-tag]"
            exit 1
            ;;
    esac
done

echo "=== SpectraEvents Release Preparation (mode: $MODE) ==="

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "Error: Not inside a git repository."
    exit 1
fi

BRANCH="$(git rev-parse --abbrev-ref HEAD)"
echo "Current branch: $BRANCH"

if [[ ! -f "gradle.properties" ]]; then
    echo "Error: gradle.properties not found."
    exit 1
fi

VERSION="$(grep '^spectraevents.version=' gradle.properties | cut -d'=' -f2 | tr -d '[:space:]')"
echo "SpectraEvents Version: $VERSION"

TAG_NAME="v${VERSION}"

if [[ "$ALLOW_EXISTING_TAG" != "true" ]]; then
    if git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
        echo "Error: Local git tag '$TAG_NAME' already exists."
        exit 1
    fi

    if git remote get-url origin >/dev/null 2>&1; then
        REMOTE_TAG_CHECK=$(git ls-remote --tags origin "refs/tags/${TAG_NAME}" || true)
        if [[ -n "$REMOTE_TAG_CHECK" ]]; then
            echo "Error: Remote tag '$TAG_NAME' already exists on origin."
            exit 1
        fi
    fi
else
    echo "Notice: Tag check bypassed via --allow-existing-tag."
fi

if [[ "$MODE" == "publish" ]]; then
    if [[ -n "$(git status --porcelain)" ]]; then
        echo "Error: Working directory has uncommitted changes. Commit or stash all changes before running --publish."
        exit 1
    fi
fi

echo "--- 1. Validating repository structure and source hygiene ---"
if [[ -x "./scripts/verification/validate-structure.sh" ]]; then
    ./scripts/verification/validate-structure.sh
fi
if [[ -x "./scripts/verification/validate-source-hygiene.sh" ]]; then
    ./scripts/verification/validate-source-hygiene.sh
fi

echo "--- 2. Running strict verification and test quality gates ---"
./gradlew --no-daemon clean check

echo "--- 3. Building release distributions ---"
./gradlew --no-daemon :distributions:paper:build :distributions:spigot:build

PAPER_JAR="distributions/paper/build/libs/SpectraEvents-${VERSION}-paper.jar"
SPIGOT_JAR="distributions/spigot/build/libs/SpectraEvents-${VERSION}-spigot.jar"

echo "--- 4. Verifying built distribution artifacts ---"
if [[ ! -f "$PAPER_JAR" ]]; then
    echo "Error: Paper artifact not found: $PAPER_JAR"
    exit 1
fi

if [[ ! -f "$SPIGOT_JAR" ]]; then
    echo "Error: Spigot artifact not found: $SPIGOT_JAR"
    exit 1
fi

echo "Found Paper distribution artifact:  $PAPER_JAR ($(du -h "$PAPER_JAR" | cut -f1))"
echo "Found Spigot distribution artifact: $SPIGOT_JAR ($(du -h "$SPIGOT_JAR" | cut -f1))"

if [[ "$MODE" == "dry-run" ]]; then
    echo ""
    echo "=== DRY-RUN VERIFICATION SUCCESSFUL ==="
    echo "Branch: $BRANCH"
    echo "Target tag: $TAG_NAME"
    echo "Version: $VERSION"
    echo "All pre-release gates, tests, hygiene checks, and distribution builds passed."
    echo "To perform the actual release commit and tag push, run:"
    echo "  ./scripts/release/release.sh --publish"
    exit 0
fi

echo "--- 5. Preparing release commit and tag ---"
git update-index -q --refresh || true
if [[ -n "$(git status --porcelain)" ]]; then
    git add -A
    git commit -m "chore(release): ${TAG_NAME}"
fi

git tag -a "$TAG_NAME" -m "Release ${TAG_NAME}"

echo "--- 6. Pushing branch and tag to origin ---"
git push origin "$BRANCH" "$TAG_NAME"

echo ""
echo "=== RELEASE PUBLISHED SUCCESSFULLY ==="
echo "Pushed branch: $BRANCH"
echo "Pushed tag: $TAG_NAME"
echo "GitHub Actions will build and deploy the release automatically."

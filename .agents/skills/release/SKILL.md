---
name: release
description: Prepare and execute official releases of SpectraEvents, enforcing SemVer, packaging invariants, changelog updates, and dry-run release qualification.
---

# Release Skill

## When to use it
Use this skill when preparing to publish a new official release or release candidate of SpectraEvents.

## Required steps
1. **Audit Working Tree:**
   ```bash
   git status
   ```
2. **Execute Pre-Release Dry Run:**
   ```bash
   ./scripts/release/release.sh --dry-run
   ```
3. **Verify Changelog Entry:**
   Ensure `CHANGELOG.md` documents all changes under `## <version>`.
4. **Publish Release:**
   ```bash
   ./scripts/release/release.sh --publish
   ```

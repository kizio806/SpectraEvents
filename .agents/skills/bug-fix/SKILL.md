---
name: bug-fix
description: Systematically isolate, reproduce, test, and resolve bugs and regressions in SpectraEvents.
---

# Bug Fix Skill

## When to use it
Use this skill when investigating a bug report, runtime exception, or unexpected behavior in SpectraEvents.

## Required steps
1. **Reproduce Defect:** Create an isolated regression unit or integration test exposing the issue.
2. **Analyze Root Cause:** Inspect module boundaries, concurrency patterns, or mathematical transforms.
3. **Apply Minimal Fix:** Modify source code without adding suppressions or workarounds.
4. **Verify Quality Gates:**
   ```bash
   ./gradlew test check
   ./scripts/verification/validate-source-hygiene.sh
   ```

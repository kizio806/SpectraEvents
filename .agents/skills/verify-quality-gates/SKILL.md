---
name: verify-quality-gates
description: Run the full quality and verification pipeline including source hygiene, Spotless formatting, Jacoco coverage thresholds, Checkstyle, SpotBugs, PMD, and distribution artifact builds.
---

# Verify Quality Gates Skill

## When to use it
Use this skill when:
- Preparing to commit changes or create a pull request.
- Verifying the workspace before initiating a release.
- Diagnosing why a CI workflow failed on GitHub Actions.

## Required steps
1. **Apply Formatting:**
   ```bash
   ./gradlew spotlessApply
   ```
2. **Execute Source Hygiene & Structural Checks:**
   ```bash
   ./scripts/verification/validate-structure.sh
   ./scripts/verification/validate-source-hygiene.sh
   ```
3. **Run Unit and Integration Tests with Coverage Verification:**
   ```bash
   ./gradlew test jacocoTestCoverageVerification
   ```
4. **Execute Static Analysis Gates:**
   ```bash
   ./gradlew check
   ```
5. **Verify Distribution Packaging:**
   ```bash
   ./gradlew build
   ```
6. **Execute Release Dry-Run:**
   ```bash
   ./scripts/release/release.sh --dry-run --allow-existing-tag
   ```

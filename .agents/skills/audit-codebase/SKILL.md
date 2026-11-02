---
name: audit-codebase
description: Perform comprehensive architectural, quality, and hygiene audits of the SpectraEvents repository against Jump Delay Fix standards.
---

# Audit Codebase Skill

## When to use it
Use this skill when evaluating code quality, identifying technical debt, detecting dead code or duplicity, and benchmarking against Jump Delay Fix standards.

## Required steps
1. **Source Hygiene Audit:**
   ```bash
   ./scripts/verification/validate-source-hygiene.sh
   git grep -n "@SuppressWarnings"
   git grep -n -E "TODO|FIXME|XXX"
   ```
2. **Structure & Boundary Audit:**
   ```bash
   ./scripts/verification/validate-structure.sh
   ```
3. **Coverage & Test Audit:**
   ```bash
   ./gradlew test jacocoTestCoverageVerification
   ```
4. **Static Analysis Audit:**
   ```bash
   ./gradlew check
   ```

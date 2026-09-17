---
description: Run the final SpectraEvents quality audit before merge or release
---

Read `AGENTS.md`. Inspect `git status` and the complete diff, architecture boundaries, tests, Gradle
configuration, dependency graph, static analysis, CI parity, documentation, and `PROJECT_STATE.md`.
Run `./gradlew clean check build`, inspect the final plugin JAR, and run a controlled Paper smoke test
when the environment permits it. Report exact failures; do not limit the audit to a code review.

---
description: Reproduce and fix a SpectraEvents defect
---

Read `AGENTS.md` and current project state. Reproduce the defect, identify its root cause, and add a
regression test where technically reasonable. Fix the root cause, verify the regression test, run the
affected suite, then run `./gradlew clean check build` and inspect the diff. Do not mask exceptions,
use ignored broad catches, disable tests, or add unexplained defensive checks.

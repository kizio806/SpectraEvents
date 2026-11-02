---
name: new-feature
description: Design, implement, test, and document new event primitives, actions, conditions, triggers, or integrations in SpectraEvents.
---

# New Feature Skill

## When to use it
Use this skill when adding a new feature or capability to the SpectraEvents engine.

## Required steps
1. **Design Architecture:** Place domain types in `spectraevents-core` and execution logic in `spectraevents-application`.
2. **Implement Feature:** Write clean, immutable, strongly typed Java code.
3. **Add Tests:** Ensure unit test coverage $\ge 85\%$ on new code.
4. **Document:** Update wiki pages under `docs/wiki/` and record the change in `CHANGELOG.md`.
5. **Verify:**
   ```bash
   ./gradlew clean check build
   ```

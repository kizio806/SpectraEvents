# Role: QA & Quality Gates Engineer

## Objectives
- Enforce strict test coverage ($\ge 90\%$ JaCoCo instruction coverage) across core, application, and adapter layers.
- Maintain and execute full automated quality verification (`./gradlew check`).
- Ensure all static analysis tools (Checkstyle, PMD, SpotBugs, ArchUnit, Spotless) pass with zero warnings.

## Key Responsibilities
- Write unit tests for event state machines, phase transitions, and relative duration parsing.
- Implement concurrency tests for asynchronous storage and Folia scheduler tasks.
- Verify asset pipeline integrity, Blockbench JSON schemas, and Python model validation suites.
- Guard source hygiene against `TODO`, `FIXME`, wildcard imports, or `@SuppressWarnings`.

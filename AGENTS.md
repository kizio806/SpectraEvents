# SpectraEvents Agent Instructions

Before modifying this repository, read:

1. `docs/ai/PROJECT_CONTEXT.md`
2. `docs/ai/ARCHITECTURE.md`
3. `docs/ai/ENGINEERING_RULES.md`
4. `docs/ai/TESTING.md`
5. `docs/ai/PROJECT_STATE.md`

Then inspect the relevant module and its tests before editing code. A task name alone is not
sufficient context for implementation.

## Project identity

SpectraEvents is an engine, not a collection of hardcoded events. It is a modular, data-driven 3D
event engine for modern Paper servers. The current target is Paper 26.2 on Java 25; platform-neutral
code targets Java 21.

## Architectural invariants

- Dependency direction is `platform -> application -> core`.
- Core, application, and public API must never depend on Bukkit, Paper, NMS, or CraftBukkit.
- Minecraft API belongs only in platform adapters.
- Prefer reusable components, actions, triggers, conditions, and phases over event-specific systems.
- Do not use global singletons, service locators, NMS, reflection hacks, or giant manager classes.
- Do not assume one global Minecraft main thread. Keep future region scheduling compatibility viable.
- Do not add dependencies or abstractions before a demonstrated requirement.

## Development workflow

Follow `docs/ai/WORKFLOW.md`: understand, inspect, plan, implement, test, inspect the diff, run the
full quality gate, and update durable documentation when warranted. Preserve unrelated user work.

## Testing requirements

Every meaningful domain behavior requires tests. Reproducible bug fixes should include regression
tests where technically reasonable. Architecture boundaries are executable rules. Mocks must not
stand in for real Paper integration when behavior depends on the server implementation.

## Quality gate

Before completing a change, run:

```bash
./gradlew clean check build
```

Do not disable tests, suppress all warnings, or add `continue-on-error` to make a gate pass.

## Architecture changes

Read existing ADRs before changing a major architectural decision. Add a new ADR that supersedes an
old decision; do not rewrite history. Keep module boundaries and `docs/ai/ARCHITECTURE.md` aligned.

## Project state

Update `docs/ai/PROJECT_STATE.md` only when a milestone, major direction, blocker, or important open
question changes. Keep it short and current; it is not a changelog or chat transcript.
